package com.example.Smart.Workplace.Management.Portal.service;

import com.example.Smart.Workplace.Management.Portal.dto.ComplaintDto;
import com.example.Smart.Workplace.Management.Portal.dto.LeaveRequestDto;
import com.example.Smart.Workplace.Management.Portal.model.*;
import com.example.Smart.Workplace.Management.Portal.repository.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
            You are a smart workplace assistant for %s.
            Current date: %s
            
            You can help with:
            1. Leave requests - apply for leave
            2. Complaint management - file new complaints or check status
            3. Information queries - check leave balance, complaint status
            
            IMPORTANT: Use tools when the user wants to perform an action. For simple status checks, 
            just respond conversationally using the context provided.
            
            Be helpful, concise, and professional.
            """;

    public String processMessage(String message, String username) {
        log.info("Processing message for user: {}", username);

        try {
            if (message == null || message.trim().isEmpty()) {
                return "Please provide a message.";
            }

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // 1. Prepare Tools
            JsonArray tools = defineTools();

            // 2. Prepare Context
            String context = getUserContext(user);
            String fullSystemPrompt = String.format(SYSTEM_PROMPT, user.getFullName(), LocalDate.now())
                    + "\n\nCurrent User Context:\n" + context;

            log.debug("Calling Groq AI with context length: {} chars", context.length());

            // 3. Call AI
            JsonObject aiResponse = groqAIService.getChatCompletion(message, fullSystemPrompt, tools);

            if (aiResponse == null) {
                log.error("AI service returned null response");
                return "I'm experiencing technical difficulties. Please try again.";
            }

            String finalResponseText;

            // 4. Check for Tool Calls
            if (aiResponse.has("tool_calls") && !aiResponse.get("tool_calls").isJsonNull()) {
                log.info("Tool calls detected");
                finalResponseText = handleToolCalls(aiResponse.getAsJsonArray("tool_calls"), user);
            } else if (aiResponse.has("content") && !aiResponse.get("content").isJsonNull()) {
                finalResponseText = aiResponse.get("content").getAsString();
                log.info("Text response received");
            } else {
                log.warn("Unexpected AI response format: {}", aiResponse);
                finalResponseText = "I'm not sure how to help with that. Can you rephrase?";
            }

            // 5. Save History
            saveConversation(user, message, finalResponseText);

            return finalResponseText;

        } catch (UsernameNotFoundException e) {
            log.error("User not found: {}", username);
            return "Authentication error. Please log in again.";
        } catch (Exception e) {
            log.error("Error processing message", e);
            return "I encountered an error: " + e.getMessage() + ". Please try again.";
        }
    }

    private String handleToolCalls(JsonArray toolCalls, User user) {
        StringBuilder resultMessage = new StringBuilder();

        try {
            for (JsonElement toolCallElement : toolCalls) {
                JsonObject toolCall = toolCallElement.getAsJsonObject();

                if (!toolCall.has("function")) {
                    log.warn("Tool call missing function: {}", toolCall);
                    continue;
                }

                JsonObject function = toolCall.getAsJsonObject("function");
                String functionName = function.get("name").getAsString();
                String arguments = function.get("arguments").getAsString();

                log.info("Executing tool: {} with args: {}", functionName, arguments);

                switch (functionName) {
                    case "apply_leave":
                        resultMessage.append(executeLeaveApplication(arguments, user));
                        break;
                    case "file_complaint":
                        resultMessage.append(executeFileComplaint(arguments, user));
                        break;
                    case "get_complaint_status":
                        resultMessage.append(getComplaintStatus(user));
                        break;
                    case "get_leave_status":
                        resultMessage.append(getLeaveStatus(user));
                        break;
                    default:
                        log.warn("Unknown tool: {}", functionName);
                        resultMessage.append("Unknown action requested.");
                }

                resultMessage.append("\n");
            }
        } catch (Exception e) {
            log.error("Error handling tool calls", e);
            return "I tried to help but encountered an error: " + e.getMessage();
        }

        return resultMessage.toString().trim();
    }

    private String executeLeaveApplication(String jsonArgs, User user) {
        try {
            Gson gson = new Gson();
            JsonObject args = gson.fromJson(jsonArgs, JsonObject.class);

            if (!args.has("startDate") || !args.has("endDate") || !args.has("reason")) {
                return "❌ Missing required information for leave request.";
            }

            String startDateStr = args.get("startDate").getAsString();
            String endDateStr = args.get("endDate").getAsString();
            String reason = args.get("reason").getAsString();

            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .startDate(LocalDate.parse(startDateStr))
                    .endDate(LocalDate.parse(endDateStr))
                    .reason(reason)
                    .build();

            leaveService.submitLeaveRequest(dto, user.getEmail());

            return String.format("✅ Leave request submitted successfully!\n" +
                            "📅 Dates: %s to %s\n" +
                            "📝 Reason: %s\n" +
                            "⏳ Status: Pending manager approval",
                    startDateStr, endDateStr, reason);

        } catch (Exception e) {
            log.error("Failed to apply leave", e);
            return "❌ Failed to apply for leave: " + e.getMessage();
        }
    }

    private String executeFileComplaint(String jsonArgs, User user) {
        try {
            Gson gson = new Gson();
            JsonObject args = gson.fromJson(jsonArgs, JsonObject.class);

            if (!args.has("title") || !args.has("description") || !args.has("priority")) {
                return "❌ Missing required information for complaint.";
            }

            String title = args.get("title").getAsString();
            String description = args.get("description").getAsString();
            String priorityStr = args.get("priority").getAsString().toUpperCase();

            ComplaintPriority priority;
            try {
                priority = ComplaintPriority.valueOf(priorityStr);
            } catch (IllegalArgumentException e) {
                priority = ComplaintPriority.MEDIUM;
                log.warn("Invalid priority '{}', defaulting to MEDIUM", priorityStr);
            }

            ComplaintDto dto = ComplaintDto.builder()
                    .title(title)
                    .description(description)
                    .priority(priority)
                    .build();

            complaintService.submitComplaint(dto, user.getEmail());

            return String.format("✅ Complaint filed successfully!\n" +
                            "📋 Title: %s\n" +
                            "🎯 Priority: %s\n" +
                            "📊 Status: Open\n" +
                            "Your complaint has been submitted and will be reviewed soon.",
                    title, priority);

        } catch (Exception e) {
            log.error("Failed to file complaint", e);
            return "❌ Failed to file complaint: " + e.getMessage();
        }
    }

    private String getComplaintStatus(User user) {
        try {
            List<Complaint> complaints = complaintRepository.findByUserId(user.getId());

            if (complaints.isEmpty()) {
                return "📋 You have no complaints filed.";
            }

            StringBuilder status = new StringBuilder("📋 **Your Complaints:**\n\n");

            for (Complaint c : complaints) {
                status.append(String.format("**%s**\n", c.getTitle()));
                status.append(String.format("   Status: %s\n", c.getStatus()));
                status.append(String.format("   Priority: %s\n", c.getPriority()));
                status.append(String.format("   Filed: %s\n",
                        c.getSubmittedAt().toLocalDate()));

                if (c.getAssignedTo() != null) {
                    status.append(String.format("   Assigned to: %s\n",
                            c.getAssignedTo().getFullName()));
                }

                if (c.getResolution() != null && !c.getResolution().isEmpty()) {
                    status.append(String.format("   Resolution: %s\n", c.getResolution()));
                }
                status.append("\n");
            }

            return status.toString();

        } catch (Exception e) {
            log.error("Error getting complaint status", e);
            return "❌ Unable to fetch complaint status: " + e.getMessage();
        }
    }

    private String getLeaveStatus(User user) {
        try {
            List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeId(user.getId());

            if (leaves.isEmpty()) {
                return "📅 You have no leave requests.";
            }

            long approved = leaves.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED).count();
            long pending = leaves.stream().filter(l -> l.getStatus() == LeaveStatus.PENDING).count();
            long rejected = leaves.stream().filter(l -> l.getStatus() == LeaveStatus.REJECTED).count();

            StringBuilder status = new StringBuilder();
            status.append(String.format("📊 **Leave Summary:**\n"));
            status.append(String.format("   Balance: %d/15 days remaining\n", 15 - approved));
            status.append(String.format("   Approved: %d | Pending: %d | Rejected: %d\n\n",
                    approved, pending, rejected));

            status.append("📅 **Recent Requests:**\n");
            leaves.stream().limit(5).forEach(l ->
                    status.append(String.format("   %s to %s - %s\n",
                            l.getStartDate(), l.getEndDate(), l.getStatus()))
            );

            return status.toString();

        } catch (Exception e) {
            log.error("Error getting leave status", e);
            return "❌ Unable to fetch leave status: " + e.getMessage();
        }
    }

    @Transactional(readOnly = true)
    private String getUserContext(User user) {
        StringBuilder context = new StringBuilder();
        context.append("User: ").append(user.getFullName()).append("\n");
        context.append("Department: ").append(user.getDepartment() != null ? user.getDepartment() : "Not specified").append("\n\n");

        try {
            // Leave Data
            List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeId(user.getId());
            long approvedLeaves = leaves.stream()
                    .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                    .count();

            context.append("Leave Balance: ").append(15 - approvedLeaves).append("/15 remaining\n");

            if (!leaves.isEmpty()) {
                context.append("Recent Leave Requests:\n");
                leaves.stream().limit(3).forEach(l ->
                        context.append(String.format("  - %s to %s (%s)\n",
                                l.getStartDate(), l.getEndDate(), l.getStatus()))
                );
            }

            // Complaint Data - WITHOUT accessing LOB fields
            List<Complaint> complaints = complaintRepository.findByUserId(user.getId());
            context.append("\nComplaint Summary:\n");

            if (complaints.isEmpty()) {
                context.append("  - No complaints filed\n");
            } else {
                long openComplaints = complaints.stream()
                        .filter(c -> c.getStatus() == ComplaintStatus.OPEN)
                        .count();
                long inProgressComplaints = complaints.stream()
                        .filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS)
                        .count();
                long resolvedComplaints = complaints.stream()
                        .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
                        .count();

                context.append(String.format("  - Total: %d (Open: %d, In Progress: %d, Resolved: %d)\n",
                        complaints.size(), openComplaints, inProgressComplaints, resolvedComplaints));

                // List recent complaints with ONLY safe fields (no LOB access)
                context.append("  Recent complaints:\n");
                complaints.stream().limit(3).forEach(c ->
                        context.append(String.format("    • %s [%s] - %s\n",
                                c.getTitle(), c.getStatus(), c.getPriority()))
                );
            }
        } catch (Exception e) {
            log.error("Error building user context", e);
            context.append("\n(Some data temporarily unavailable)\n");
        }

        return context.toString();
    }

    private JsonArray defineTools() {
        String leaveTool = """
            {
                "type": "function",
                "function": {
                    "name": "apply_leave",
                    "description": "Submit a new leave request for the user",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "startDate": {"type": "string", "description": "Start date in YYYY-MM-DD format"},
                            "endDate": {"type": "string", "description": "End date in YYYY-MM-DD format"},
                            "reason": {"type": "string", "description": "Reason for taking leave"}
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
                    "description": "File a new workplace complaint or issue",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "title": {"type": "string", "description": "Brief title of the complaint"},
                            "description": {"type": "string", "description": "Detailed description of the issue"},
                            "priority": {"type": "string", "enum": ["LOW", "MEDIUM", "HIGH"], "description": "Urgency level"}
                        },
                        "required": ["title", "description", "priority"]
                    }
                }
            }
            """;

        String complaintStatusTool = """
            {
                "type": "function",
                "function": {
                    "name": "get_complaint_status",
                    "description": "Get detailed status of all user complaints",
                    "parameters": {
                        "type": "object",
                        "properties": {}
                    }
                }
            }
            """;

        String leaveStatusTool = """
            {
                "type": "function",
                "function": {
                    "name": "get_leave_status",
                    "description": "Get detailed leave balance and request history",
                    "parameters": {
                        "type": "object",
                        "properties": {}
                    }
                }
            }
            """;

        JsonArray tools = new JsonArray();
        Gson gson = new Gson();
        tools.add(gson.fromJson(leaveTool, JsonObject.class));
        tools.add(gson.fromJson(complaintTool, JsonObject.class));
        tools.add(gson.fromJson(complaintStatusTool, JsonObject.class));
        tools.add(gson.fromJson(leaveStatusTool, JsonObject.class));
        return tools;
    }

    private void saveConversation(User user, String msg, String response) {
        try {
            ChatMessage chatMessage = ChatMessage.builder()
                    .user(user)
                    .message(msg)
                    .response(response)
                    .createdAt(LocalDateTime.now())
                    .intent("AI_CHAT")
                    .build();
            chatMessageRepository.save(chatMessage);
        } catch (Exception e) {
            log.error("Failed to save chat history", e);
        }
    }

    public List<ChatMessage> getChatHistory(String username) {
        try {
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return chatMessageRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());
        } catch (Exception e) {
            log.error("Error fetching chat history", e);
            return List.of();
        }
    }
}