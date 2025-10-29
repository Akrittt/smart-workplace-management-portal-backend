package com.example.Smart.Workplace.Management.Portal.controller;

import com.example.Smart.Workplace.Management.Portal.model.ChatMessage;
import com.example.Smart.Workplace.Management.Portal.service.AIAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-assistant")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173"})
public class AIAssistantController {

    private final AIAssistantService aiAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String message = request.get("message");
        log.info("Received chat message from: {}", authentication.getName());

        String response = aiAssistantService.processMessage(message, authentication.getName());

        Map<String, String> result = new HashMap<>();
        result.put("response", response);
        result.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(Authentication authentication) {
        List<ChatMessage> history = aiAssistantService.getChatHistory(authentication.getName());
        return ResponseEntity.ok(history);
    }
}
