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
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

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
//    private final ArrayNode chatHistoryMessages = objectMapper.createArrayNode();

    public AiChat(String aiToken) {
        this.deepseekApiKey = aiToken;
        bot = TelegramBot.getInstance();
        sender = new MessageSender(bot);
    }

    public void addMessageToHistoryAi(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();
        String userName = message.getFrom().getUserName();
        checkContextChatAi(message);

        chatHistory.get(chatId).addObject()
                .put("role", "user")
                .put("content", "Сообщение от: " + userName + " : " + text);

        logger.info("Сохранено сообщение в контекст ИИ для чата " + message.getChat().getTitle());

        timeOfLastAiQuestionInChat.replace(chatId, LocalTime.now());
    }

    private void checkContextChatAi(Message message) {
        Long chatId = message.getChatId();
        if (!timeOfLastAiQuestionInChat.containsKey(chatId))
            timeOfLastAiQuestionInChat.put(chatId, LocalTime.now());

        Duration duration = Duration.between(timeOfLastAiQuestionInChat.get(chatId), LocalTime.now());

        if (!chatHistory.containsKey(chatId)) {
            ArrayNode historyArray = objectMapper.createArrayNode();

            try {
                historyArray.addObject()
                        .put("role", "system")
                        .put("content", getSystemPromtForChat(chatId));
                chatHistory.put(chatId, historyArray);
            }
            catch (Exception e) {
                logger.error("Ошибка инициализации чата: " + e);
            }

        }

        if (duration.toHours() > 1) {
            chatHistory.get(chatId).removeAll();
            logger.info("Контекст ИИ обнулен для чата: " + message.getChat().getTitle());
            chatHistory.get(chatId).addObject()
                    .put("role", "system")
                    .put("content", getSystemPromtForChat(chatId));
        }
    }

    public void askAi(Message message) {
        String[] parts = message.getText().split(" ", 2);
        String userQuestion = parts.length > 1 ? parts[1] : "";
        Long chatId = message.getChatId();

        if (userQuestion.isEmpty() || userQuestion.isBlank()) {
            sender.sendMessage(chatId, "Напиши свой вопрос после команды /ai");
            return;
        }

        checkContextChatAi(message);

        String userName = message.getFrom().getUserName();

        chatHistory.get(chatId).addObject()
                .put("role", "user")
                .put("content", userName + " спрашивает: " + userQuestion);


        String aiAnswer = sendRequestToAi(chatHistory.get(chatId), answerTemperature);
        logger.info("Задан вопрос ИИ в чате: " + message.getChat().getTitle());
        if (aiAnswer != null) {
            chatHistory.get(chatId).addObject()
                    .put("role", "system")
                    .put("content", aiAnswer);
//            chatHistory.put(chatId, chatHistoryMessages);
            timeOfLastAiQuestionInChat.replace(chatId, LocalTime.now());
            sender.sendMessage(chatId, aiAnswer);
        }
    }

    private String getSystemPromtForChat(Long chatId) {
        if (Objects.equals(chatId, MY_CHAT_ID))
            return AI_CONTEXT_FOR_MY_CHAT;
        return AI_CONTEXT;
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

