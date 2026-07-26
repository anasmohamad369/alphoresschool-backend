package com.lumo.backend.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumo.backend.quiz.dto.QuestionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.api.model:openrouter/free}")
    private String modelName;

    @Value("${openrouter.api.max-tokens:1024}")
    private int maxTokens;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<QuestionDTO> generateQuestions(String topic, int count) {
        if (apiKey == null || apiKey.isBlank()) {
            return generateMockQuestions(topic, count);
        }

        try {
            String prompt = String.format(
                    "Generate exactly %d multiple-choice questions on the topic \"%s\". " +
                            "Output must be a valid JSON array where each object contains: " +
                            "\"questionText\", \"optionA\", \"optionB\", \"optionC\", \"optionD\", and \"correctAnswer\" (value must be exactly \"A\", \"B\", \"C\", or \"D\"). " +
                            "Do not include any markdown format like ```json. Just return raw JSON.",
                    count, topic
            );

            Map<String, Object> requestBody = Map.of(
                    "model", modelName,
                    "max_tokens", maxTokens,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("OpenRouter API returned status " + response.statusCode() + ": " + response.body());
                return generateMockQuestions(topic, count);
            }

            Map<String, Object> responseMap = objectMapper.readValue(response.body(), new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                return generateMockQuestions(topic, count);
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            if (content == null) {
                return generateMockQuestions(topic, count);
            }

            content = cleanJsonString(content);
            return objectMapper.readValue(content, new TypeReference<List<QuestionDTO>>() {});

        } catch (Exception e) {
            System.err.println("Error calling OpenRouter API: " + e.getMessage());
            return generateMockQuestions(topic, count);
        }
    }

    private String cleanJsonString(String text) {
        text = text.trim();
        if (text.contains("```json")) {
            text = text.substring(text.indexOf("```json") + 7);
            text = text.substring(0, text.indexOf("```"));
        } else if (text.contains("```")) {
            text = text.substring(text.indexOf("```") + 3);
            text = text.substring(0, text.indexOf("```"));
        }
        return text.trim();
    }

    private List<QuestionDTO> generateMockQuestions(String topic, int count) {
        List<QuestionDTO> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new QuestionDTO(
                    "Sample Question " + i + " about " + topic + "?",
                    "Option A Description",
                    "Option B Description",
                    "Option C Description",
                    "Option D Description",
                    "A"
            ));
        }
        return list;
    }
}
