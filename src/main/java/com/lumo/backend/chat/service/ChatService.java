package com.lumo.backend.chat.service;

import com.lumo.backend.chat.entity.ChatMessage;
import com.lumo.backend.chat.dto.ChatMessageRequest;
import com.lumo.backend.chat.dto.ConversationResponse;
import com.lumo.backend.chat.dto.ChatContactResponse;
import com.lumo.backend.chat.dto.ChatBroadcastRequest;
import com.lumo.backend.chat.dto.AdminConversationResponse;
import com.lumo.backend.chat.repository.ChatMessageRepository;
import com.lumo.backend.students.entity.Student;
import com.lumo.backend.students.repository.StudentRepository;
import com.lumo.backend.teachers.entity.Teacher;
import com.lumo.backend.teachers.repository.TeacherRepository;
import com.lumo.backend.admin.entity.Principal;
import com.lumo.backend.admin.repository.PrincipalRepository;
import com.lumo.backend.security.JwtService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private final ChatMessageRepository chatRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PrincipalRepository principalRepository;
    private final JwtService jwtService;

    public ChatService(
            ChatMessageRepository chatRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            PrincipalRepository principalRepository,
            JwtService jwtService) {
        this.chatRepository = chatRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.principalRepository = principalRepository;
        this.jwtService = jwtService;
    }

    public ChatMessage sendMessage(String tokenHeader, ChatMessageRequest request) {
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        if (request.receiverId() == null || request.receiverId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receiver ID cannot be empty");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
        }

        String token = tokenHeader.substring(7).trim();
        String senderId;
        String senderRole;

        if (jwtService.extractTeacherSubject(token) != null) {
            senderId = jwtService.extractTeacherSubject(token);
            senderRole = "TEACHER";
        } else if (jwtService.extractStudentSubject(token) != null) {
            senderId = jwtService.extractStudentSubject(token);
            senderRole = "STUDENT";
        } else if (jwtService.extractAdminSubject(token) != null) {
            senderId = jwtService.extractAdminSubject(token);
            senderRole = "ADMIN";
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or unauthorized token");
        }

        String receiverRole = null;
        if (principalRepository.findByEmailId(request.receiverId()).isPresent()) {
            receiverRole = "ADMIN";
        } else if (teacherRepository.findByEmailId(request.receiverId()).isPresent()) {
            receiverRole = "TEACHER";
        } else if (studentRepository.findByStudentId(request.receiverId()).isPresent()) {
            receiverRole = "STUDENT";
        }

        if (receiverRole == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient not found: " + request.receiverId());
        }

        if (senderRole.equals("STUDENT") && receiverRole.equals("STUDENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students cannot chat directly with other students.");
        }

        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setReceiverId(request.receiverId());
        message.setReceiverRole(receiverRole);
        message.setContent(request.content());
        message.setTimestamp(Instant.now().truncatedTo(ChronoUnit.MILLIS));

        return chatRepository.save(message);
    }

    public List<ChatMessage> getChatHistory(String tokenHeader, String recipientId) {
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        if (recipientId == null || recipientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient ID cannot be empty");
        }

        String token = tokenHeader.substring(7).trim();
        String userId = jwtService.extractTeacherSubject(token);
        if (userId == null) {
            userId = jwtService.extractStudentSubject(token);
        }
        if (userId == null) {
            userId = jwtService.extractAdminSubject(token);
        }
        
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or unauthorized token");
        }
        return chatRepository.findChatHistory(userId, recipientId);
    }

    public List<ConversationResponse> getConversations(String tokenHeader) {
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        String token = tokenHeader.substring(7).trim();
        String userId = jwtService.extractTeacherSubject(token);
        if (userId == null) {
            userId = jwtService.extractStudentSubject(token);
        }
        if (userId == null) {
            userId = jwtService.extractAdminSubject(token);
        }
        
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or unauthorized token");
        }

        List<ChatMessage> latestMessages = chatRepository.findLatestMessagesForUser(userId);
        List<ConversationResponse> responses = new ArrayList<>();

        for (ChatMessage msg : latestMessages) {
            String partnerId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            String partnerRole = msg.getSenderId().equals(userId) ? msg.getReceiverRole() : msg.getSenderRole();
            String partnerName = "User";
            String partnerSubtext = "";

            if (partnerId.contains("@")) {
                var teacher = teacherRepository.findByEmailId(partnerId);
                if (teacher.isPresent()) {
                    var t = teacher.get();
                    partnerName = t.getName();
                    partnerRole = "TEACHER";
                    String subjects = (t.getSubjects() != null && !t.getSubjects().isEmpty())
                        ? String.join(", ", t.getSubjects())
                        : (t.getClassTeacher() != null && !t.getClassTeacher().isBlank() ? "Class Teacher " + t.getClassTeacher() : "Teacher");
                    partnerSubtext = subjects;
                } else {
                    var principal = principalRepository.findByEmailId(partnerId);
                    if (principal.isPresent()) {
                        var p = principal.get();
                        partnerName = p.getName();
                        partnerRole = "ADMIN";
                        partnerSubtext = p.getSchoolName() != null ? p.getSchoolName() : "Director / Principal";
                    }
                }
            } else { // Student
                var student = studentRepository.findByStudentId(partnerId);
                if (student.isPresent()) {
                    var s = student.get();
                    partnerName = (s.getFirstName() != null ? s.getFirstName() : "") + 
                                  (s.getLastName() != null ? " " + s.getLastName() : "");
                    if (partnerName.isBlank()) partnerName = "Student #" + s.getStudentId();
                    partnerRole = "STUDENT";
                    String className = s.getSchoolClass() != null ? s.getSchoolClass().getName() : (s.getStudentClass() != null ? s.getStudentClass() : "");
                    String section = s.getSection() != null ? " - " + s.getSection().getName() : "";
                    partnerSubtext = className.isBlank() ? "Student" : ("Class " + className + section);
                }
            }

            responses.add(new ConversationResponse(
                partnerId,
                partnerName,
                partnerRole,
                partnerSubtext,
                msg.getContent(),
                msg.getTimestamp(),
                msg.getSenderRole()
            ));
        }

        return responses;
    }

    /**
     * Principal / Admin supervisory access: retrieves all conversation threads between teachers and parents across the school.
     */
    public List<AdminConversationResponse> getAdminAllConversations(String tokenHeader) {
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        String token = tokenHeader.substring(7).trim();
        String adminEmail = jwtService.extractAdminSubject(token);
        if (adminEmail == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can access this endpoint");
        }

        List<ChatMessage> latestMessages = chatRepository.findAllLatestConversations();
        List<AdminConversationResponse> result = new ArrayList<>();

        for (ChatMessage msg : latestMessages) {
            String userA = msg.getSenderId();
            String userB = msg.getReceiverId();

            String teacherId = null;
            String studentId = null;

            if (userA.contains("@")) {
                teacherId = userA;
                studentId = userB;
            } else if (userB.contains("@")) {
                teacherId = userB;
                studentId = userA;
            } else {
                studentId = userA;
            }

            String teacherName = "Teacher";
            String teacherSubject = "";
            String teacherPhone = "";

            if (teacherId != null) {
                var teacherOpt = teacherRepository.findByEmailId(teacherId);
                if (teacherOpt.isPresent()) {
                    Teacher t = teacherOpt.get();
                    teacherName = t.getName();
                    teacherPhone = t.getMobileNumber() != null ? t.getMobileNumber() : "";
                    teacherSubject = (t.getSubjects() != null && !t.getSubjects().isEmpty())
                        ? String.join(", ", t.getSubjects())
                        : (t.getClassTeacher() != null ? "Class Teacher: " + t.getClassTeacher() : "Faculty");
                } else {
                    var princOpt = principalRepository.findByEmailId(teacherId);
                    if (princOpt.isPresent()) {
                        teacherName = princOpt.get().getName() + " (Director)";
                        teacherSubject = "Administration";
                        teacherPhone = princOpt.get().getMobileNumber() != null ? princOpt.get().getMobileNumber() : "";
                    }
                }
            }

            String studentName = "Student";
            String parentName = "";
            String className = "";
            String sectionName = "";
            String parentPhone = "";

            if (studentId != null) {
                var studentOpt = studentRepository.findByStudentId(studentId);
                if (studentOpt.isPresent()) {
                    Student s = studentOpt.get();
                    studentName = (s.getFirstName() != null ? s.getFirstName() : "") +
                                  (s.getLastName() != null ? " " + s.getLastName() : "");
                    if (studentName.isBlank()) studentName = "Student #" + s.getStudentId();
                    parentName = s.getFatherName() != null ? s.getFatherName() : (s.getMotherName() != null ? s.getMotherName() : "");
                    className = s.getSchoolClass() != null ? s.getSchoolClass().getName() : (s.getStudentClass() != null ? s.getStudentClass() : "");
                    sectionName = s.getSection() != null ? s.getSection().getName() : "";
                    parentPhone = s.getMobileNumber() != null ? s.getMobileNumber() : "";
                }
            }

            String threadId = (teacherId != null ? teacherId : "unknown") + "_" + (studentId != null ? studentId : "unknown");

            result.add(new AdminConversationResponse(
                threadId,
                teacherId != null ? teacherId : "",
                teacherName,
                teacherSubject,
                teacherPhone,
                studentId != null ? studentId : "",
                studentName,
                parentName,
                className,
                sectionName,
                parentPhone,
                msg.getContent(),
                msg.getTimestamp(),
                msg.getSenderRole()
            ));
        }

        return result;
    }

    /**
     * Principal / Admin supervisory access: views full chat history between any teacher and student/parent.
     */
    public List<ChatMessage> getAdminChatHistory(String tokenHeader, String user1, String user2) {
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        String token = tokenHeader.substring(7).trim();
        String adminEmail = jwtService.extractAdminSubject(token);
        if (adminEmail == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can access this endpoint");
        }
        if (user1 == null || user1.isBlank() || user2 == null || user2.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both user1 and user2 must be provided");
        }

        return chatRepository.findChatHistory(user1, user2);
    }

    public List<ChatContactResponse> getContacts(String tokenHeader) {
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        String token = tokenHeader.substring(7).trim();
        String userId = jwtService.extractTeacherSubject(token);
        String role = "TEACHER";
        if (userId == null) {
            userId = jwtService.extractStudentSubject(token);
            role = "STUDENT";
        }
        if (userId == null) {
            userId = jwtService.extractAdminSubject(token);
            role = "ADMIN";
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or unauthorized token");
        }

        List<ChatContactResponse> contacts = new ArrayList<>();

        if ("ADMIN".equals(role)) {
            // Admin can contact all Teachers
            List<Teacher> teachers = teacherRepository.findAll();
            for (Teacher t : teachers) {
                String sub = (t.getSubjects() != null && !t.getSubjects().isEmpty())
                    ? String.join(", ", t.getSubjects())
                    : (t.getClassTeacher() != null ? "Class: " + t.getClassTeacher() : "Teacher");
                contacts.add(new ChatContactResponse(
                    t.getEmailId(),
                    t.getName(),
                    "TEACHER",
                    sub,
                    t.getMobileNumber(),
                    t.getEmailId()
                ));
            }
            // Admin can also contact Students
            List<Student> students = studentRepository.findAll();
            for (Student s : students) {
                String className = s.getSchoolClass() != null ? s.getSchoolClass().getName() : (s.getStudentClass() != null ? s.getStudentClass() : "");
                String section = s.getSection() != null ? " - Sec " + s.getSection().getName() : "";
                String sub = className.isBlank() ? "Student" : ("Class " + className + section);
                String fullName = (s.getFirstName() != null ? s.getFirstName() : "") +
                                  (s.getLastName() != null ? " " + s.getLastName() : "");
                contacts.add(new ChatContactResponse(
                    s.getStudentId(),
                    fullName.isBlank() ? "Student #" + s.getStudentId() : fullName,
                    "STUDENT",
                    sub,
                    s.getMobileNumber(),
                    s.getStudentId()
                ));
            }
        } else if ("TEACHER".equals(role)) {
            // Teacher contacts Principals/Admins
            List<Principal> principals = principalRepository.findAll();
            for (Principal p : principals) {
                contacts.add(new ChatContactResponse(
                    p.getEmailId(),
                    p.getName() + " (Principal / Director)",
                    "ADMIN",
                    p.getSchoolName() != null ? p.getSchoolName() : "Administration",
                    p.getMobileNumber(),
                    p.getEmailId()
                ));
            }
            // Teacher contacts Students
            List<Student> students = studentRepository.findAll();
            for (Student s : students) {
                String className = s.getSchoolClass() != null ? s.getSchoolClass().getName() : (s.getStudentClass() != null ? s.getStudentClass() : "");
                String section = s.getSection() != null ? " - Sec " + s.getSection().getName() : "";
                String sub = className.isBlank() ? "Student" : ("Class " + className + section);
                String fullName = (s.getFirstName() != null ? s.getFirstName() : "") +
                                  (s.getLastName() != null ? " " + s.getLastName() : "");
                contacts.add(new ChatContactResponse(
                    s.getStudentId(),
                    fullName.isBlank() ? "Student #" + s.getStudentId() : fullName,
                    "STUDENT",
                    sub,
                    s.getMobileNumber(),
                    s.getStudentId()
                ));
            }
        } else {
            // Student contacts Principals and Teachers
            List<Principal> principals = principalRepository.findAll();
            for (Principal p : principals) {
                contacts.add(new ChatContactResponse(
                    p.getEmailId(),
                    p.getName() + " (Principal / Director)",
                    "ADMIN",
                    p.getSchoolName() != null ? p.getSchoolName() : "Administration",
                    p.getMobileNumber(),
                    p.getEmailId()
                ));
            }
            List<Teacher> teachers = teacherRepository.findAll();
            for (Teacher t : teachers) {
                contacts.add(new ChatContactResponse(
                    t.getEmailId(),
                    t.getName(),
                    "TEACHER",
                    t.getSubjects() != null ? String.join(", ", t.getSubjects()) : "Teacher",
                    t.getMobileNumber(),
                    t.getEmailId()
                ));
            }
        }

        return contacts;
    }

    public List<ChatContactResponse> getPrincipals() {
        List<Principal> principals = principalRepository.findAll();
        List<ChatContactResponse> list = new ArrayList<>();
        for (Principal p : principals) {
            list.add(new ChatContactResponse(
                p.getEmailId(),
                p.getName(),
                "ADMIN",
                p.getSchoolName() != null ? p.getSchoolName() : "School Director",
                p.getMobileNumber(),
                p.getEmailId()
            ));
        }
        return list;
    }

    public List<ChatMessage> broadcastMessage(String tokenHeader, ChatBroadcastRequest request) {
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
        }

        String token = tokenHeader.substring(7).trim();
        String senderId = jwtService.extractAdminSubject(token);
        if (senderId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can broadcast messages");
        }

        List<String> targetRecipients = new ArrayList<>();
        String targetRole = request.targetRole() != null ? request.targetRole().toUpperCase() : "TEACHER";

        if (request.recipientIds() != null && !request.recipientIds().isEmpty()) {
            targetRecipients.addAll(request.recipientIds());
        } else if ("TEACHER".equals(targetRole)) {
            List<Teacher> teachers = teacherRepository.findAll();
            for (Teacher t : teachers) {
                targetRecipients.add(t.getEmailId());
            }
        } else if ("STUDENT".equals(targetRole)) {
            List<Student> students = studentRepository.findAll();
            for (Student s : students) {
                targetRecipients.add(s.getStudentId());
            }
        }

        List<ChatMessage> saved = new ArrayList<>();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        for (String recId : targetRecipients) {
            ChatMessage msg = new ChatMessage();
            msg.setSenderId(senderId);
            msg.setSenderRole("ADMIN");
            msg.setReceiverId(recId);
            msg.setReceiverRole(targetRole);
            msg.setContent(request.content());
            msg.setTimestamp(now);
            saved.add(msg);
        }

        if (!saved.isEmpty()) {
            return chatRepository.saveAll(saved);
        }
        return saved;
    }
}
