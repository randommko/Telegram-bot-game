package org.example.AiChat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.Settings.Settings.*;

public class AiChat {
    private final Logger logger = LoggerFactory.getLogger(AiChat.class);
    private final TelegramBot bot;
    private final MessageSender sender;
    private final SettingsService settings = new SettingsService();
    private final ContextRepo repo = new ContextRepo();
    private final Float answerTemperature = Float.valueOf(settings.getSettingValue(AI_ANSWER_TEMPERATURE));
    private final Float summaryTemperature = Float.valueOf(settings.getSettingValue(AI_SUMMARY_TEMPERATURE));
    private final Float supportDialogueTemperature = 0.5F;
    private final Integer maxTokens = Integer.valueOf(settings.getSettingValue(AI_MAX_TOKENS_ANSWER_QUESTION));
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String deepseekApiKey;
    private final String deepseekBaseUrl = "https://api.deepseek.com/v1/chat/completions";
    private final static Map<Long, LocalTime> timeOfLastAiQuestionInChat = new HashMap<>();
    private final static Map<Long, ArrayNode> chatHistory = new HashMap<>();

    public AiChat(String aiToken) {
        this.deepseekApiKey = aiToken;
        bot = TelegramBot.getInstance();
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
        if (!timeOfLastAiQuestionInChat.containsKey(chatId))
            timeOfLastAiQuestionInChat.put(chatId, LocalTime.now());

        ArrayNode chatHistoryMessages = objectMapper.createArrayNode();


        if (!chatHistory.containsKey(chatId) & Objects.equals(chatId, MY_CHAT_ID)) {
            chatHistoryMessages.addObject().put("role", "system").put("content", AI_CONTEXT_FOR_MY_CHAT);
            chatHistory.put(chatId, chatHistoryMessages);
        }

        if (!chatHistory.containsKey(chatId) & !Objects.equals(chatId, MY_CHAT_ID)) {
            chatHistoryMessages.addObject().put("role", "system").put("content", AI_CONTEXT);
            chatHistory.put(chatId, chatHistoryMessages);
        }

        String userName = message.getFrom().getUserName();
        chatHistoryMessages.addObject().put("role", "user").put("content", userName + " спрашивает: " + userQuestion;);

        String aiAnswer = sendRequestToAi(chatHistory.get(chatId), answerTemperature);
        if (aiAnswer != null) {
            sender.sendMessage(chatId, aiAnswer);
        }
    }

    private String sendRequestToAi(ArrayNode question, Float temperature) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", "deepseek-chat");
            request.set("messages", question);
            request.put("temperature", temperature);
            request.put("max_tokens", maxTokens);

            return sendHttpRequest(request);
        } catch (Exception e) {
            logger.error("AI не смог ответить '{}':", e.getMessage());
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

