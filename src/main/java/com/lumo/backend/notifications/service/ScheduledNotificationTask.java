package com.lumo.backend.notifications.service;

import com.lumo.backend.notifications.entity.Notification;
import com.lumo.backend.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduledNotificationTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledNotificationTask.class);

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /**
     * Runs every minute to dispatch any notifications scheduled for now or earlier
     */
    @Scheduled(cron = "0 * * * * *")
    public void processScheduledNotifications() {
        Instant now = Instant.now();
        List<Notification> dueNotifications = notificationRepository.findByStatusAndScheduledAtLessThanEqual("SCHEDULED", now);

        if (!dueNotifications.isEmpty()) {
            log.info("[Notifications] Found {} scheduled notifications ready for dispatch.", dueNotifications.size());
            for (Notification notification : dueNotifications) {
                try {
                    log.info("[Notifications] Triggering scheduled notification ID [{}]", notification.getId());
                    notificationService.sendNotification(notification.getId());
                } catch (Exception e) {
                    log.error("[Notifications] Error dispatching scheduled notification [{}]: {}", notification.getId(), e.getMessage(), e);
                }
            }
        }
    }
}
