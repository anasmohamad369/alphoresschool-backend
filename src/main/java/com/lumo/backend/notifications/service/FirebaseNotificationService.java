package com.lumo.backend.notifications.service;

import com.google.firebase.messaging.*;
import com.lumo.backend.notifications.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FirebaseNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseNotificationService.class);
    private static final int FCM_BATCH_SIZE = 500;

    private final FirebaseConfig firebaseConfig;
    private final UserDeviceRepository userDeviceRepository;

    public static class SendReport {
        public int successCount = 0;
        public int failureCount = 0;
        public List<String> invalidTokens = new ArrayList<>();
    }

    /**
     * Send multicast push notification to multiple device tokens
     */
    @Transactional
    public SendReport sendMulticast(List<String> tokens, String title, String body, Map<String, String> data, String priority) {
        SendReport report = new SendReport();

        if (tokens == null || tokens.isEmpty()) {
            log.info("[Notifications] No device tokens to send to.");
            return report;
        }

        // Deduplicate tokens
        List<String> uniqueTokens = new ArrayList<>(new LinkedHashSet<>(tokens));

        if (!firebaseConfig.isInitialized()) {
            log.warn("[Notifications] Firebase not initialized. Simulating notification delivery for {} devices.", uniqueTokens.size());
            report.successCount = uniqueTokens.size();
            return report;
        }

        Map<String, String> safeData = data != null ? new HashMap<>(data) : new HashMap<>();
        if (!safeData.containsKey("title")) safeData.put("title", title);
        if (!safeData.containsKey("body")) safeData.put("body", body);

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        AndroidConfig.Priority androidPriority = "high".equalsIgnoreCase(priority)
                ? AndroidConfig.Priority.HIGH
                : AndroidConfig.Priority.NORMAL;

        // Channel ID mapping based on data type
        String channelId = "general";
        String type = safeData.get("type");
        if ("ANNOUNCEMENT".equalsIgnoreCase(type) || "CIRCULAR".equalsIgnoreCase(type)) {
            channelId = "announcements";
        } else if ("EXAM".equalsIgnoreCase(type) || "EXAM_RESULT".equalsIgnoreCase(type)) {
            channelId = "exams";
        } else if ("EMERGENCY".equalsIgnoreCase(type)) {
            channelId = "emergency";
        }

        AndroidNotification androidNotification = AndroidNotification.builder()
                .setChannelId(channelId)
                .setIcon("ic_notification")
                .setColor("#1565C0")
                .setPriority("high".equalsIgnoreCase(priority) ? AndroidNotification.Priority.HIGH : AndroidNotification.Priority.DEFAULT)
                .build();

        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(androidPriority)
                .setNotification(androidNotification)
                .build();

        // Process in chunks of 500
        for (int i = 0; i < uniqueTokens.size(); i += FCM_BATCH_SIZE) {
            int end = Math.min(i + FCM_BATCH_SIZE, uniqueTokens.size());
            List<String> batch = uniqueTokens.subList(i, end);

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .setNotification(notification)
                    .putAllData(safeData)
                    .setAndroidConfig(androidConfig)
                    .build();

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                report.successCount += response.getSuccessCount();
                report.failureCount += response.getFailureCount();

                List<SendResponse> responses = response.getResponses();
                for (int j = 0; j < responses.size(); j++) {
                    SendResponse sendResponse = responses.get(j);
                    if (!sendResponse.isSuccessful()) {
                        String badToken = batch.get(j);
                        FirebaseMessagingException exc = sendResponse.getException();
                        MessagingErrorCode errorCode = exc != null ? exc.getMessagingErrorCode() : null;

                        if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                            report.invalidTokens.add(badToken);
                        }
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.error("[Notifications] Batch send failed: {}", e.getMessage(), e);
                report.failureCount += batch.size();
            }
        }

        // Clean up invalid tokens automatically
        if (!report.invalidTokens.isEmpty()) {
            log.info("[Notifications] Cleaning up {} invalid/unregistered FCM tokens.", report.invalidTokens.size());
            try {
                userDeviceRepository.deactivateTokens(report.invalidTokens, Instant.now());
            } catch (Exception e) {
                log.error("[Notifications] Error deactivating invalid tokens: {}", e.getMessage());
            }
        }

        log.info("[Notifications] Batch completed. Success: {}, Failed: {}, Deactivated invalid: {}",
                report.successCount, report.failureCount, report.invalidTokens.size());

        return report;
    }

    /**
     * Send push notification to a specific topic
     */
    public boolean sendToTopic(String topic, String title, String body, Map<String, String> data) {
        if (!firebaseConfig.isInitialized()) {
            log.warn("[Notifications] Firebase not initialized. Simulating topic message to [{}]", topic);
            return true;
        }

        Map<String, String> safeData = data != null ? new HashMap<>(data) : new HashMap<>();
        safeData.put("title", title);
        safeData.put("body", body);

        Message message = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(safeData)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[Notifications] Successfully sent topic [{}] message: {}", topic, response);
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("[Notifications] Failed to send topic message: {}", e.getMessage(), e);
            return false;
        }
    }
}
