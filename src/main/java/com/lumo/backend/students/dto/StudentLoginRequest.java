package com.lumo.backend.students.dto;

public record StudentLoginRequest(String studentId, String mobileNumber, String dateOfBirth) {
    public StudentLoginRequest(String mobileNumber, String dateOfBirth) {
        this(null, mobileNumber, dateOfBirth);
    }
}
