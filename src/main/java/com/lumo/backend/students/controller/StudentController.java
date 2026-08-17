package com.lumo.backend.students.controller;

import com.lumo.backend.students.dto.StudentAdd;
import com.lumo.backend.students.dto.StudentAddResponse;
import com.lumo.backend.students.dto.StudentResponse;
import com.lumo.backend.students.dto.StudentUpdate;
import com.lumo.backend.students.dto.RefreshTokenRequest;
import com.lumo.backend.students.dto.RefreshTokenResponse;
import com.lumo.backend.students.dto.StudentLoginRequest;
import com.lumo.backend.students.dto.StudentLoginResponse;
import com.lumo.backend.students.dto.BirthdayStudentResponse;
import com.lumo.backend.students.service.StudentService;
import java.util.List;
import com.lumo.backend.students.repository.StudentRepository;
import com.lumo.backend.students.entity.Student;
import com.lumo.backend.service.FileStorageService;
import com.lumo.backend.security.JwtService;
import java.util.Map;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final FileStorageService fileStorageService;
    private final JwtService jwtService;

    // POST /api/students/add
    @PostMapping("/add")
    public ResponseEntity<StudentAddResponse> addStudent(@RequestBody StudentAdd request ,  @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        System.out.println(authorizationHeader);
        if (authorizationHeader == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new StudentAddResponse("Unauthorized", null, null));
        }
        return ResponseEntity.ok(studentService.addStudent(request , authorizationHeader));
    }

    // POST /api/students/login
    @PostMapping("/login")
    public ResponseEntity<StudentLoginResponse> loginStudent(@RequestBody StudentLoginRequest request) {
        Map<String, String> tokens = studentService.loginStudent(request);
        if (tokens == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new StudentLoginResponse(false, "Invalid mobile number or date of birth", null, null));
        }
        return ResponseEntity.ok(new StudentLoginResponse(true, "Student login successful", tokens.get("accessToken"), tokens.get("refreshToken")));
    }

    // POST /api/students/refresh-token
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        String newAccessToken = studentService.refreshAccessToken(request.refreshToken());
        if (newAccessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RefreshTokenResponse(false, "Invalid or expired refresh token", null));
        }
        return ResponseEntity.ok(new RefreshTokenResponse(true, "Token refreshed successfully", newAccessToken));
    }

    // GET /api/students/{studentId}
    @GetMapping("/{studentId}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentService.getStudentWithTeacher(studentId));
    }

    // PUT /api/students/{studentId}
    @PutMapping("/{studentId}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long studentId,
            @RequestBody StudentUpdate request) {
        return ResponseEntity.ok(studentService.updateStudent(studentId, request));
    }

    // DELETE /api/students/{studentId}
    @DeleteMapping("/{studentId}")
    public ResponseEntity<Map<String, Object>> deleteStudent(
            @PathVariable String studentId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access.");
        }
        String token = authorizationHeader.substring(7).trim();
        String adminEmail = jwtService.extractAdminSubject(token);
        String teacherEmail = jwtService.extractTeacherSubject(token);
        if (adminEmail == null && teacherEmail == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Admins or Teachers can delete students.");
        }

        studentService.deleteStudent(studentId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Student deleted successfully"
        ));
    }

    // GET /api/students/me
    @GetMapping("/me")
    public ResponseEntity<StudentResponse> getStudentMe(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return ResponseEntity.ok(studentService.getStudentByToken(authorizationHeader));
    }

    // GET /api/students/upcoming-birthdays
    @GetMapping("/upcoming-birthdays")
    public ResponseEntity<List<BirthdayStudentResponse>> getUpcomingBirthdays(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(value = "days", defaultValue = "30") Integer days
    ) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access.");
        }
        String token = authorizationHeader.substring(7).trim();
        String adminEmail = jwtService.extractAdminSubject(token);
        String teacherEmail = jwtService.extractTeacherSubject(token);
        String studentId = jwtService.extractStudentSubject(token);

        if (adminEmail == null && teacherEmail == null && studentId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token.");
        }

        return ResponseEntity.ok(studentService.getUpcomingBirthdays(days));
    }

    // GET /api/students/class/{className}
    @GetMapping("/class/{className}")
    public ResponseEntity<?> getStudentsByClass(
            @PathVariable String className,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        if (page != null && size != null) {
            return ResponseEntity.ok(studentService.getStudentsByClass(className, page, size));
        }
        return ResponseEntity.ok(studentService.getStudentsByClass(className));
    }

    // GET /api/students/class/id/{classId}
    @GetMapping("/class/id/{classId}")
    public ResponseEntity<?> getStudentsByClassId(
            @PathVariable Long classId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        if (page != null && size != null) {
            return ResponseEntity.ok(studentService.getStudentsByClassId(classId, page, size));
        }
        return ResponseEntity.ok(studentService.getStudentsByClassId(classId));
    }

    // GET /api/students/class/{classId}/section/{sectionId}
    @GetMapping("/class/{classId}/section/{sectionId}")
    public ResponseEntity<?> getStudentsByClassAndSection(
            @PathVariable Long classId,
            @PathVariable Long sectionId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        if (page != null && size != null) {
            return ResponseEntity.ok(studentService.getStudentsByClassAndSection(classId, sectionId, page, size));
        }
        return ResponseEntity.ok(studentService.getStudentsByClassAndSection(classId, sectionId));
    }

    // POST /api/students/{studentId}/photo
    @PostMapping("/{studentId}/photo")
    public ResponseEntity<Map<String, Object>> uploadStudentPhoto(
            @PathVariable("studentId") String studentIdStr,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        if ("temp".equalsIgnoreCase(studentIdStr)) {
            checkTempPhotoPermission(authorizationHeader);
            String photoUrl = fileStorageService.saveTempStudentPhoto(file);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Profile photo uploaded successfully",
                "profilePhotoUrl", photoUrl
            ));
        }

        Student student = findStudentByIdOrAlphanumeric(studentIdStr);
        checkPhotoPermission(authorizationHeader, student);

        String photoUrl = fileStorageService.saveStudentPhoto(file, student.getId());

        student.setProfilePhotoUrl(photoUrl);
        studentRepository.save(student);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Profile photo uploaded successfully",
            "profilePhotoUrl", photoUrl
        ));
    }

    // GET /api/students/{studentId}/photo
    @GetMapping("/{studentId}/photo")
    public ResponseEntity<Map<String, String>> getStudentPhoto(
            @PathVariable("studentId") String studentIdStr) {

        Student student = findStudentByIdOrAlphanumeric(studentIdStr);
        return ResponseEntity.ok(Map.of(
            "profilePhotoUrl", student.getProfilePhotoUrl() != null ? student.getProfilePhotoUrl() : ""
        ));
    }

    // DELETE /api/students/{studentId}/photo
    @DeleteMapping("/{studentId}/photo")
    public ResponseEntity<Map<String, Object>> deleteStudentPhoto(
            @PathVariable("studentId") String studentIdStr,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        Student student = findStudentByIdOrAlphanumeric(studentIdStr);
        checkPhotoPermission(authorizationHeader, student);

        if (student.getProfilePhotoUrl() != null && !student.getProfilePhotoUrl().isEmpty()) {
            fileStorageService.deleteFile(student.getProfilePhotoUrl());
            student.setProfilePhotoUrl(null);
            studentRepository.save(student);
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Profile photo deleted successfully"
        ));
    }

    private Student findStudentByIdOrAlphanumeric(String identifier) {
        try {
            Long id = Long.parseLong(identifier);
            java.util.Optional<Student> studentOpt = studentRepository.findById(id);
            if (studentOpt.isPresent()) {
                return studentOpt.get();
            }
        } catch (NumberFormatException e) {
            // Ignore, try alphanumeric
        }
        return studentRepository.findByStudentId(identifier)
                .orElseThrow(() -> new com.lumo.backend.exception.StorageFileNotFoundException("Student not found with ID: " + identifier));
    }

    private void checkPhotoPermission(String authHeader, Student student) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access.");
        }
        String token = authHeader.substring(7).trim();
        String adminEmail = jwtService.extractAdminSubject(token);
        if (adminEmail != null) {
            return; // Admin allowed
        }
        String teacherEmail = jwtService.extractTeacherSubject(token);
        if (teacherEmail != null) {
            return; // Teacher allowed
        }
        String studentId = jwtService.extractStudentSubject(token);
        if (studentId != null && studentId.equals(student.getStudentId())) {
            return; // Student themselves allowed
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to modify this student's profile photo.");
    }

    private void checkTempPhotoPermission(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access.");
        }
        String token = authHeader.substring(7).trim();
        String adminEmail = jwtService.extractAdminSubject(token);
        if (adminEmail != null) {
            return; // Admin allowed
        }
        String teacherEmail = jwtService.extractTeacherSubject(token);
        if (teacherEmail != null) {
            return; // Teacher allowed
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to upload student photos.");
    }
}