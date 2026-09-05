package com.lumo.backend.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceRegistrationRequest {
    private String token;
    private String platform;
    private String deviceId;
    private String deviceName;
    private String appVersion;
}
