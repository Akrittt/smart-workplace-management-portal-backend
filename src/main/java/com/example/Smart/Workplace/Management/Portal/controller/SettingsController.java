package com.example.Smart.Workplace.Management.Portal.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173"})
public class SettingsController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        // In a real app, fetch from database
        Map<String, Object> settings = new HashMap<>();
        settings.put("systemName", "Smart Workplace Management Portal");
        settings.put("systemEmail", "admin@company.com");
        settings.put("timezone", "Asia/Kolkata");
        settings.put("dateFormat", "DD/MM/YYYY");
        settings.put("maxLeaveDaysPerRequest", 10);
        settings.put("minLeaveNotice", 2);
        settings.put("carryForwardLeaves", true);
        settings.put("autoApproveLeaves", false);
        settings.put("sessionTimeout", 24);
        settings.put("passwordMinLength", 8);
        settings.put("requireStrongPassword", true);
        settings.put("emailNotifications", true);

        return ResponseEntity.ok(settings);
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> updateSettings(@RequestBody Map<String, Object> settings) {
        // In a real app, save to database
        Map<String, String> response = new HashMap<>();
        response.put("message", "Settings updated successfully");
        return ResponseEntity.ok(response);
    }
}
