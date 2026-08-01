package com.lumo.backend.teachers.dto;

import java.util.List;

public record TeacherProfileResponse(
        Long id,
        String emailId,
        String name,
        String mobileNumber,
        String classTeacher,
        String dateOfBirth,
        List<String> classes,
        List<String> subjects) {
}
