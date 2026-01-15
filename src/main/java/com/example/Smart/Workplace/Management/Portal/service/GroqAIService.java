package com.example.Smart.Workplace.Management.Portal.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GroqAIService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model}")
    private String model;

    @Value("${groq.api.url}")
    private String apiUrl;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30,TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    public JsonObject getChatCompletion(String userMessage, String systemPrompt, JsonArray tools) {

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("GROQ_API_KEY")) {
            log.error("CRITICAL: Groq API Key is missing or invalid in application.properties");
            return createErrorResponse("System Error: API Key is missing. Please check backend logs.");
        }

        try {
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

            // Add tools
            if (tools != null && !tools.isEmpty()) {
                requestBody.add("tools", tools);
                requestBody.addProperty("tool_choice", "auto");
            }

            requestBody.addProperty("temperature", 0.3); // Lower temperature for better tool precision
            requestBody.addProperty("max_tokens", 1000);

            RequestBody body = RequestBody.create(
                    requestBody.toString(), // content
                    MediaType.parse("application/json") // label that text inside is json formate
            );

            Request request = new Request.Builder()  // addressing the data envelope
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) { // send http request
                if (!response.isSuccessful()) {
                    String err = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Groq API Error: {} - {}", response.code(), err);
                    return createErrorResponse("I'm having trouble thinking right now. (Error: " + response.code() + ")");
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                // Return the full message object
                return jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message");
            }

        } catch (IOException e) {
            log.error("Critical AI Service Error", e);
            return createErrorResponse("I can't connect to my brain right now. Please try again.");
        }
    }

    private JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("content", message);
        return error;
    }
}