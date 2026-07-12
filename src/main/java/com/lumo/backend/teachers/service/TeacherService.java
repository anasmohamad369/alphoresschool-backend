package com.lumo.backend.teachers.service;

import com.lumo.backend.teachers.dto.TeacherLogin;
import com.lumo.backend.teachers.dto.TeacherAdd;
import com.lumo.backend.teachers.dto.TeacherUpdateRequest;
import com.lumo.backend.teachers.dto.TeacherProfileResponse;
import com.lumo.backend.teachers.dto.GetAllTeacherProfile;
import com.lumo.backend.teachers.entity.Teacher;
import com.lumo.backend.teachers.repository.TeacherRepository;
import com.lumo.backend.admin.dto.GetAllStudentProfile;
import com.lumo.backend.admin.dto.StudentProfileResponse;
import com.lumo.backend.security.JwtService;
import com.lumo.backend.students.entity.Student;
import com.lumo.backend.students.repository.StudentRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final com.lumo.backend.classes.repository.SchoolClassRepository classRepository;
    private final com.lumo.backend.classes.repository.SectionRepository sectionRepository;

    public TeacherService(
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            com.lumo.backend.classes.repository.SchoolClassRepository classRepository,
            com.lumo.backend.classes.repository.SectionRepository sectionRepository) {
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.classRepository = classRepository;
        this.sectionRepository = sectionRepository;
    }

    @CacheEvict(value = {"teachersByEmail", "teachersByClass"}, allEntries = true)
    public boolean addTeacher(TeacherAdd request) {
        Teacher teacher = new Teacher();
        teacher.setEmailId(request.emailId());
        teacher.setPasswordHash(passwordEncoder.encode(request.passwordHash()));
        teacher.setName(request.name());
        teacher.setMobileNumber(request.mobileNumber());
        teacher.setClassTeacher(request.classTeacher());
        teacher.setClasses(request.classes());
        teacher.setSubjects(request.subjects());
        Teacher savedTeacher = teacherRepository.save(teacher);
        return savedTeacher.getId() != null;
    }

    @CacheEvict(value = {"teachersByEmail", "teachersByClass"}, allEntries = true)
    public boolean updateTeacher(TeacherUpdateRequest request) {
        Teacher teacher = teacherRepository.findByEmailId(request.emailId()).orElse(null);
        if (teacher == null) {
            return false;
        }
        teacher.setPasswordHash(passwordEncoder.encode(request.passwordHash()));
        teacher.setName(request.name());
        teacher.setMobileNumber(request.mobileNumber());
        teacher.setClassTeacher(request.classTeacher());
        teacher.setClasses(request.classes());
        teacher.setSubjects(request.subjects());
        Teacher savedTeacher = teacherRepository.save(teacher);
        return savedTeacher.getId() != null;
    }

    @CacheEvict(value = {"teachersByEmail", "teachersByClass"}, allEntries = true)
    public boolean updateTeacherById(Long id, TeacherUpdateRequest request) {
        Teacher teacher = teacherRepository.findById(id).orElse(null);
        if (teacher == null) {
            return false;
        }
        teacher.setEmailId(request.emailId());
        teacher.setPasswordHash(passwordEncoder.encode(request.passwordHash()));
        teacher.setName(request.name());
        teacher.setMobileNumber(request.mobileNumber());
        teacher.setClassTeacher(request.classTeacher());
        teacher.setClasses(request.classes());
        teacher.setSubjects(request.subjects());
        Teacher savedTeacher = teacherRepository.save(teacher);
        return savedTeacher.getId() != null;
    }

    @CacheEvict(value = {"teachersByEmail", "teachersByClass"}, allEntries = true)
    public boolean deleteTeacher(Long id) {
        if (teacherRepository.existsById(id)) {
            teacherRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public java.util.Map<String, String> loginTeacher(TeacherLogin request) {
        Teacher teacher = teacherRepository.findByEmailId(request.emailId()).orElse(null);
        if (teacher == null || !passwordEncoder.matches(request.passwordHash(), teacher.getPasswordHash())) {
            return null;
        }
        java.util.Map<String, String> tokens = new java.util.HashMap<>();
        tokens.put("accessToken", jwtService.generateTeacherToken(teacher.getEmailId()));
        tokens.put("refreshToken", jwtService.generateTeacherRefreshToken(teacher.getEmailId()));
        return tokens;
    }

    public String refreshAccessToken(String refreshToken) {
        String emailId = jwtService.extractTeacherSubject(refreshToken);
        if (emailId == null) {
            return null;
        }
        Teacher teacher = teacherRepository.findByEmailId(emailId).orElse(null);
        if (teacher == null) {
            return null;
        }
        return jwtService.generateTeacherToken(teacher.getEmailId());
    }

    public GetAllTeacherProfile getTeacherProfile(Integer page, Integer size) {
        if (page != null && size != null) {
            org.springframework.data.domain.Page<Teacher> teacherPage = teacherRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size));
            List<TeacherProfileResponse> rows = teacherPage.getContent().stream()
                    .map(teacher -> new TeacherProfileResponse(
                            teacher.getId(),
                            teacher.getEmailId(),
                            teacher.getName(),
                            teacher.getMobileNumber(),
                            teacher.getClassTeacher(),
                            teacher.getClasses() == null ? List.of() : List.copyOf(teacher.getClasses()),
                            teacher.getSubjects() == null ? List.of() : List.copyOf(teacher.getSubjects())))
                    .toList();
            return new GetAllTeacherProfile(rows, teacherPage.getNumber(), teacherPage.getSize(), teacherPage.getTotalElements(), teacherPage.getTotalPages(), teacherPage.isLast());
        }

        List<Teacher> teachers = teacherRepository.findAll();
        List<TeacherProfileResponse> rows = teachers.stream()
                .map(teacher -> new TeacherProfileResponse(
                        teacher.getId(),
                        teacher.getEmailId(),
                        teacher.getName(),
                        teacher.getMobileNumber(),
                        teacher.getClassTeacher(),
                        teacher.getClasses() == null ? List.of() : List.copyOf(teacher.getClasses()),
                        teacher.getSubjects() == null ? List.of() : List.copyOf(teacher.getSubjects())))
                .toList();
        return new GetAllTeacherProfile(rows);
    }

    public TeacherProfileResponse getTeacherById(Long id) {
        Teacher teacher = teacherRepository.findById(id).orElse(null);
        if (teacher == null) return null;
        return new TeacherProfileResponse(
                teacher.getId(),
                teacher.getEmailId(),
                teacher.getName(),
                teacher.getMobileNumber(),
                teacher.getClassTeacher(),
                teacher.getClasses() == null ? List.of() : List.copyOf(teacher.getClasses()),
                teacher.getSubjects() == null ? List.of() : List.copyOf(teacher.getSubjects()));
    }

    public GetAllStudentProfile getStudentProfile(String authorizationHeader, Integer page, Integer size) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring(7).trim();
        String email = jwtService.extractTeacherSubject(token);
        if (email == null) {
            return null;
        }
        Teacher teacher = teacherRepository.findByEmailId(email).orElse(null);
        if (teacher == null) {
            return null;
        }

        // 1. Resolve homeroom class teacher students
        List<Student> homeroomStudents = new java.util.ArrayList<>();
        String homeroom = teacher.getClassTeacher();
        if (homeroom != null && !homeroom.isBlank() && !"Not Assigned".equalsIgnoreCase(homeroom)) {
            String baseClass = homeroom;
            String sectionName = null;

            if (homeroom.contains("Section ")) {
                baseClass = homeroom.split("Section ")[0].trim();
                sectionName = homeroom.substring(homeroom.indexOf("Section ") + 8).trim();
            } else {
                for (com.lumo.backend.classes.entity.SchoolClass sc : classRepository.findAll()) {
                    if (homeroom.startsWith(sc.getName())) {
                        baseClass = sc.getName();
                        sectionName = homeroom.substring(baseClass.length()).trim();
                        break;
                    }
                }
            }

            if (sectionName != null && !sectionName.isBlank()) {
                com.lumo.backend.classes.entity.SchoolClass sc = classRepository.findByName(baseClass).orElse(null);
                if (sc != null) {
                    com.lumo.backend.classes.entity.Section sec = sectionRepository.findByNameAndSchoolClassId(sectionName, sc.getId()).orElse(null);
                    if (sec != null) {
                        homeroomStudents = studentRepository.findBySchoolClassIdAndSectionId(sc.getId(), sec.getId());
                    } else {
                        homeroomStudents = studentRepository.findByStudentClass(baseClass);
                    }
                } else {
                    homeroomStudents = studentRepository.findByStudentClass(baseClass);
                }
            } else {
                homeroomStudents = studentRepository.findByStudentClass(baseClass);
            }
        }

        // 2. Resolve students explicitly added by this teacher
        List<Long> addedIds = teacher.getStudentIds() == null ? List.of() : List.copyOf(teacher.getStudentIds());
        List<Student> addedStudents = addedIds.isEmpty() ? List.of() : studentRepository.findAllById(addedIds);

        // 3. Combine both lists (avoiding duplicates)
        Map<Long, Student> combinedStudentsMap = new LinkedHashMap<>();
        for (Student s : homeroomStudents) {
            combinedStudentsMap.put(s.getId(), s);
        }
        for (Student s : addedStudents) {
            combinedStudentsMap.put(s.getId(), s);
        }

        List<Student> allStudents = new java.util.ArrayList<>(combinedStudentsMap.values());

        if (page != null && size != null) {
            int totalElements = allStudents.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int start = page * size;
            if (start >= totalElements) {
                return new GetAllStudentProfile(List.of(), page, size, totalElements, totalPages, true);
            }
            int end = Math.min(start + size, totalElements);
            List<Student> pageStudents = allStudents.subList(start, end);

            List<StudentProfileResponse> rows = pageStudents.stream()
                    .map(this::toStudentProfile)
                    .toList();
            boolean last = (end == totalElements);
            return new GetAllStudentProfile(rows, page, size, totalElements, totalPages, last);
        }

        List<StudentProfileResponse> rows = allStudents.stream()
                .map(this::toStudentProfile)
                .toList();
        return new GetAllStudentProfile(rows);
    }

    public com.lumo.backend.teachers.dto.TeacherClassStatsResponse getTeacherClassStats(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return new com.lumo.backend.teachers.dto.TeacherClassStatsResponse(0);
        }
        String token = authorizationHeader.substring(7).trim();
        String email = jwtService.extractTeacherSubject(token);
        if (email == null) {
            return new com.lumo.backend.teachers.dto.TeacherClassStatsResponse(0);
        }
        Teacher teacher = teacherRepository.findByEmailId(email).orElse(null);
        if (teacher == null || teacher.getStudentIds() == null) {
            return new com.lumo.backend.teachers.dto.TeacherClassStatsResponse(0);
        }
        
        return new com.lumo.backend.teachers.dto.TeacherClassStatsResponse(teacher.getStudentIds().size());
    }

    public com.lumo.backend.teachers.dto.TeacherClassesResponse getMyAssignedClasses(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing token");
        }
        String token = authorizationHeader.substring(7).trim();
        String email = jwtService.extractTeacherSubject(token);
        if (email == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        Teacher teacher = teacherRepository.findByEmailId(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Teacher not found"));
        String homeroom = teacher.getClassTeacher();
        String baseClass = homeroom;
        String sectionName = null;

        if (homeroom != null) {
            if (homeroom.contains("Section ")) {
                baseClass = homeroom.split("Section ")[0].trim();
                sectionName = homeroom.substring(homeroom.indexOf("Section ") + 8).trim();
            } else {
                for (com.lumo.backend.classes.entity.SchoolClass sc : classRepository.findAll()) {
                    if (homeroom.startsWith(sc.getName())) {
                        baseClass = sc.getName();
                        sectionName = homeroom.substring(baseClass.length()).trim();
                        break;
                    }
                }
            }
        }
        
        return new com.lumo.backend.teachers.dto.TeacherClassesResponse(
            baseClass,
            sectionName,
            teacher.getClasses() == null ? List.of() : List.copyOf(teacher.getClasses())
        );
    }

    private StudentProfileResponse toStudentProfile(Student s) {
        return new StudentProfileResponse(
                s.getId(),
                s.getFirstName(),
                s.getLastName(),
                s.getMiddleName(),
                s.getMobileNumber(),
                s.getFatherName(),
                s.getMotherName(),
                s.getFatherAadharNumber(),
                s.getMotherAadharNumber(),
                s.getStudentAadharNumber(),
                s.getDateOfBirth(),
                s.getGender(),
                s.getStudentClass(),
                s.getProfilePhotoUrl()
            );
    }
}
