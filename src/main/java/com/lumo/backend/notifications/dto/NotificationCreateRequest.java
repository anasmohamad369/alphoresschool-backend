package com.lumo.backend.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateRequest {
    private String title;
    private String body;
    private String type; // ANNOUNCEMENT, EXAM, EXAM_RESULT, ATTENDANCE, ASSIGNMENT, HOMEWORK, FEE, EVENT, CIRCULAR, GENERAL, EMERGENCY
    private String screen; // Attendance, Homework, Timetable, ExamSchedule, ReportCard, Events, Messages, etc.
    private Map<String, String> data;
    private String targetType; // ALL, ROLE, CLASS, USER, TOPIC
    private String targetId;   // e.g. "Class 10", "TEACHER", "studentId", "school_all"
    private String priority;   // normal, high
    private Instant scheduledAt;
    private Boolean sendNow;
}
