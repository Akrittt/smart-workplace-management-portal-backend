package com.example.Smart.Workplace.Management.Portal.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GroqAIService {

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${groq.api.url:https://api.groq.com/v1/chat/completions}")
    private String apiUrl;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    @PostConstruct
    public void validateConfig() {
        log.info("Groq API Configuration:");
        log.info("API URL: {}", apiUrl);
        log.info("Model: {}", model);
        log.info("API Key present: {}", apiKey != null && !apiKey.trim().isEmpty());

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("GROQ_API_KEY")) {
            log.error("⚠️ CRITICAL: Groq API Key is not configured properly!");
        }
    }

    public JsonObject getChatCompletion(String userMessage, String systemPrompt, JsonArray tools) {
        log.info("Received chat request. User message: {}", userMessage);

        // Validation
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("GROQ_API_KEY")) {
            log.error("CRITICAL: Groq API Key is missing or invalid");
            return createErrorResponse("System Error: API Key is missing. Please configure GROQ_API_KEY.");
        }

        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            log.error("CRITICAL: Groq API URL is missing");
            return createErrorResponse("System Error: API URL is not configured.");
        }

        try {
            JsonObject requestBody = buildRequestBody(userMessage, systemPrompt, tools);
            log.debug("Request body: {}", requestBody.toString());

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            log.info("Sending request to Groq API...");

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                log.info("Groq API Response Code: {}", response.code());
                log.debug("Groq API Response Body: {}", responseBody);

                if (!response.isSuccessful()) {
                    log.error("Groq API Error: {} - {}", response.code(), responseBody);
                    return createErrorResponse(
                            "AI Service Error (Code: " + response.code() + "). Please try again."
                    );
                }

                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (jsonResponse.has("choices") &&
                        jsonResponse.getAsJsonArray("choices").size() > 0) {

                    return jsonResponse.getAsJsonArray("choices")
                            .get(0)
                            .getAsJsonObject()
                            .getAsJsonObject("message");
                } else {
                    log.error("Unexpected response format: {}", responseBody);
                    return createErrorResponse("Unexpected response from AI service.");
                }
            }

        } catch (IOException e) {
            log.error("Network error calling Groq API", e);
            return createErrorResponse(
                    "Network error: " + e.getMessage() + ". Please check your connection."
            );
        } catch (Exception e) {
            log.error("Unexpected error in AI service", e);
            return createErrorResponse(
                    "Unexpected error: " + e.getMessage()
            );
        }
    }

    private JsonObject buildRequestBody(String userMessage, String systemPrompt, JsonArray tools) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);

        JsonArray messages = new JsonArray();

        // System message
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        // User message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        // Add tools if provided
        if (tools != null && !tools.isEmpty()) {
            requestBody.add("tools", tools);
            requestBody.addProperty("tool_choice", "auto");
        }

        requestBody.addProperty("temperature", 0.3);
        requestBody.addProperty("max_tokens", 1000);

        return requestBody;
    }

    private JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("content", message);
        return error;
    }
}