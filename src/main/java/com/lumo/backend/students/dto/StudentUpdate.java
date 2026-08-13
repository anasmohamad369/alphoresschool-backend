package com.lumo.backend.students.dto;

public record StudentUpdate(
    String firstName,
    String lastName,
    String middleName,
    String mobileNumber,
    String fatherName,
    String motherName,
    String dateOfBirth,
    String gender,
    String studentClass,
    String marks,
    String profilePhotoUrl
) {}