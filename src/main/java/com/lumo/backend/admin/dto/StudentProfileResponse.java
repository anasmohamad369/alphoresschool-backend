package com.lumo.backend.admin.dto;

public record StudentProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String middleName,
        String mobileNumber,
        String fatherName,
        String motherName,
        String fatherAadharNumber,
        String motherAadharNumber,
        String studentAadharNumber,
        String dateOfBirth,
        String gender,
        String studentClass,
        String profilePhotoUrl
) {}
