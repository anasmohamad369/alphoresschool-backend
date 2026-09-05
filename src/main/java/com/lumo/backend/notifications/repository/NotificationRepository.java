package com.lumo.backend.notifications.repository;

import com.lumo.backend.notifications.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStatusAndScheduledAtLessThanEqual(String status, Instant scheduledAt);

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Notification> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<Notification> findByTypeOrderByCreatedAtDesc(String type, Pageable pageable);

    long countByStatus(String status);
}
