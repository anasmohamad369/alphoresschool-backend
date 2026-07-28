package com.lumo.backend.students.dto;

public record BirthdayStudentResponse(
    Long id,
    String studentId,
    String firstName,
    String lastName,
    String dateOfBirth,
    String className,
    String profilePhotoUrl,
    Long daysUntilBirthday
) {}
