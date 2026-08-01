package com.lumo.backend.teachers.dto;

public record TeacherLogin(
    boolean success,
    String mobileNumber,
    String dateOfBirth
) {
}
