package com.lumo.backend.notifications.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumo.backend.notifications.dto.*;
import com.lumo.backend.notifications.entity.Notification;
import com.lumo.backend.notifications.entity.NotificationRecipient;
import com.lumo.backend.notifications.entity.UserDevice;
import com.lumo.backend.notifications.repository.NotificationRecipientRepository;
import com.lumo.backend.notifications.repository.NotificationRepository;
import com.lumo.backend.notifications.repository.UserDeviceRepository;
import com.lumo.backend.students.entity.Student;
import com.lumo.backend.students.repository.StudentRepository;
import com.lumo.backend.teachers.entity.Teacher;
import com.lumo.backend.teachers.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final UserDeviceRepository userDeviceRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final FirebaseNotificationService firebaseNotificationService;
    private final ObjectMapper objectMapper;

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Device Token Management
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public void registerDevice(String userId, String userType, DeviceRegistrationRequest request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device FCM token is required");
        }

        String token = request.getToken().trim();
        Optional<UserDevice> existing = userDeviceRepository.findByFcmToken(token);

        UserDevice device;
        if (existing.isPresent()) {
            device = existing.get();
            device.setUserId(userId);
            device.setUserType(userType);
            device.setIsActive(true);
            device.setLastSeenAt(Instant.now());
            if (request.getPlatform() != null) device.setPlatform(request.getPlatform());
            if (request.getDeviceId() != null) device.setDeviceId(request.getDeviceId());
            if (request.getDeviceName() != null) device.setDeviceName(request.getDeviceName());
            if (request.getAppVersion() != null) device.setAppVersion(request.getAppVersion());
        } else {
            device = UserDevice.builder()
                    .userId(userId)
                    .userType(userType)
                    .fcmToken(token)
                    .platform(request.getPlatform() != null ? request.getPlatform() : "android")
                    .deviceId(request.getDeviceId())
                    .deviceName(request.getDeviceName())
                    .appVersion(request.getAppVersion())
                    .isActive(true)
                    .lastSeenAt(Instant.now())
                    .build();
        }

        userDeviceRepository.save(device);
        log.info("[Notifications] Registered device token for user [{}] ({})", userId, userType);
    }

    @Transactional
    public void unregisterDevice(String userId, String token) {
        if (token == null || token.isBlank()) return;
        userDeviceRepository.findByFcmToken(token.trim()).ifPresent(device -> {
            if (device.getUserId().equalsIgnoreCase(userId)) {
                device.setIsActive(false);
                device.setUpdatedAt(Instant.now());
                userDeviceRepository.save(device);
                log.info("[Notifications] Unregistered device token for user [{}]", userId);
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Notification Creation & Sending
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public NotificationResponse createNotification(NotificationCreateRequest request, String createdBy, String createdByName) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (request.getBody() == null || request.getBody().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is required");
        }

        String targetType = request.getTargetType() != null ? request.getTargetType().toUpperCase() : "ALL";
        String status;

        if (Boolean.TRUE.equals(request.getSendNow()) || (request.getScheduledAt() == null && request.getSendNow() == null)) {
            status = "SENDING";
        } else if (request.getScheduledAt() != null && request.getScheduledAt().isAfter(Instant.now())) {
            status = "SCHEDULED";
        } else {
            status = "DRAFT";
        }

        String dataJson = null;
        if (request.getData() != null && !request.getData().isEmpty()) {
            try {
                dataJson = objectMapper.writeValueAsString(request.getData());
            } catch (Exception e) {
                log.warn("[Notifications] Could not serialize data payload: {}", e.getMessage());
            }
        }

        Notification notification = Notification.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .type(request.getType() != null ? request.getType().toUpperCase() : "GENERAL")
                .screen(request.getScreen())
                .dataJson(dataJson)
                .targetType(targetType)
                .targetId(request.getTargetId())
                .status(status)
                .priority(request.getPriority() != null ? request.getPriority() : "high")
                .scheduledAt(request.getScheduledAt())
                .createdBy(createdBy)
                .createdByName(createdByName)
                .build();

        notification = notificationRepository.save(notification);
        log.info("[Notifications] Notification created ID: {}, Status: {}", notification.getId(), status);

        if ("SENDING".equals(status)) {
            sendNotification(notification.getId());
            notification = notificationRepository.findById(notification.getId()).orElse(notification);
        }

        return toResponse(notification, null);
    }

    @Transactional
    public NotificationResponse sendNotification(Long notificationId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        log.info("[Notifications] Dispatching notification ID: [{}] {}", notif.getId(), notif.getTitle());
        notif.setStatus("SENDING");
        notificationRepository.save(notif);

        // 1. Resolve target users and devices
        Map<String, String> userTypeMap = new HashMap<>(); // userId -> userType
        List<String> targetUserIds = new ArrayList<>();

        String targetType = notif.getTargetType();
        String targetId = notif.getTargetId();

        if ("ALL".equalsIgnoreCase(targetType)) {
            studentRepository.findAll().forEach(s -> {
                targetUserIds.add(s.getStudentId());
                userTypeMap.put(s.getStudentId(), "STUDENT");
            });
            teacherRepository.findAll().forEach(t -> {
                targetUserIds.add(t.getEmailId());
                userTypeMap.put(t.getEmailId(), "TEACHER");
            });
        } else if ("ROLE".equalsIgnoreCase(targetType)) {
            if ("STUDENT".equalsIgnoreCase(targetId) || "STUDENTS".equalsIgnoreCase(targetId)) {
                studentRepository.findAll().forEach(s -> {
                    targetUserIds.add(s.getStudentId());
                    userTypeMap.put(s.getStudentId(), "STUDENT");
                });
            } else if ("TEACHER".equalsIgnoreCase(targetId) || "TEACHERS".equalsIgnoreCase(targetId)) {
                teacherRepository.findAll().forEach(t -> {
                    targetUserIds.add(t.getEmailId());
                    userTypeMap.put(t.getEmailId(), "TEACHER");
                });
            }
        } else if ("CLASS".equalsIgnoreCase(targetType)) {
            // Find students in class
            List<Student> students = studentRepository.findByStudentClass(targetId);
            students.forEach(s -> {
                targetUserIds.add(s.getStudentId());
                userTypeMap.put(s.getStudentId(), "STUDENT");
            });
        } else if ("USER".equalsIgnoreCase(targetType)) {
            if (targetId != null && !targetId.isBlank()) {
                targetUserIds.add(targetId);
                userTypeMap.put(targetId, "USER");
            }
        }

        // 2. Fetch active device tokens
        List<UserDevice> devices = targetUserIds.isEmpty()
                ? Collections.emptyList()
                : userDeviceRepository.findByUserIdInAndIsActiveTrue(targetUserIds);

        List<String> tokens = devices.stream().map(UserDevice::getFcmToken).collect(Collectors.toList());

        // 3. Prepare payload data
        Map<String, String> dataPayload = new HashMap<>();
        if (notif.getDataJson() != null && !notif.getDataJson().isBlank()) {
            try {
                dataPayload = objectMapper.readValue(notif.getDataJson(), new TypeReference<Map<String, String>>() {});
            } catch (Exception ignored) {}
        }
        dataPayload.put("id", String.valueOf(notif.getId()));
        dataPayload.put("type", notif.getType());
        if (notif.getScreen() != null) {
            dataPayload.put("screen", notif.getScreen());
        }

        // 4. Send via Firebase
        FirebaseNotificationService.SendReport report;
        if ("TOPIC".equalsIgnoreCase(targetType) && targetId != null) {
            boolean topicSuccess = firebaseNotificationService.sendToTopic(targetId, notif.getTitle(), notif.getBody(), dataPayload);
            report = new FirebaseNotificationService.SendReport();
            if (topicSuccess) {
                report.successCount = 1;
            } else {
                report.failureCount = 1;
            }
        } else {
            report = firebaseNotificationService.sendMulticast(tokens, notif.getTitle(), notif.getBody(), dataPayload, notif.getPriority());
        }

        // 5. Create recipient delivery rows for user tracking
        if (!targetUserIds.isEmpty()) {
            List<NotificationRecipient> recipients = new ArrayList<>();
            for (String uid : targetUserIds) {
                recipients.add(NotificationRecipient.builder()
                        .notification(notif)
                        .userId(uid)
                        .userType(userTypeMap.getOrDefault(uid, "USER"))
                        .status("SENT")
                        .build());
            }
            recipientRepository.saveAll(recipients);
        }

        // 6. Update notification status
        notif.setTotalRecipients(targetUserIds.size());
        notif.setSuccessfulDeliveries(report.successCount);
        notif.setFailedDeliveries(report.failureCount);
        notif.setSentAt(Instant.now());

        if (report.failureCount == 0 || report.successCount > 0) {
            notif.setStatus(report.failureCount > 0 ? "PARTIALLY_SENT" : "SENT");
        } else {
            notif.setStatus("FAILED");
            notif.setFailureReason("All " + report.failureCount + " device deliveries failed");
        }

        notificationRepository.save(notif);
        log.info("[Notifications] Completed send for notification [{}]. Status: {}", notif.getId(), notif.getStatus());
        return toResponse(notif, null);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. User Notification History & Read Status
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationRecipient> recipients = recipientRepository.findByUserIdWithNotification(userId, pageable);
        return recipients.map(r -> toResponse(r.getNotification(), r));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return recipientRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, String userId) {
        recipientRepository.findByNotificationIdAndUserId(notificationId, userId).ifPresent(r -> {
            r.setReadAt(Instant.now());
            r.setStatus("READ");
            recipientRepository.save(r);
        });
    }

    @Transactional
    public int markAllAsRead(String userId) {
        return recipientRepository.markAllAsRead(userId, Instant.now());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Admin Management APIs
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listNotifications(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifs;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            notifs = notificationRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
        } else {
            notifs = notificationRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return notifs.map(n -> toResponse(n, null));
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(Long id) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        return toResponse(notif, null);
    }

    @Transactional
    public NotificationResponse updateNotification(Long id, NotificationCreateRequest request) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!"DRAFT".equals(notif.getStatus()) && !"SCHEDULED".equals(notif.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DRAFT or SCHEDULED notifications can be edited");
        }

        if (request.getTitle() != null) notif.setTitle(request.getTitle());
        if (request.getBody() != null) notif.setBody(request.getBody());
        if (request.getType() != null) notif.setType(request.getType().toUpperCase());
        if (request.getScreen() != null) notif.setScreen(request.getScreen());
        if (request.getTargetType() != null) notif.setTargetType(request.getTargetType().toUpperCase());
        if (request.getTargetId() != null) notif.setTargetId(request.getTargetId());
        if (request.getScheduledAt() != null) {
            notif.setScheduledAt(request.getScheduledAt());
            notif.setStatus("SCHEDULED");
        }

        if (request.getData() != null) {
            try {
                notif.setDataJson(objectMapper.writeValueAsString(request.getData()));
            } catch (Exception ignored) {}
        }

        notif = notificationRepository.save(notif);
        return toResponse(notif, null);
    }

    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    @Transactional
    public NotificationResponse cancelScheduledNotification(Long id) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!"SCHEDULED".equals(notif.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only SCHEDULED notifications can be cancelled");
        }

        notif.setStatus("CANCELLED");
        notificationRepository.save(notif);
        return toResponse(notif, null);
    }

    @Transactional(readOnly = true)
    public NotificationStatsDto getStats() {
        long total = notificationRepository.count();
        long sent = notificationRepository.countByStatus("SENT") + notificationRepository.countByStatus("PARTIALLY_SENT");
        long scheduled = notificationRepository.countByStatus("SCHEDULED");
        long failed = notificationRepository.countByStatus("FAILED");
        long drafts = notificationRepository.countByStatus("DRAFT");
        return new NotificationStatsDto(total, sent, scheduled, failed, drafts);
    }

    private NotificationResponse toResponse(Notification n, NotificationRecipient r) {
        Map<String, String> data = null;
        if (n.getDataJson() != null && !n.getDataJson().isBlank()) {
            try {
                data = objectMapper.readValue(n.getDataJson(), new TypeReference<Map<String, String>>() {});
            } catch (Exception ignored) {}
        }

        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .type(n.getType())
                .screen(n.getScreen())
                .data(data)
                .targetType(n.getTargetType())
                .targetId(n.getTargetId())
                .status(n.getStatus())
                .priority(n.getPriority())
                .scheduledAt(n.getScheduledAt())
                .sentAt(n.getSentAt())
                .totalRecipients(n.getTotalRecipients())
                .successfulDeliveries(n.getSuccessfulDeliveries())
                .failedDeliveries(n.getFailedDeliveries())
                .createdBy(n.getCreatedBy())
                .createdByName(n.getCreatedByName())
                .createdAt(n.getCreatedAt())
                .isRead(r != null ? r.getReadAt() != null : null)
                .readAt(r != null ? r.getReadAt() : null)
                .build();
    }
}
