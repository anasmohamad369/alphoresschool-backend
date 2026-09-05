package com.lumo.backend.notifications.controller;

import com.lumo.backend.notifications.dto.NotificationCreateRequest;
import com.lumo.backend.notifications.dto.NotificationResponse;
import com.lumo.backend.notifications.dto.NotificationStatsDto;
import com.lumo.backend.notifications.service.NotificationService;
import com.lumo.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    private static class AdminUser {
        String id;
        String name;
        AdminUser(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private AdminUser verifyAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid admin authorization token");
        }
        String token = authHeader.substring(7).trim();

        String adminSubject = jwtService.extractAdminSubject(token);
        if (adminSubject != null) {
            return new AdminUser(adminSubject, "Administrator");
        }

        String teacherEmail = jwtService.extractTeacherSubject(token);
        if (teacherEmail != null) {
            return new AdminUser(teacherEmail, "Staff / Principal");
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin or Principal privileges required");
    }

    /**
     * Get notification management stats
     */
    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsDto> getStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        verifyAdmin(authHeader);
        return ResponseEntity.ok(notificationService.getStats());
    }

    /**
     * List all notifications with optional status filter
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> listNotifications(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        verifyAdmin(authHeader);
        return ResponseEntity.ok(notificationService.listNotifications(status, page, size));
    }

    /**
     * Get single notification details
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        verifyAdmin(authHeader);
        return ResponseEntity.ok(notificationService.getNotification(id));
    }

    /**
     * Create notification (Draft, Scheduled, or Send Now)
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody NotificationCreateRequest request) {
        AdminUser user = verifyAdmin(authHeader);
        NotificationResponse response = notificationService.createNotification(request, user.id, user.name);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update draft or scheduled notification
     */
    @PutMapping("/{id}")
    public ResponseEntity<NotificationResponse> updateNotification(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody NotificationCreateRequest request) {
        verifyAdmin(authHeader);
        return ResponseEntity.ok(notificationService.updateNotification(id, request));
    }

    /**
     * Delete notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        verifyAdmin(authHeader);
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Trigger immediate send of a draft or scheduled notification
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<NotificationResponse> sendNotification(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        verifyAdmin(authHeader);
        return ResponseEntity.ok(notificationService.sendNotification(id));
    }

    /**
     * Cancel a scheduled notification
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<NotificationResponse> cancelNotification(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        verifyAdmin(authHeader);
        return ResponseEntity.ok(notificationService.cancelScheduledNotification(id));
    }
}
