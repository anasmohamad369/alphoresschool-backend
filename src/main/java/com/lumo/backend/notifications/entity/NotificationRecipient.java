package com.lumo.backend.notifications.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(
    name = "notification_recipients",
    indexes = {
        @Index(name = "idx_recipients_user", columnList = "user_id, read_at"),
        @Index(name = "idx_recipients_notif", columnList = "notification_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Notification notification;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "user_type", nullable = false)
    private String userType;

    @Column(nullable = false)
    private String status; // PENDING, SENT, FAILED, READ

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) this.status = "SENT";
    }
}
