package com.example.Smart.Workplace.Management.Portal.service;

import com.example.Smart.Workplace.Management.Portal.dto.ComplaintDto;
import com.example.Smart.Workplace.Management.Portal.dto.LeaveRequestDto;
import com.example.Smart.Workplace.Management.Portal.model.*;
import com.example.Smart.Workplace.Management.Portal.repository.ChatMessageRepository;
import com.example.Smart.Workplace.Management.Portal.repository.ComplaintRepository;
import com.example.Smart.Workplace.Management.Portal.repository.LeaveRequestRepository;
import com.example.Smart.Workplace.Management.Portal.repository.UserRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
    private final ComplaintRepository complaintRepository;
    private final LeaveService leaveService;
    private final ComplaintService complaintService;

    private static final String SYSTEM_PROMPT = """
            You are a smart workplace assistant.
            Current date: %s
            
            You can assist with:
            1. Answering questions about leave balance.
            2. Applying for leave on behalf of the user (use 'apply_leave' tool).
            3. Filing complaints (use 'file_complaint' tool).
            4. check complaint status.
            
            If the user asks to file a complaint, you MUST ask for a title, description, and priority (Low, Medium, High) if not provided.
            You must infer the start and end dates from the user's message (e.g., "next Monday").
            If the user provides only one date (e.g., "Take leave tomorrow"), assume start and end date are the same.
            """;

    public String processMessage(String message, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 1. Prepare Tools
        JsonArray tools = defineTools();

        // 2. Prepare Context
        String context = getUserContext(user);
        String fullSystemPrompt = String.format(SYSTEM_PROMPT, LocalDate.now()) + "\n\n" + context;

        // 3. Call AI
        JsonObject aiResponse = groqAIService.getChatCompletion(message, fullSystemPrompt, tools);

        String finalResponseText;

        // 4. Check for Tool Calls
        if (aiResponse.has("tool_calls")) {
            finalResponseText = handleToolCalls(aiResponse.getAsJsonArray("tool_calls"), user);
        } else {
            // Normal text response
            finalResponseText = aiResponse.has("content") && !aiResponse.get("content").isJsonNull()
                    ? aiResponse.get("content").getAsString()
                    : "I'm not sure how to handle that.";
        }

        // 5. Save History
        saveConversation(user, message, finalResponseText);

        return finalResponseText;
    }

    private String handleToolCalls(JsonArray toolCalls, User user) {
        StringBuilder resultMessage = new StringBuilder();

        for (JsonElement toolCallElement : toolCalls) {
            JsonObject toolCall = toolCallElement.getAsJsonObject();
            JsonObject function = toolCall.getAsJsonObject("function");
            String functionName = function.get("name").getAsString();
            String arguments = function.get("arguments").getAsString();

            if ("apply_leave".equals(functionName)) {
                resultMessage.append(executeLeaveApplication(arguments, user));
            } else if ("file_complaint".equals(functionName)) {
                resultMessage.append(executeFileComplaint(arguments, user));
            }
        }
        return resultMessage.toString();
    }

    private String executeLeaveApplication(String jsonArgs, User user) {
        try {
            Gson gson = new Gson();
            JsonObject args = gson.fromJson(jsonArgs, JsonObject.class);

            String startDateStr = args.get("startDate").getAsString();
            String endDateStr = args.get("endDate").getAsString();
            String reason = args.get("reason").getAsString();

            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .startDate(LocalDate.parse(startDateStr))
                    .endDate(LocalDate.parse(endDateStr))
                    .reason(reason)
                    .build();

            leaveService.submitLeaveRequest(dto, user.getEmail());

            return String.format("✅ Success! I have applied for leave from %s to %s for reason: '%s'. Awaiting manager approval.",
                    startDateStr, endDateStr, reason);

        } catch (Exception e) {
            log.error("Failed to execute tool", e);
            return "❌ I tried to apply for leave, but something went wrong: " + e.getMessage();
        }
    }

    private String executeFileComplaint(String jsonArgs, User user) {
        try {
            Gson gson = new Gson();
            JsonObject args = gson.fromJson(jsonArgs, JsonObject.class);

            String title = args.get("title").getAsString();
            String description = args.get("description").getAsString();
            String priorityStr = args.has("priority") ? args.get("priority").getAsString().toUpperCase() : "MEDIUM";

            // Map string to Enum safely
            ComplaintPriority priority;
            try {
                priority = ComplaintPriority.valueOf(priorityStr);
            } catch (IllegalArgumentException e) {
                priority = ComplaintPriority.MEDIUM;
            }

            ComplaintDto dto = ComplaintDto.builder()
                    .title(title)
                    .description(description)
                    .priority(priority)
                    .build();

            complaintService.submitComplaint(dto, user.getEmail());
            return String.format("✅ Complaint '%s' filed successfully with %s priority.", title, priority);
        } catch (Exception e) {
            log.error("Complaint error", e);
            return "❌ Failed to file complaint: " + e.getMessage();
        }
    }

    // --- CONTEXT GENERATION ---
    private String getUserContext(User user) {
        StringBuilder context = new StringBuilder();
        context.append("User: ").append(user.getFullName()).append("\n");

        // 1. Fetch Leave Data
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeId(user.getId());
        long approvedLeaves = leaves.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED).count();
        context.append("Approved Leaves Used: ").append(approvedLeaves).append("/15\n");

        context.append("Recent Leaves:\n");
        leaves.stream().limit(5).forEach(l ->
                context.append("- ").append(l.getStartDate()).append(" to ").append(l.getEndDate())
                        .append(" (").append(l.getStatus()).append(")\n")
        );

        // 2. Fetch Complaint Data
        List<Complaint> complaints = complaintRepository.findByUserId(user.getId());
        context.append("\nRecent Complaints:\n");
        if (complaints.isEmpty()) {
            context.append("- No recent complaints.\n");
        } else {
            complaints.stream().limit(5).forEach(c ->
                    context.append("- '").append(c.getTitle()).append("' [").append(c.getStatus())
                            .append("] Priority: ").append(c.getPriority()).append("\n")
            );
        }

        return context.toString();
    }

    private JsonArray defineTools() {
        String leaveTool = """
            {
                "type": "function",
                "function": {
                    "name": "apply_leave",
                    "description": "Apply for leave",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "startDate": {"type": "string", "description": "YYYY-MM-DD"},
                            "endDate": {"type": "string", "description": "YYYY-MM-DD"},
                            "reason": {"type": "string"}
                        },
                        "required": ["startDate", "endDate", "reason"]
                    }
                }
            }
            """;

        String complaintTool = """
            {
                "type": "function",
                "function": {
                    "name": "file_complaint",
                    "description": "File a workplace complaint or grievance",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "title": {"type": "string", "description": "Short title of the issue"},
                            "description": {"type": "string", "description": "Detailed description"},
                            "priority": {"type": "string", "enum": ["LOW", "MEDIUM", "HIGH"], "description": "Priority level"}
                        },
                        "required": ["title", "description", "priority"]
                    }
                }
            }
            """;

        JsonArray tools = new JsonArray();
        Gson gson = new Gson();
        tools.add(gson.fromJson(leaveTool, JsonObject.class));
        tools.add(gson.fromJson(complaintTool, JsonObject.class));
        return tools;
    }

    private void saveConversation(User user, String msg, String response) {
        ChatMessage chatMessage = ChatMessage.builder()
                .user(user)
                .message(msg)
                .response(response)
                .createdAt(LocalDateTime.now())
                .intent("AI_AGENT_ACTION")
                .build();
        chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> getChatHistory(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return chatMessageRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());
    }
}