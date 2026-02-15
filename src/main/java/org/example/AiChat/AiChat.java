package org.example.AiChat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.MessageSender;
import org.example.Settings.SettingsService;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.example.Settings.Settings.*;

public class AiChat {
    private final Logger logger = LoggerFactory.getLogger(AiChat.class);
    private final MessageSender sender;
    private final SettingsService settings = new SettingsService();
    private final Float answerTemperature = Float.valueOf(settings.getSettingValue(AI_ANSWER_TEMPERATURE));
    private final Integer maxTokens = Integer.valueOf(settings.getSettingValue(AI_MAX_TOKENS_ANSWER_QUESTION));
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String deepseekApiKey;
    private final String deepseekBaseUrl = "https://api.deepseek.com/v1/chat/completions";

    public AiChat(String aiToken) {
        this.deepseekApiKey = aiToken;
        TelegramBot bot = TelegramBot.getInstance();
        sender = new MessageSender(bot);
    }

    public void askAi(Message message) {
        String[] parts = message.getText().split(" ", 2);
        String userQuestion = parts.length > 1 ? parts[1] : "";
        Long chatId = message.getChatId();
        if (userQuestion.isEmpty() || userQuestion.isBlank()) {
            sender.sendMessage(chatId, "Напиши свой вопрос после команды /ai");
            return;
        }
        String contex;
        if (Objects.equals(chatId, MY_CHAT_ID))
            contex = AI_CONTEXT_FOR_MY_CHAT;
        else
            contex = AI_CONTEXT;

        String aiAnswer = sendRequestToAi(settings.getSettingValue(contex), userQuestion, answerTemperature);
        if (aiAnswer != null) {
            sender.sendMessage(chatId, aiAnswer);
        }
    }

    private String sendRequestToAi(String context, String userQuestion, Float temperature) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", "deepseek-chat");
            ArrayNode messages = objectMapper.createArrayNode();
            messages.addObject().put("role", "system").put("content", context);
            messages.addObject().put("role", "user").put("content", userQuestion);
            request.set("messages", messages);
            request.put("temperature", temperature);
            request.put("max_tokens", maxTokens);

            return sendHttpRequest(request);
        } catch (Exception e) {
            logger.error("AI не смог ответить '{}': {}", userQuestion, e.getMessage());
            return null;
        }
    }

    private String sendHttpRequest(ObjectNode requestBody) throws IOException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deepseekBaseUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + deepseekApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.error("DeepSeek API error: {}", response.body());
            return null;
        }

        ObjectNode respNode = objectMapper.readValue(response.body(), ObjectNode.class);
        return respNode.get("choices").get(0).get("message").get("content").asText().trim();
    }
}

