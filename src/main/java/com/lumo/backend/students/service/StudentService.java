package com.lumo.backend.students.service;

import com.lumo.backend.students.dto.StudentAdd;
import com.lumo.backend.students.dto.StudentAddResponse;
import com.lumo.backend.students.dto.StudentResponse;
import com.lumo.backend.students.dto.StudentUpdate;
import com.lumo.backend.students.dto.StudentLoginRequest;
import com.lumo.backend.students.dto.BirthdayStudentResponse;
import com.lumo.backend.students.entity.Student;
import com.lumo.backend.students.repository.StudentRepository;
import com.lumo.backend.teachers.entity.Teacher;
import com.lumo.backend.teachers.repository.TeacherRepository;
import com.lumo.backend.classes.entity.SchoolClass;
import com.lumo.backend.classes.entity.Section;
import com.lumo.backend.classes.repository.SchoolClassRepository;
import com.lumo.backend.classes.repository.SectionRepository;
import com.lumo.backend.attendance.repository.AttendanceRepository;
import com.lumo.backend.attendance.entity.Attendance;
import com.lumo.backend.security.JwtService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import com.lumo.backend.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final AttendanceRepository attendanceRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public StudentService(
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            SchoolClassRepository classRepository,
            SectionRepository sectionRepository,
            AttendanceRepository attendanceRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            FileStorageService fileStorageService) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.classRepository = classRepository;
        this.sectionRepository = sectionRepository;
        this.attendanceRepository = attendanceRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
    }

    public StudentAddResponse addStudent(StudentAdd request, String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "").trim();
        
        String adminEmail = jwtService.extractAdminSubject(token);
        
        if (adminEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Only Admins or Principals can add students.");
        }

        Teacher teacher = null;

        String mobile = request.mobileNumber();
        if (mobile == null || mobile.isBlank()) {
            throw new IllegalArgumentException("mobileNumber is required.");
        }
        if (studentRepository.findByMobileNumber(mobile).isPresent()) {
            throw new IllegalArgumentException(
                    "Student with mobile number " + mobile + " already exists.");
        }

        if (request.studentId() == null || request.studentId().isBlank()) {
            throw new IllegalArgumentException("studentId is required.");
        }
        if (studentRepository.findByStudentId(request.studentId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Student with admission number " + request.studentId() + " already exists.");
        }

        Student student = new Student();
        mapFields(student, request.firstName(), request.lastName(), request.middleName(),
                mobile, request.fatherName(), request.motherName(),
                request.dateOfBirth(),
                request.gender(), request.studentClass(), request.profilePhotoUrl());

        student.setStudentId(request.studentId());

        // Default password "123456"
        student.setPassword(passwordEncoder.encode("123456"));
        student.setStatus("ACTIVE");

        // Associate class if exists
        if (request.studentClass() != null && !request.studentClass().isBlank()) {
            SchoolClass sc = classRepository.findByName(request.studentClass()).orElse(null);
            if (sc == null) {
                // If not found by name, try to create it or auto-link
                sc = new SchoolClass();
                sc.setName(request.studentClass());
                sc = classRepository.save(sc);
            }
            student.setSchoolClass(sc);

            // Fetch or create section provided by request
            if (request.sectionName() != null && !request.sectionName().isBlank()) {
                String targetSection = request.sectionName();
                Section sec = sectionRepository.findByNameAndSchoolClassId(targetSection, sc.getId()).orElse(null);
                if (sec == null) {
                    sec = new Section();
                    sec.setName(targetSection);
                    sec.setSchoolClass(sc);
                    sec = sectionRepository.save(sec);
                }
                student.setSection(sec);
            } else {
                student.setSection(null);
            }
        }

        Student saved = studentRepository.save(student);
        
        if (teacher == null && request.studentClass() != null && !request.studentClass().isBlank()) {
            // Admin added the student, try to link to class teacher if exists
            if (request.sectionName() != null && !request.sectionName().isBlank()) {
                String targetSection = request.sectionName();
                String match1 = request.studentClass() + "Section " + targetSection;
                String match2 = request.studentClass() + targetSection;
                
                teacher = teacherRepository.findFirstByClassTeacher(match1).orElse(null);
                if (teacher == null) {
                    teacher = teacherRepository.findFirstByClassTeacher(match2).orElse(null);
                }
            }
            if (teacher == null) {
                teacher = teacherRepository.findFirstByClassTeacher(request.studentClass()).orElse(null);
            }
        }

        if (teacher != null) {
            teacher.getStudentIds().add(saved.getId());
            teacherRepository.save(teacher);
        }
        
        return new StudentAddResponse("Student added successfully", saved.getId(), saved.getProfilePhotoUrl() != null ? saved.getProfilePhotoUrl() : "");
    }

    public List<StudentResponse> getStudentsByClass(String className) {
        return studentRepository.findByStudentClass(className).stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    public com.lumo.backend.gallery.dto.PaginatedResponse<StudentResponse> getStudentsByClass(String className, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Student> studentPage = studentRepository.findByStudentClass(className, pageable);
        List<StudentResponse> content = studentPage.getContent().stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
        return new com.lumo.backend.gallery.dto.PaginatedResponse<>(
                content,
                studentPage.getNumber(),
                studentPage.getSize(),
                studentPage.getTotalElements(),
                studentPage.getTotalPages(),
                studentPage.isLast()
        );
    }

    public List<StudentResponse> getStudentsByClassId(Long classId) {
        return studentRepository.findBySchoolClassId(classId).stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    public com.lumo.backend.gallery.dto.PaginatedResponse<StudentResponse> getStudentsByClassId(Long classId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Student> studentPage = studentRepository.findBySchoolClassId(classId, pageable);
        List<StudentResponse> content = studentPage.getContent().stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
        return new com.lumo.backend.gallery.dto.PaginatedResponse<>(
                content,
                studentPage.getNumber(),
                studentPage.getSize(),
                studentPage.getTotalElements(),
                studentPage.getTotalPages(),
                studentPage.isLast()
        );
    }

    public List<StudentResponse> getStudentsByClassAndSection(Long classId, Long sectionId) {
        return studentRepository.findBySchoolClassIdAndSectionId(classId, sectionId).stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    public com.lumo.backend.gallery.dto.PaginatedResponse<StudentResponse> getStudentsByClassAndSection(Long classId, Long sectionId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Student> studentPage = studentRepository.findBySchoolClassIdAndSectionId(classId, sectionId, pageable);
        List<StudentResponse> content = studentPage.getContent().stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
        return new com.lumo.backend.gallery.dto.PaginatedResponse<>(
                content,
                studentPage.getNumber(),
                studentPage.getSize(),
                studentPage.getTotalElements(),
                studentPage.getTotalPages(),
                studentPage.isLast()
        );
    }

    public Map<String, String> loginStudent(StudentLoginRequest request) {
        Student student = studentRepository.findByMobileNumber(request.mobileNumber()).orElse(null);
        if (student == null || student.getDateOfBirth() == null || !student.getDateOfBirth().equals(request.dateOfBirth())) {
            return null;
        }
        
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", jwtService.generateStudentToken(student.getStudentId()));
        tokens.put("refreshToken", jwtService.generateStudentRefreshToken(student.getStudentId()));
        return tokens;
    }

    public String refreshAccessToken(String refreshToken) {
        String studentId = jwtService.extractStudentSubject(refreshToken);
        if (studentId == null) {
            return null;
        }
        // Verify student still exists
        Student student = studentRepository.findByStudentId(studentId).orElse(null);
        if (student == null) {
            return null;
        }
        return jwtService.generateStudentToken(student.getStudentId());
    }

    public StudentResponse getStudentByToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String token = authorizationHeader.replace("Bearer ", "").trim();
        String studentId = jwtService.extractStudentSubject(token);
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        return buildResponse(student);
    }

    public StudentResponse updateStudent(Long studentId, StudentUpdate request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Student not found with id: " + studentId));

        mapFields(student, request.firstName(), request.lastName(), request.middleName(),
                request.mobileNumber(), request.fatherName(), request.motherName(),
                request.dateOfBirth(),
                request.gender(), request.studentClass(), request.profilePhotoUrl());

        Student updated = studentRepository.save(student);
        return buildResponse(updated);
    }

    public StudentResponse getStudentWithTeacher(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Student not found with id: " + studentId));
        return buildResponse(student);
    }

    private void mapFields(Student student, String firstName, String lastName,
            String middleName, String mobileNumber, String fatherName, String motherName,
            String dateOfBirth, String gender, String studentClass, String profilePhotoUrl) {
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setMiddleName(middleName);
        student.setMobileNumber(mobileNumber);
        student.setFatherName(fatherName);
        student.setMotherName(motherName);
        student.setDateOfBirth(dateOfBirth);
        student.setGender(gender);
        student.setStudentClass(studentClass);
        student.setProfilePhotoUrl(profilePhotoUrl);
    }

    private StudentResponse buildResponse(Student student) {
        Optional<Teacher> teacherOpt = Optional.empty();
        if (student.getSection() != null && student.getSection().getName() != null) {
            String match1 = student.getStudentClass() + "Section " + student.getSection().getName();
            String match2 = student.getStudentClass() + student.getSection().getName();
            
            teacherOpt = teacherRepository.findFirstByClassTeacher(match1);
            if (teacherOpt.isEmpty()) {
                teacherOpt = teacherRepository.findFirstByClassTeacher(match2);
            }
        }
        
        if (teacherOpt.isEmpty()) {
            teacherOpt = teacherRepository.findFirstByClassTeacher(student.getStudentClass());
        }

        Long teacherId = null;
        String teacherName = null;
        String teacherEmail = null;
        String teacherMobile = null;

        if (teacherOpt.isPresent()) {
            Teacher t = teacherOpt.get();
            teacherId = t.getId();
            teacherName = t.getName();
            teacherEmail = t.getEmailId();
            teacherMobile = t.getMobileNumber();
        }
        
        List<Attendance> attendance = attendanceRepository.findByStudentId(student.getStudentId());
        Long classId = student.getSchoolClass() != null ? student.getSchoolClass().getId() : null;

        return new StudentResponse(
                student.getId(), student.getFirstName(), student.getLastName(),
                student.getMiddleName(), student.getMobileNumber(),
                student.getFatherName(), student.getMotherName(),
                student.getDateOfBirth(), student.getGender(), student.getStudentClass(),
                classId, teacherId, teacherName, teacherEmail, teacherMobile, attendance,
                student.getStudentId(), student.getProfilePhotoUrl() != null ? student.getProfilePhotoUrl() : ""
            );
    }

    public List<BirthdayStudentResponse> getUpcomingBirthdays(int daysAhead) {
        List<Student> students = studentRepository.findAll();
        List<BirthdayStudentResponse> upcoming = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Student s : students) {
            LocalDate dob = parseDate(s.getDateOfBirth());
            if (dob != null) {
                LocalDate bdayThisYear = dob.withYear(today.getYear());
                if (bdayThisYear.isBefore(today)) {
                    bdayThisYear = bdayThisYear.plusYears(1);
                }

                long daysBetween = ChronoUnit.DAYS.between(today, bdayThisYear);
                if (daysBetween >= 0 && daysBetween <= daysAhead) {
                    String className = s.getSchoolClass() != null ? s.getSchoolClass().getName() : s.getStudentClass();
                    upcoming.add(new BirthdayStudentResponse(
                            s.getId(),
                            s.getStudentId(),
                            s.getFirstName(),
                            s.getLastName(),
                            s.getDateOfBirth(),
                            className,
                            s.getProfilePhotoUrl() != null ? s.getProfilePhotoUrl() : "",
                            daysBetween
                    ));
                }
            }
        }

        upcoming.sort(Comparator.comparingLong(BirthdayStudentResponse::daysUntilBirthday));
        return upcoming;
    }

    public void deleteStudent(String identifier) {
        Student student = null;
        try {
            Long id = Long.parseLong(identifier);
            student = studentRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            // ignore, try studentId
        }

        if (student == null) {
            student = studentRepository.findByStudentId(identifier)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found with ID: " + identifier));
        }

        // Remove student ID from any linked teacher
        List<Teacher> teachers = teacherRepository.findAll();
        for (Teacher t : teachers) {
            if (t.getStudentIds() != null && t.getStudentIds().contains(student.getId())) {
                t.getStudentIds().remove(student.getId());
                teacherRepository.save(t);
            }
        }

        // Remove profile photo if present
        if (student.getProfilePhotoUrl() != null && !student.getProfilePhotoUrl().isBlank()) {
            try {
                fileStorageService.deleteFile(student.getProfilePhotoUrl());
            } catch (Exception e) {
                // ignore storage exception
            }
        }

        studentRepository.delete(student);
    }

    private LocalDate parseDate(String dobStr) {
        if (dobStr == null || dobStr.isBlank()) return null;
        String clean = dobStr.trim();
        String[] formats = {"yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd"};
        for (String f : formats) {
            try {
                return LocalDate.parse(clean, DateTimeFormatter.ofPattern(f));
            } catch (Exception e) {
                // Try next
            }
        }
        return null;
    }
}