package com.lumo.backend.attendance.service;

import com.lumo.backend.attendance.dto.AttendanceRequest;
import com.lumo.backend.attendance.entity.Attendance;
import com.lumo.backend.attendance.repository.AttendanceRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final com.lumo.backend.students.service.StudentService studentService;
    private final com.lumo.backend.students.repository.StudentRepository studentRepository;
    private final com.lumo.backend.security.JwtService jwtService;
    private final com.lumo.backend.teachers.repository.TeacherRepository teacherRepository;
    private final com.lumo.backend.classes.repository.SchoolClassRepository classRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository, 
            com.lumo.backend.students.service.StudentService studentService,
            com.lumo.backend.students.repository.StudentRepository studentRepository,
            com.lumo.backend.security.JwtService jwtService,
            com.lumo.backend.teachers.repository.TeacherRepository teacherRepository,
            com.lumo.backend.classes.repository.SchoolClassRepository classRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentService = studentService;
        this.studentRepository = studentRepository;
        this.jwtService = jwtService;
        this.teacherRepository = teacherRepository;
        this.classRepository = classRepository;
    }

    public Attendance markAttendance(AttendanceRequest request) {
        LocalDate date = request.attendanceDate() != null ? request.attendanceDate() : LocalDate.now();
        Attendance attendance = attendanceRepository.findByStudentIdAndAttendanceDate(request.studentId(), date)
                .orElse(new Attendance());
        attendance.setStudentId(request.studentId());
        attendance.setAttendanceDate(date);
        attendance.setStatus(request.status());
        return attendanceRepository.save(attendance);
    }

    public List<Attendance> getAttendanceHistory(String studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    public com.lumo.backend.attendance.dto.ClassAttendanceResponse markClassAttendance(com.lumo.backend.attendance.dto.ClassAttendanceRequest request) {
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();
        List<String> absents = request.absentStudentIds() != null ? request.absentStudentIds() : List.of();

        List<com.lumo.backend.students.dto.StudentResponse> students;
        if (request.sectionId() != null) {
            students = studentService.getStudentsByClassAndSection(request.classId(), request.sectionId());
        } else {
            students = studentService.getStudentsByClassId(request.classId());
        }

        List<com.lumo.backend.students.dto.StudentResponse> presentStudents = new java.util.ArrayList<>();
        List<com.lumo.backend.students.dto.StudentResponse> absentStudents = new java.util.ArrayList<>();

        for (com.lumo.backend.students.dto.StudentResponse student : students) {
            boolean isAbsent = absents.contains(student.studentId().toString());
            
            Attendance attendance = attendanceRepository.findByStudentIdAndAttendanceDate(student.studentId().toString(), date)
                    .orElse(new Attendance());
            
            attendance.setStudentId(student.studentId().toString());
            attendance.setAttendanceDate(date);
            attendance.setStatus(isAbsent ? "ABSENT" : "PRESENT");
            attendanceRepository.save(attendance);

            if (isAbsent) {
                absentStudents.add(student);
            } else {
                presentStudents.add(student);
            }
        }

        return new com.lumo.backend.attendance.dto.ClassAttendanceResponse(date, presentStudents, absentStudents);
    }

    public com.lumo.backend.attendance.dto.DailyAttendanceStatsResponse getDailyStats(LocalDate date) {
        if (date == null) date = LocalDate.now();
        long totalStudents = studentRepository.count();
        List<Attendance> attendances = attendanceRepository.findByAttendanceDate(date);
        long totalMarked = attendances.size();
        long totalPresent = attendances.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        long totalAbsent = attendances.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();

        return new com.lumo.backend.attendance.dto.DailyAttendanceStatsResponse(
            date, totalStudents, totalMarked, totalPresent, totalAbsent
        );
    }

    public com.lumo.backend.attendance.dto.DailyAttendanceStatsResponse getTeacherHomeroomDailyStats(String authorizationHeader, LocalDate date) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing token");
        }
        String token = authorizationHeader.substring(7).trim();
        String email = jwtService.extractTeacherSubject(token);
        if (email == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        com.lumo.backend.teachers.entity.Teacher teacher = teacherRepository.findByEmailId(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Teacher not found"));
        
        String homeroom = teacher.getClassTeacher();
        if (homeroom == null || homeroom.isBlank()) {
            return new com.lumo.backend.attendance.dto.DailyAttendanceStatsResponse(date != null ? date : LocalDate.now(), 0, 0, 0, 0);
        }

        // Extract base class name if it contains section info (e.g., "Grade 10Section Lion" -> "Grade 10")
        String baseClass = homeroom;
        if (homeroom.contains("Section ")) {
            baseClass = homeroom.split("Section ")[0].trim();
        } else {
            for (com.lumo.backend.classes.entity.SchoolClass sc : classRepository.findAll()) {
                if (homeroom.startsWith(sc.getName())) {
                    baseClass = sc.getName();
                    break;
                }
            }
        }

        if (date == null) date = LocalDate.now();
        
        List<com.lumo.backend.students.entity.Student> classStudents = studentRepository.findByStudentClass(baseClass);
        List<String> studentIdsInClass = classStudents.stream().map(s -> s.getId().toString()).toList();
        
        List<Attendance> attendances = attendanceRepository.findByAttendanceDate(date);
        List<Attendance> classAttendances = attendances.stream()
                .filter(a -> studentIdsInClass.contains(a.getStudentId()))
                .toList();
                
        long totalStudents = classStudents.size();
        long totalMarked = classAttendances.size();
        long totalPresent = classAttendances.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        long totalAbsent = classAttendances.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();

        return new com.lumo.backend.attendance.dto.DailyAttendanceStatsResponse(
            date, totalStudents, totalMarked, totalPresent, totalAbsent
        );
    }

    public List<com.lumo.backend.attendance.dto.AttendanceHistoryRecord> getAttendanceHistoryByDate(LocalDate date) {
        if (date == null) date = LocalDate.now();
        List<Attendance> attendances = attendanceRepository.findByAttendanceDate(date);
        
        return attendances.stream().map(a -> {
            Long studentDbId = null;
            try {
                studentDbId = Long.parseLong(a.getStudentId());
            } catch (NumberFormatException e) {
                // In case the DB has old ALP260001 format instead of numeric IDs.
            }
            
            com.lumo.backend.students.entity.Student student = null;
            if (studentDbId != null) {
                student = studentRepository.findById(studentDbId).orElse(null);
            }
            
            String regId = student != null ? student.getStudentId() : a.getStudentId();
            String name = student != null ? (student.getFirstName() + " " + student.getLastName()) : "Unknown";
            String className = student != null && student.getSchoolClass() != null ? student.getSchoolClass().getName() : "Unknown";

            return new com.lumo.backend.attendance.dto.AttendanceHistoryRecord(
                studentDbId, regId, name, className, a.getStatus()
            );
        }).toList();
    }

    public com.lumo.backend.attendance.dto.StudentAttendanceSummaryResponse getMyAttendance(String authorizationHeader, Integer month, Integer year) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String token = authorizationHeader.replace("Bearer ", "").trim();
        
        String studentId = jwtService.extractStudentSubject(token);
        if (studentId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        
        com.lumo.backend.students.entity.Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Student not found"));
        
        List<Attendance> allAttendance = attendanceRepository.findByStudentId(studentId);
        
        int targetMonth = month != null ? month : LocalDate.now().getMonthValue();
        int targetYear = year != null ? year : LocalDate.now().getYear();
        
        List<Attendance> filtered = allAttendance.stream()
            .filter(a -> a.getAttendanceDate() != null && 
                         a.getAttendanceDate().getMonthValue() == targetMonth && 
                         a.getAttendanceDate().getYear() == targetYear)
            .toList();
            
        long present = filtered.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        long absent = filtered.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();
        long late = filtered.stream().filter(a -> "LATE".equalsIgnoreCase(a.getStatus())).count();
        long halfDay = filtered.stream().filter(a -> "HALF_DAY".equalsIgnoreCase(a.getStatus())).count();
        long holiday = filtered.stream().filter(a -> "HOLIDAY".equalsIgnoreCase(a.getStatus())).count();
        
        long totalWorkingDays = present + absent + late + halfDay;
        
        double percentage = 0.0;
        if (totalWorkingDays > 0) {
            percentage = Math.round(((double) (present + late + halfDay) / totalWorkingDays) * 100.0);
        }
        
        String name = student.getFirstName() + (student.getLastName() != null ? " " + student.getLastName() : "");
        String className = student.getStudentClass() != null ? student.getStudentClass() : "";
        if (student.getSection() != null) {
            className = "Class " + className + "-" + student.getSection().getName();
        } else {
            className = "Class " + className;
        }
        
        return new com.lumo.backend.attendance.dto.StudentAttendanceSummaryResponse(
            name,
            className,
            percentage,
            present,
            absent,
            late,
            halfDay,
            holiday,
            filtered
        );
    }
}
