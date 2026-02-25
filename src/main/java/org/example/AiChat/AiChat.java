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
import java.util.*;


import static org.example.Settings.Settings.*;

public class AiChat {
    private final Logger logger = LoggerFactory.getLogger(AiChat.class);
    private final MessageSender sender;
    private final SettingsService settings = new SettingsService();
    private final Float answerTemperature = Float.valueOf(settings.getSettingValue(AI_ANSWER_TEMPERATURE));
    private final Integer maxTokens = Integer.valueOf(settings.getSettingValue(AI_MAX_TOKENS_ANSWER_QUESTION));
    private static final long MAX_IDLE_TIME_MINUTES = 30; // Максимальное время простоя в минутах
    private static final long MAX_IDLE_TIME_MILLIS = MAX_IDLE_TIME_MINUTES * 60 * 1000; // Конвертируем в миллисекунды
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String deepseekApiKey;
    private final String deepseekBaseUrl = "https://api.deepseek.com/v1/chat/completions";
    private final ConversationHistoryService conversationHistoryService;


    public AiChat(String aiToken, ConversationHistoryService conversationHistoryService) {
        this.deepseekApiKey = aiToken;
        TelegramBot bot = TelegramBot.getInstance();
        sender = new MessageSender(bot);
        this.conversationHistoryService = conversationHistoryService;
    }
    public void askAi(Message message) {
        String[] parts = message.getText().split(" ", 2);
        String userQuestion = parts.length > 1 ? parts[1] : "";
        Long chatId = message.getChatId();

        if (userQuestion.isBlank()) {
            sender.sendMessage(chatId, "Напиши свой вопрос после команды /ai");
            return;
        }

        String aiAnswer = sendRequestToAi(userQuestion, chatId, answerTemperature);
        if (aiAnswer != null) {
            sender.sendMessage(chatId, aiAnswer);
            conversationHistoryService.addMessage(message, "assistant", aiAnswer);
        }
    }
    public ArrayNode getHistoryInChat(Long chatId) {
        ArrayNode messages = objectMapper.createArrayNode();

        // Получаем всех пользователей в чате и их сообщения
        var allMessagesInChat = conversationHistoryService.getAllMessagesInChat(chatId);

        // Собираем все сообщения от всех пользователей
        List<ConversationHistoryService.Message> allMessagesInchatList = allMessagesInChat.values()
                .stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(ConversationHistoryService.Message::timestamp))
                .toList();

        // Преобразуем в JSON формат
        for (ConversationHistoryService.Message msg : allMessagesInchatList) {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", msg.role());
            messageNode.put("content", msg.content());
            messages.add(messageNode);
        }

        logger.debug("Найдена история в чате {} для запроса в ИИ, всего {} сообщений", chatId, messages.size());
        return messages;
    }
    private String sendRequestToAi(String userQuestion, Long chatId, Float temperature) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", "deepseek-chat");

            ArrayNode messages = getHistoryInChat(chatId);
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

