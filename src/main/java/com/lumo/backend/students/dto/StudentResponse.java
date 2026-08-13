package com.lumo.backend.students.dto;

public record StudentResponse(
    Long studentId,
    String firstName,
    String lastName,
    String middleName,
    String mobileNumber,
    String fatherName,
    String motherName,
    String dateOfBirth,
    String gender,
    String studentClass,
    Long classId,
    Long teacherId,
    String teacherName,
    String teacherEmail,
    String teacherMobile,
    java.util.List<com.lumo.backend.attendance.entity.Attendance> attendance,
    String admissionId,
    String profilePhotoUrl
) {}