package com.lumo.backend.teachers.controller;

import com.lumo.backend.teachers.dto.TeacherAdd;
import com.lumo.backend.teachers.dto.TeacherLogin;
import com.lumo.backend.teachers.dto.TeacherUpdateRequest;
import com.lumo.backend.teachers.dto.TeacherResponse;
import com.lumo.backend.teachers.dto.TeacherAddResponse;
import com.lumo.backend.teachers.dto.GetAllTeacherProfile;
import com.lumo.backend.admin.dto.GetAllStudentProfile;
import com.lumo.backend.teachers.service.TeacherService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/admin")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping("/add-teacher")
    public ResponseEntity<TeacherAddResponse> addTeacher(
            @Valid @RequestBody TeacherAdd request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        boolean ok = teacherService.addTeacher(request);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TeacherAddResponse(false, "Failed to add teacher"));
        }
        return ResponseEntity.ok(new TeacherAddResponse(true, "Teacher added successfully"));
    }

    @PostMapping("/update-teacher")
    public ResponseEntity<TeacherAddResponse> updateTeacher(
            @Valid @RequestBody TeacherUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        boolean ok = teacherService.updateTeacher(request);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TeacherAddResponse(false, "Failed to update teacher"));
        }
        return ResponseEntity.ok(new TeacherAddResponse(true, "Teacher updated successfully"));
    }

    @org.springframework.web.bind.annotation.PutMapping("/update-teacher/{id}")
    public ResponseEntity<TeacherAddResponse> updateTeacherById(
            @PathVariable Long id,
            @Valid @RequestBody TeacherUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        boolean ok = teacherService.updateTeacherById(id, request);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TeacherAddResponse(false, "Failed to update teacher. Teacher might not exist."));
        }
        return ResponseEntity.ok(new TeacherAddResponse(true, "Teacher updated successfully"));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/delete-teacher/{id}")
    public ResponseEntity<TeacherAddResponse> deleteTeacher(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        boolean ok = teacherService.deleteTeacher(id);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TeacherAddResponse(false, "Failed to delete teacher. Teacher might not exist."));
        }
        return ResponseEntity.ok(new TeacherAddResponse(true, "Teacher deleted successfully"));
    }

    @PostMapping("/login-teacher")
    public ResponseEntity<TeacherResponse> loginTeacher(
            @Valid @RequestBody TeacherLogin request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        java.util.Map<String, String> tokens = teacherService.loginTeacher(request);
        if (tokens == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TeacherResponse(false, "Invalid mobile number or date of birth", null, null));
        }
        return ResponseEntity.ok(new TeacherResponse(true, "Teacher login successful", tokens.get("accessToken"), tokens.get("refreshToken")));
    }

    @PostMapping("/login-teacher/refresh-token")
    public ResponseEntity<com.lumo.backend.students.dto.RefreshTokenResponse> refreshTeacherToken(
            @RequestBody com.lumo.backend.students.dto.RefreshTokenRequest request) {
        String newAccessToken = teacherService.refreshAccessToken(request.refreshToken());
        if (newAccessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new com.lumo.backend.students.dto.RefreshTokenResponse(false, "Invalid or expired refresh token", null));
        }
        return ResponseEntity.ok(new com.lumo.backend.students.dto.RefreshTokenResponse(true, "Token refreshed successfully", newAccessToken));
    }

    @GetMapping("/get-all")
    public ResponseEntity<GetAllTeacherProfile> getAllTeacherProfile(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        GetAllTeacherProfile profile = teacherService.getTeacherProfile(page, size);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/get-all-students")
    public ResponseEntity<GetAllStudentProfile> getAllStudentProfile(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        GetAllStudentProfile profile = teacherService.getStudentProfile(authorizationHeader, page, size);
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/get-teacher/{id}")
    public ResponseEntity<com.lumo.backend.teachers.dto.TeacherProfileResponse> getTeacherById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        com.lumo.backend.teachers.dto.TeacherProfileResponse profile = teacherService.getTeacherById(id);
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/my-class-stats")
    public ResponseEntity<com.lumo.backend.teachers.dto.TeacherClassStatsResponse> getMyClassStats(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return ResponseEntity.ok(teacherService.getTeacherClassStats(authorizationHeader));
    }

    @GetMapping("/my-assigned-classes")
    public ResponseEntity<com.lumo.backend.teachers.dto.TeacherClassesResponse> getMyAssignedClasses(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return ResponseEntity.ok(teacherService.getMyAssignedClasses(authorizationHeader));
    }
}
