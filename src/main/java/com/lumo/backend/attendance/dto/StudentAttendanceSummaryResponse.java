package com.lumo.backend.attendance.dto;

import com.lumo.backend.attendance.entity.Attendance;
import java.util.List;

public record StudentAttendanceSummaryResponse(
    String studentName,
    String studentClass,
    double attendancePercentage,
    long totalPresent,
    long totalAbsent,
    long totalLate,
    long totalHalfDay,
    long totalHoliday,
    List<Attendance> history
) {}
