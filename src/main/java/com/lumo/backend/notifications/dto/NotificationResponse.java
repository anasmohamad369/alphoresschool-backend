package com.lumo.backend.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String body;
    private String type;
    private String screen;
    private Map<String, String> data;
    private String targetType;
    private String targetId;
    private String status;
    private String priority;
    private Instant scheduledAt;
    private Instant sentAt;
    private Integer totalRecipients;
    private Integer successfulDeliveries;
    private Integer failedDeliveries;
    private String createdBy;
    private String createdByName;
    private Instant createdAt;
    // For user recipient views:
    private Boolean isRead;
    private Instant readAt;
}
