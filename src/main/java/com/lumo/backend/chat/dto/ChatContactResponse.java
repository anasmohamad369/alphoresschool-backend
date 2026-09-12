package com.lumo.backend.chat.dto;

public record ChatContactResponse(
    String id,
    String name,
    String role,
    String subtext,
    String mobileNumber,
    String email
) {
}
