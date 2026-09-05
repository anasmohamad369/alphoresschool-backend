package com.lumo.backend.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationStatsDto {
    private long total;
    private long sent;
    private long scheduled;
    private long failed;
    private long drafts;
}
