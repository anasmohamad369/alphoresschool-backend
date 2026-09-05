package com.lumo.backend.notifications.repository;

import com.lumo.backend.notifications.entity.NotificationRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    List<NotificationRecipient> findByNotificationId(Long notificationId);

    @Query("SELECT r FROM NotificationRecipient r JOIN FETCH r.notification WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    Page<NotificationRecipient> findByUserIdWithNotification(@Param("userId") String userId, Pageable pageable);

    Optional<NotificationRecipient> findByNotificationIdAndUserId(Long notificationId, String userId);

    long countByUserIdAndReadAtIsNull(String userId);

    @Modifying
    @Query("UPDATE NotificationRecipient r SET r.readAt = :now, r.status = 'READ' WHERE r.userId = :userId AND r.readAt IS NULL")
    int markAllAsRead(@Param("userId") String userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM NotificationRecipient r WHERE r.notification.id = :notificationId")
    void deleteByNotificationId(@Param("notificationId") Long notificationId);
}
