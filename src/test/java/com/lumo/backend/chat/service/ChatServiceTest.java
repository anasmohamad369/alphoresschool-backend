package com.lumo.backend.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lumo.backend.chat.dto.ChatMessageRequest;
import com.lumo.backend.chat.dto.ConversationResponse;
import com.lumo.backend.chat.entity.ChatMessage;
import com.lumo.backend.chat.repository.ChatMessageRepository;
import com.lumo.backend.students.entity.Student;
import com.lumo.backend.students.repository.StudentRepository;
import com.lumo.backend.teachers.entity.Teacher;
import com.lumo.backend.teachers.repository.TeacherRepository;
import com.lumo.backend.admin.repository.PrincipalRepository;
import com.lumo.backend.security.JwtService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private PrincipalRepository principalRepository;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSendMessageTeacherToStudentSuccess() {
        String tokenHeader = "Bearer teacher-token";
        ChatMessageRequest request = new ChatMessageRequest("ALP260001", "Hello Student/Parent");

        when(jwtService.extractTeacherSubject("teacher-token")).thenReturn("teacher@school.com");
        when(studentRepository.findByStudentId("ALP260001")).thenReturn(Optional.of(new Student()));

        when(chatRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage saved = chatService.sendMessage(tokenHeader, request);

        assertNotNull(saved);
        assertEquals("teacher@school.com", saved.getSenderId());
        assertEquals("TEACHER", saved.getSenderRole());
        assertEquals("ALP260001", saved.getReceiverId());
        assertEquals("STUDENT", saved.getReceiverRole());
        assertEquals("Hello Student/Parent", saved.getContent());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void testSendMessageStudentToTeacherSuccess() {
        String tokenHeader = "Bearer student-token";
        ChatMessageRequest request = new ChatMessageRequest("teacher@school.com", "Hello Teacher");

        when(jwtService.extractStudentSubject("student-token")).thenReturn("ALP260001");
        when(teacherRepository.findByEmailId("teacher@school.com")).thenReturn(Optional.of(new Teacher()));

        when(chatRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage saved = chatService.sendMessage(tokenHeader, request);

        assertNotNull(saved);
        assertEquals("ALP260001", saved.getSenderId());
        assertEquals("STUDENT", saved.getSenderRole());
        assertEquals("teacher@school.com", saved.getReceiverId());
        assertEquals("TEACHER", saved.getReceiverRole());
        assertEquals("Hello Teacher", saved.getContent());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void testSendMessageValidationFailures() {
        // Missing receiverId
        assertThrows(ResponseStatusException.class, () -> 
            chatService.sendMessage("Bearer token", new ChatMessageRequest(null, "content"))
        );

        // Blank content
        assertThrows(ResponseStatusException.class, () -> 
            chatService.sendMessage("Bearer token", new ChatMessageRequest("ALP260001", "   "))
        );

        // Receiver not found (Teacher to non-existent Student)
        when(jwtService.extractTeacherSubject("token")).thenReturn("teacher@school.com");
        when(studentRepository.findByStudentId("ALP260001")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> 
            chatService.sendMessage("Bearer token", new ChatMessageRequest("ALP260001", "content"))
        );
    }

    @Test
    void testGetChatHistory() {
        String tokenHeader = "Bearer token";
        when(jwtService.extractTeacherSubject("token")).thenReturn("teacher@school.com");
        
        List<ChatMessage> mockHistory = List.of(new ChatMessage());
        when(chatRepository.findChatHistory("teacher@school.com", "ALP260001")).thenReturn(mockHistory);

        List<ChatMessage> history = chatService.getChatHistory(tokenHeader, "ALP260001");
        assertEquals(mockHistory, history);
    }

    @Test
    void testGetConversations() {
        String tokenHeader = "Bearer token";
        when(jwtService.extractTeacherSubject("token")).thenReturn("teacher@school.com");

        ChatMessage latestMsg = new ChatMessage();
        latestMsg.setSenderId("teacher@school.com");
        latestMsg.setReceiverId("ALP260001");
        latestMsg.setContent("Latest content");
        latestMsg.setTimestamp(Instant.parse("2026-06-22T10:00:00.000Z"));

        when(chatRepository.findLatestMessagesForUser("teacher@school.com")).thenReturn(List.of(latestMsg));

        Student mockStudent = new Student();
        mockStudent.setFirstName("John");
        mockStudent.setLastName("Doe");
        when(studentRepository.findByStudentId("ALP260001")).thenReturn(Optional.of(mockStudent));

        List<ConversationResponse> conversations = chatService.getConversations(tokenHeader);

        assertEquals(1, conversations.size());
        ConversationResponse resp = conversations.get(0);
        assertEquals("ALP260001", resp.participantId());
        assertEquals("John Doe (Parent)", resp.participantName());
        assertEquals("Latest content", resp.lastMessage());
    }
}
