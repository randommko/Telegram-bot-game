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
        String chatTitle = message.getChat().getTitle();
        if (userQuestion.isBlank()) {
            sender.sendMessage(chatId, "Напиши свой вопрос после команды /ai");
            return;
        }

        String aiAnswer = sendRequestToAi(userQuestion, chatId, answerTemperature);
        if (aiAnswer != null) {
            sender.sendMessage(chatId, aiAnswer);
            conversationHistoryService.addMessage(chatId, 0L, "assistant", aiAnswer);
            logger.info("Сохранена история переписки: {}: {}: {}", chatTitle, "AI", aiAnswer);
        }
    }

    public ArrayNode getHistoryInChat(Long chatId) {
        ArrayNode messages = objectMapper.createArrayNode();

        // Получаем всех пользователей в чате и их сообщения
        var allUsersInChat = conversationHistoryService.getAllUsersInChat(chatId);

        // Собираем все сообщения от всех пользователей
        List<ConversationHistoryService.Message> allMessages = allUsersInChat.values()
                .stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(ConversationHistoryService.Message::timestamp))
                .toList();

        if (!allMessages.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            ConversationHistoryService.Message lastMessage = allMessages.get(allMessages.size() - 1);
            long lastMessageTime = lastMessage.timestamp();
            long timeDifference = currentTime - lastMessageTime;

            logger.debug("Последнее сообщение было {} минут назад", timeDifference / (60 * 1000));

            // Если прошло больше MAX_IDLE_TIME_MINUTES минут
            if (timeDifference > MAX_IDLE_TIME_MILLIS) {
                logger.info("Обнаружен длительный перерыв ({} минут). Очищаем историю чата {}",
                        timeDifference / (60 * 1000), chatId);

                // Очищаем историю для ВСЕХ пользователей в чате
                conversationHistoryService.clearAllHistory(chatId);

                // Инициируем список сообщений системным промтом
                allMessages = conversationHistoryService.initHistory(chatId);
            }
        }

        // Преобразуем в JSON формат
        for (ConversationHistoryService.Message msg : allMessages) {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", msg.role());
            messageNode.put("content", msg.content());
            messages.add(messageNode);
        }

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

