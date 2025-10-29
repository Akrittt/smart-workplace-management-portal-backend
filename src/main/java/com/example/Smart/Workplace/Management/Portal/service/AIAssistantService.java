package com.example.Smart.Workplace.Management.Portal.service;

import com.example.Smart.Workplace.Management.Portal.model.ChatMessage;
import com.example.Smart.Workplace.Management.Portal.model.LeaveRequest;
import com.example.Smart.Workplace.Management.Portal.model.User;
import com.example.Smart.Workplace.Management.Portal.repository.ChatMessageRepository;
import com.example.Smart.Workplace.Management.Portal.repository.LeaveRequestRepository;
import com.example.Smart.Workplace.Management.Portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIAssistantService {

    private final GroqAIService groqAIService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    private static final String SYSTEM_PROMPT = """
            You are a helpful workplace assistant for the Smart Workplace Management Portal.
            You help employees with:
            - Leave requests and leave balance queries
            - Filing complaints
            - Understanding company policies
            - General HR questions
            
            Be friendly, professional, and concise. If asked to perform an action like 
            submitting a leave request, guide the user through the process step by step.
            
            Current date: %s
            
            Available commands you can help with:
            - Check leave balance
            - Apply for leave
            - File a complaint
            - View upcoming leaves
            - Ask about policies
            """;

    public String processMessage(String message, String username) {
        log.info("Processing message from user: {}", username);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Get user context
        String userContext = getUserContext(user);

        // Build system prompt with context
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String fullSystemPrompt = String.format(SYSTEM_PROMPT, currentDate) + "\n\n" + userContext;

        // Get AI response
        String aiResponse = groqAIService.getChatCompletion(message, fullSystemPrompt);

        // Save conversation
        ChatMessage chatMessage = ChatMessage.builder()
                .user(user)
                .message(message)
                .response(aiResponse)
                .createdAt(LocalDateTime.now())
                .build();

        chatMessageRepository.save(chatMessage);

        return aiResponse;
    }

    private String getUserContext(User user) {
        StringBuilder context = new StringBuilder();

        context.append("USER INFORMATION:\n");
        context.append("Name: ").append(user.getFullName()).append("\n");
        context.append("Email: ").append(user.getEmail()).append("\n");
        context.append("Role: ").append(user.getRole()).append("\n");
        context.append("Department: ").append(user.getDepartment()).append("\n\n");

        // Get leave balance
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeId(user.getId());
        long pendingLeaves = leaves.stream().filter(l -> l.getStatus().toString().equals("PENDING")).count();
        long approvedLeaves = leaves.stream().filter(l -> l.getStatus().toString().equals("APPROVED")).count();

        context.append("LEAVE INFORMATION:\n");
        context.append("Total leave requests: ").append(leaves.size()).append("\n");
        context.append("Pending requests: ").append(pendingLeaves).append("\n");
        context.append("Approved leaves: ").append(approvedLeaves).append("\n");
        context.append("Remaining leave balance: ").append(15 - approvedLeaves).append(" days\n");

        return context.toString();
    }

    public List<ChatMessage> getChatHistory(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return chatMessageRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());
    }
}
