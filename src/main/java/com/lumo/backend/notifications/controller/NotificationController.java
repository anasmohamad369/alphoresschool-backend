package com.lumo.backend.notifications.controller;

import com.lumo.backend.notifications.dto.DeviceRegistrationRequest;
import com.lumo.backend.notifications.dto.DeviceUnregisterRequest;
import com.lumo.backend.notifications.dto.NotificationResponse;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    private static class AuthUser {
        String userId;
        String userType;
        AuthUser(String userId, String userType) {
            this.userId = userId;
            this.userType = userType;
        }
    }

    private AuthUser resolveUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token");
        }
        String token = authHeader.substring(7).trim();

        String studentId = jwtService.extractStudentSubject(token);
        if (studentId != null) return new AuthUser(studentId, "STUDENT");

        String teacherEmail = jwtService.extractTeacherSubject(token);
        if (teacherEmail != null) return new AuthUser(teacherEmail, "TEACHER");

        String adminSubject = jwtService.extractAdminSubject(token);
        if (adminSubject != null) return new AuthUser(adminSubject, "ADMIN");

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired session token");
    }

    /**
     * Register device FCM token
     */
    @PostMapping("/devices/register")
    public ResponseEntity<Map<String, Object>> registerDevice(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody DeviceRegistrationRequest request) {
        AuthUser user = resolveUser(authHeader);
        notificationService.registerDevice(user.userId, user.userType, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Device token registered successfully"));
    }

    /**
     * Unregister device FCM token (on logout)
     */
    @PostMapping("/devices/unregister")
    public ResponseEntity<Map<String, Object>> unregisterDevice(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody DeviceUnregisterRequest request) {
        AuthUser user = resolveUser(authHeader);
        notificationService.unregisterDevice(user.userId, request.getToken());
        return ResponseEntity.ok(Map.of("success", true, "message", "Device token unregistered successfully"));
    }

    /**
     * Get paginated notifications for current user
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuthUser user = resolveUser(authHeader);
        return ResponseEntity.ok(notificationService.getUserNotifications(user.userId, page, size));
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthUser user = resolveUser(authHeader);
        long count = notificationService.getUnreadCount(user.userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Mark single notification as read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        AuthUser user = resolveUser(authHeader);
        notificationService.markAsRead(id, user.userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Mark all notifications as read
     */
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthUser user = resolveUser(authHeader);
        int updated = notificationService.markAllAsRead(user.userId);
        return ResponseEntity.ok(Map.of("success", true, "updated", updated));
    }
}
