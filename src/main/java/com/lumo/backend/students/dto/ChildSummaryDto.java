package com.lumo.backend.students.dto;

public record ChildSummaryDto(
    Long id,
    String studentId,
    String firstName,
    String lastName,
    String middleName,
    String studentClass,
    String sectionName,
    String profilePhotoUrl
) {}
