package com.lumo.backend.notifications.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_status", columnList = "status"),
        @Index(name = "idx_notifications_scheduled", columnList = "status, scheduled_at"),
        @Index(name = "idx_notifications_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private String type; // ANNOUNCEMENT, EXAM, EXAM_RESULT, ATTENDANCE, ASSIGNMENT, HOMEWORK, FEE, EVENT, CIRCULAR, GENERAL, EMERGENCY

    @Column(name = "screen")
    private String screen; // Attendance, Homework, Timetable, ExamSchedule, ExamDetails, ReportCard, Events, Messages, Notifications

    @Column(name = "data_json", length = 3000)
    private String dataJson;

    @Column(name = "target_type", nullable = false)
    private String targetType; // ALL, ROLE, CLASS, SECTION, USER, TOPIC

    @Column(name = "target_id")
    private String targetId; // e.g. "STUDENT", "TEACHER", "Class 10", "student_123"

    @Column(nullable = false)
    private String status; // DRAFT, SCHEDULED, SENDING, SENT, PARTIALLY_SENT, FAILED, CANCELLED

    @Column(name = "priority")
    private String priority; // normal, high

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "total_recipients")
    @Builder.Default
    private Integer totalRecipients = 0;

    @Column(name = "successful_deliveries")
    @Builder.Default
    private Integer successfulDeliveries = 0;

    @Column(name = "failed_deliveries")
    @Builder.Default
    private Integer failedDeliveries = 0;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NotificationRecipient> recipients = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.totalRecipients == null) this.totalRecipients = 0;
        if (this.successfulDeliveries == null) this.successfulDeliveries = 0;
        if (this.failedDeliveries == null) this.failedDeliveries = 0;
        if (this.priority == null) this.priority = "high";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
