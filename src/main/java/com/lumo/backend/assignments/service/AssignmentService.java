package com.lumo.backend.assignments.service;

import com.lumo.backend.assignments.dto.AssignmentRequest;
import com.lumo.backend.assignments.entity.Assignment;
import com.lumo.backend.assignments.repository.AssignmentRepository;
import com.lumo.backend.classes.entity.SchoolClass;
import com.lumo.backend.classes.entity.Section;
import com.lumo.backend.classes.repository.SchoolClassRepository;
import com.lumo.backend.classes.repository.SectionRepository;
import com.lumo.backend.students.repository.StudentRepository;
import com.lumo.backend.assignments.repository.HomeworkSubmissionRepository;
import com.lumo.backend.assignments.dto.HomeworkMarkRequest;
import com.lumo.backend.assignments.dto.HomeworkSubmissionResponse;
import com.lumo.backend.assignments.dto.ClassHomeworkMarkRequest;
import com.lumo.backend.assignments.dto.ClassHomeworkSubmissionResponse;
import com.lumo.backend.assignments.entity.HomeworkSubmission;
import com.lumo.backend.students.entity.Student;
import com.lumo.backend.students.dto.StudentResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final com.lumo.backend.security.JwtService jwtService;
    private final com.lumo.backend.teachers.repository.TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final HomeworkSubmissionRepository homeworkSubmissionRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            SchoolClassRepository classRepository,
            SectionRepository sectionRepository,
            com.lumo.backend.security.JwtService jwtService,
            com.lumo.backend.teachers.repository.TeacherRepository teacherRepository,
            com.lumo.backend.students.repository.StudentRepository studentRepository,
            HomeworkSubmissionRepository homeworkSubmissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.classRepository = classRepository;
        this.sectionRepository = sectionRepository;
        this.jwtService = jwtService;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.homeworkSubmissionRepository = homeworkSubmissionRepository;
    }

    public Assignment createAssignment(AssignmentRequest request, String authorizationHeader) {
        return createAssignments(java.util.List.of(request), authorizationHeader).get(0);
    }

    public List<Assignment> createAssignments(List<AssignmentRequest> requests, String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing token");
        }
        String token = authorizationHeader.substring(7).trim();
        String email = jwtService.extractTeacherSubject(token);
        if (email == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid teacher token");
        }
        
        com.lumo.backend.teachers.entity.Teacher teacher = teacherRepository.findByEmailId(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Teacher not found"));

        // Extract base class name if it contains section info (e.g., "Grade 10Section Lion" -> "Grade 10")
        String homeroom = teacher.getClassTeacher();
        if (homeroom != null) {
            if (homeroom.contains("Section ")) {
                homeroom = homeroom.split("Section ")[0].trim();
            } else {
                for (SchoolClass sClass : classRepository.findAll()) {
                    if (homeroom.startsWith(sClass.getName())) {
                        homeroom = sClass.getName();
                        break;
                    }
                }
            }
        }

        List<Assignment> savedAssignments = new java.util.ArrayList<>();

        for (AssignmentRequest request : requests) {
            SchoolClass sc = classRepository.findById(request.classId())
                    .orElseThrow(() -> new IllegalArgumentException("Class not found: " + request.classId()));
            
            // Authorization: Check if teacher is assigned to this class
            if (homeroom == null || !homeroom.equals(sc.getName())) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "You can only upload homework for your assigned homeroom class.");
            }

            Section section = null;
            if (request.sectionId() != null) {
                section = sectionRepository.findById(request.sectionId())
                        .orElseThrow(() -> new IllegalArgumentException("Section not found: " + request.sectionId()));
            }

            Assignment assignment = new Assignment();
            assignment.setSchoolClass(sc);
            assignment.setSection(section);
            assignment.setType(request.type());
            assignment.setTitle(request.title());
            assignment.setDescription(request.description());
            assignment.setImageUrl(request.imageUrl());
            assignment.setAssignedDate(request.assignedDate() != null ? request.assignedDate() : LocalDate.now());
            assignment.setDueDate(request.dueDate());
            assignment.setCreatedBy(teacher.getName());

            savedAssignments.add(assignmentRepository.save(assignment));
        }

        return savedAssignments;
    }

    public List<Assignment> getAssignmentsByAssignedDate(LocalDate date, Long classId) {
        if (date == null) date = LocalDate.now();
        if (classId != null) {
            return assignmentRepository.findByAssignedDateAndSchoolClassId(date, classId);
        }
        return assignmentRepository.findByAssignedDate(date);
    }

    public List<Assignment> getAssignmentsByClassAndSection(Long classId, Long sectionId) {
        if (sectionId != null) {
            return assignmentRepository.findBySchoolClassIdAndSectionId(classId, sectionId);
        }
        return assignmentRepository.findBySchoolClassId(classId);
    }

    public List<Assignment> getMyAssignments(String authorizationHeader, LocalDate date) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing token");
        }
        String token = authorizationHeader.substring(7).trim();
        String studentId = jwtService.extractStudentSubject(token);
        if (studentId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid student token");
        }
        com.lumo.backend.students.entity.Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Student not found"));
        
        if (student.getSchoolClass() == null) {
            return java.util.List.of();
        }

        Long classId = student.getSchoolClass().getId();
        Long sectionId = student.getSection() != null ? student.getSection().getId() : null;

        if (date != null) {
            if (sectionId != null) {
                return assignmentRepository.findByAssignedDateAndSchoolClassIdAndSectionId(date, classId, sectionId);
            }
            return assignmentRepository.findByAssignedDateAndSchoolClassId(date, classId);
        }

        return getAssignmentsByClassAndSection(classId, sectionId);
    }

    public HomeworkSubmissionResponse markSubmission(Long assignmentId, HomeworkMarkRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
            
        HomeworkSubmission submission = homeworkSubmissionRepository.findByAssignmentIdAndStudentId(assignmentId, request.studentId())
            .orElse(new HomeworkSubmission());
            
        submission.setAssignment(assignment);
        submission.setStudentId(request.studentId());
        submission.setStatus(request.status());
        submission.setSubmissionDate(request.submissionDate() != null ? request.submissionDate() : LocalDate.now());
        submission.setTeacherRemarks(request.teacherRemarks());
        submission.setAttachmentUrl(request.attachmentUrl());
        
        HomeworkSubmission saved = homeworkSubmissionRepository.save(submission);
        
        String studentName = "Unknown";
        try {
            Long studentDbId = Long.parseLong(request.studentId());
            Student s = studentRepository.findById(studentDbId).orElse(null);
            if (s != null) {
                studentName = s.getFirstName() + " " + s.getLastName();
            }
        } catch (Exception ignored) {}
        
        return new HomeworkSubmissionResponse(saved.getId(), assignmentId, request.studentId(), studentName, saved.getStatus(), saved.getSubmissionDate(), saved.getTeacherRemarks(), saved.getAttachmentUrl());
    }

    public ClassHomeworkSubmissionResponse markClassHomework(Long assignmentId, ClassHomeworkMarkRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
            
        LocalDate date = request.submissionDate() != null ? request.submissionDate() : LocalDate.now();
        List<String> targetStudentsIds = request.studentIdsToMark() != null ? request.studentIdsToMark() : List.of();
        
        List<Student> students;
        if (assignment.getSection() != null) {
            students = studentRepository.findBySchoolClassIdAndSectionId(assignment.getSchoolClass().getId(), assignment.getSection().getId());
        } else {
            students = studentRepository.findBySchoolClassId(assignment.getSchoolClass().getId());
        }
        
        List<StudentResponse> targetStatusStudents = new java.util.ArrayList<>();
        List<StudentResponse> defaultStatusStudents = new java.util.ArrayList<>();
        
        for (Student student : students) {
            boolean isTarget = targetStudentsIds.contains(student.getId().toString()) || targetStudentsIds.contains(student.getStudentId());
            
            String lookupId = student.getId().toString();
            HomeworkSubmission submission = homeworkSubmissionRepository.findByAssignmentIdAndStudentId(assignmentId, lookupId)
                .orElse(new HomeworkSubmission());
                
            submission.setAssignment(assignment);
            submission.setStudentId(lookupId);
            submission.setSubmissionDate(date);
            submission.setStatus(isTarget ? request.targetStatus() : request.defaultStatus());
            
            homeworkSubmissionRepository.save(submission);
            
            StudentResponse resp = new StudentResponse(
                student.getId(), student.getFirstName(), student.getLastName(),
                student.getMiddleName(), student.getMobileNumber(),
                student.getFatherName(), student.getMotherName(),
                student.getDateOfBirth(), student.getGender(), student.getStudentClass(),
                null, null, null, null, null, null, student.getStudentId(), student.getProfilePhotoUrl()
            );
            if (isTarget) {
                targetStatusStudents.add(resp);
            } else {
                defaultStatusStudents.add(resp);
            }
        }
        
        return new ClassHomeworkSubmissionResponse(assignmentId, date, targetStatusStudents, defaultStatusStudents);
    }

    public List<HomeworkSubmissionResponse> getSubmissionsByAssignment(Long assignmentId) {
        return homeworkSubmissionRepository.findByAssignmentId(assignmentId).stream()
            .map(sub -> {
                String studentName = "Unknown";
                try {
                    Long studentDbId = Long.parseLong(sub.getStudentId());
                    Student s = studentRepository.findById(studentDbId).orElse(null);
                    if (s != null) {
                        studentName = s.getFirstName() + " " + s.getLastName();
                    }
                } catch (Exception ignored) {}
                return new HomeworkSubmissionResponse(sub.getId(), sub.getAssignment().getId(), sub.getStudentId(), studentName, sub.getStatus(), sub.getSubmissionDate(), sub.getTeacherRemarks(), sub.getAttachmentUrl());
            }).toList();
    }

    public List<HomeworkSubmissionResponse> getSubmissionsByStudent(String studentId) {
        return homeworkSubmissionRepository.findByStudentId(studentId).stream()
            .map(sub -> {
                String studentName = "Unknown";
                try {
                    Long studentDbId = Long.parseLong(sub.getStudentId());
                    Student s = studentRepository.findById(studentDbId).orElse(null);
                    if (s != null) {
                        studentName = s.getFirstName() + " " + s.getLastName();
                    }
                } catch (Exception ignored) {}
                return new HomeworkSubmissionResponse(sub.getId(), sub.getAssignment().getId(), sub.getStudentId(), studentName, sub.getStatus(), sub.getSubmissionDate(), sub.getTeacherRemarks(), sub.getAttachmentUrl());
            }).toList();
    }
}
