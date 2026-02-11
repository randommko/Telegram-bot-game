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
import java.time.Duration;
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
    private final static Map<Long, LocalTime> lastAiQuestion = new HashMap<>();

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
        if (!lastAiQuestion.containsKey(chatId))
            lastAiQuestion.put(chatId, LocalTime.now());

        String userName = message.getFrom().getUserName();
        userQuestion = userName + " спрашивает: " + userQuestion;
        String systemPromtForChat;

//        String contextInChat;
//        Duration duration = Duration.between(lastAiQuestion.get(chatId), LocalTime.now());

//        if (duration.toHours() < 1)
//        Заполнить контекст последними сообщениями в чате
//            contextInChat

        if (Objects.equals(chatId, MY_CHAT_ID))
            systemPromtForChat = AI_CONTEXT_FOR_MY_CHAT;
        else
            systemPromtForChat = AI_CONTEXT;

        String aiAnswer = sendRequestToAi(settings.getSettingValue(systemPromtForChat), userQuestion, answerTemperature);
        if (aiAnswer != null) {
            sender.sendMessage(chatId, aiAnswer);
        }
    }

    public void summary(Message message) {
        Long chatId = message.getChatId();
        List<ChatMessage> context = new ArrayList<>();

        context.add(new ChatMessage(AiChatRole.SYSTEM, AI_SUMMARY_CONTEXT));

        context.addAll(repo.getChatContext(chatId, 100));

        String fullAnswer = sendRequestToAi(context, summaryTemperature);
        if (fullAnswer != null) {
            sender.sendMessage(chatId, fullAnswer);
        }
    }
    public void supportDialogue(Message message) {
        /*
        Алгоритм:
        1. Получить текущее сообщение
        2. Определить нужно ли поддержать диалог в этом месте
        3. Если нужно, сформировать и отправить ответ
         */
        Long chatId = message.getChatId();
        String jsonResponse = defineSupportDialogueNecessity(
                repo.getChatContext(chatId, 100),
                """
                        Ты универсальный всезнайка со своим мнением. Прочитай историю переписки и 
                        реши можешь ли ты вступить в диалог и сказать что-то интересное. 
                        Можно давать интересные факты. Давать определения сложных терминов.
                        Дай ответ в формате JSON:
                        {
                        	"needSendToChat": true,
                        	"reason": "String",
                        	"answerMsg": "String"
                        }
                        Если сказать нечего (needSendToChat = false), то заполни только причину (reason)
                        Если есть что сказать (needSendToChat = true), то причину (reason) не заполняй
                        """
        );

        try {
            // Парсим JSON-ответ
            ObjectMapper objectMapper = new ObjectMapper();
            // Извлекаем JSON из ```json ... ```

            Pattern pattern = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(jsonResponse.trim());
            String cleanJson;
            if (matcher.find()) {
                cleanJson = matcher.group(1).trim();
            } else {
                // Fallback: если нет блока, пробуем как есть
                cleanJson = jsonResponse.trim();
            }

            JsonNode responseNode = objectMapper.readTree(cleanJson);

            boolean needSendToChat = responseNode.get("needSendToChat").asBoolean();
            String reason = responseNode.get("reason").asText();
            String answerMsg = responseNode.get("answerMsg").asText();

            if (needSendToChat) {
                // Отправляем сообщение в чат
                sender.sendMessage(chatId, answerMsg);
            } else {
                // Логируем причину
                logger.info("ИИ решил не отвечать. Причина: {}", reason);
            }

        } catch (JsonProcessingException e) {
            logger.error("Ошибка парсинга JSON ответа: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Ошибка обработки ответа: {}", e.getMessage());
        }
    }

    private String defineSupportDialogueNecessity(List<ChatMessage> context, String systemPromt) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", "deepseek-chat");
            ArrayNode messages = objectMapper.createArrayNode();
            messages.addObject().put("role", AiChatRole.SYSTEM.value).put("content", systemPromt);

            // Преобразуем контекст в строку
            String contextAsString = convertContextToString(context);
            messages.addObject().put("role", AiChatRole.USER.value).put("content", contextAsString);

            request.set("messages", messages);
            request.put("temperature", supportDialogueTemperature);
            request.put("max_tokens", maxTokens);

            return sendHttpRequest(request);
        } catch (Exception e) {
            logger.error("AI не смог ответить: {}", e.getMessage());
            return null;
        }
    }

    private String convertContextToString(List<ChatMessage> context) {
        if (context == null || context.isEmpty()) {
            return "История переписки отсутствует";
        }

        StringBuilder sb = new StringBuilder("История переписки:\n");
        for (ChatMessage msg : context) {
            // Предполагаем, что ChatMessage имеет методы getRole() и getContent()
            sb.append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    private String sendRequestToAi(String context, String userQuestion, Float temperature) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", "deepseek-chat");
            ArrayNode messages = objectMapper.createArrayNode();
            messages.addObject().put("role", "system").put("content", context);
            //Надо добавлять сообщения последовательно из истории чата что бы хранить историю переписки
            //Нужно отдельная функция наполнения ArrayNode messages = objectMapper.createArrayNode() сообщениями
            //или наполнять историю на "горячую" сразу после полчения сообщений и хранить в памяти в Map<chatId, Map<user, message>>
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

    private String sendRequestToAi(List<ChatMessage> context, Float temperature) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", "deepseek-chat");
            ArrayNode messages = objectMapper.createArrayNode();
            for (ChatMessage msg : context) {
                ObjectNode msgNode = messages.addObject();
                msgNode.put("role", msg.getRole().value);
                msgNode.put("content", msg.getContent());
            }
            request.set("messages", messages);
            request.put("temperature", temperature);
            request.put("max_tokens", maxTokens);

            return sendHttpRequest(request);
        } catch (Exception e) {
            logger.error("AI не смог ответить: {}", e.getMessage());
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

