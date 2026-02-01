package org.example.AiChat;

import chat.giga.client.GigaChatClient;
import chat.giga.model.ModelName;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import org.example.Settings.SettingsService;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

import static org.example.Settings.Settings.*;

public class AiChat {
    private static final Logger logger = LoggerFactory.getLogger(AiChat.class);
    private final TelegramBot bot;
    private final GigaChatClient aiClient;
    private final SettingsService settings = new SettingsService();
    private final ContextRepo repo = new ContextRepo();
    private final Float temperature = Float.valueOf(settings.getSettingValue(AI_CREATIVE_TEMPERATURE));
    private final Integer maxTokens = Integer.valueOf(settings.getSettingValue(AI_MAX_TOKENS_ANSWER_QUESTION));

    public AiChat() {
        bot = TelegramBot.getInstance();
        aiClient = TelegramBot.getAi();
    }

    public void askAi(Message message) {
        String[] parts = message.getText().split(" ", 2);
        String userQuestion = parts[1];
        Long chatId = message.getChatId();
        if (userQuestion.isEmpty() || userQuestion.isBlank()) {
            bot.sendMessage(chatId, "Напиши свой вопрос после команды /ai");
            return;
        }

        String content = settings.getSettingValue(AI_CONTEXT);

        String aiAnswer = sendRequestToAi(content, userQuestion);
        bot.sendMessage(chatId, aiAnswer);

    }
    public void summary(Message message) {
        Long chatId = message.getChatId();
        List<ChatMessage> context = repo.getChatContext(chatId, 100);
//        String context = settings.getSettingValue(AI_SUMMARY_QUESTION);
        context.add(ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content("Прочитай историю чата и перескажи в нашем стиле")
                .build());

        try {
            CompletionRequest request = CompletionRequest.builder()
                    .model(ModelName.GIGA_CHAT)
                    .messages(context)
                    .temperature(temperature)  // чуть больше креатива
                    .maxTokens(maxTokens)      // длинна ответа
                    .build();

            CompletionResponse response = aiClient.completions(request);
            String fullAnswer = response.choices()
                    .get(0)
                    .message()
                    .content()
                    .trim();

            bot.sendMessage(chatId, fullAnswer);
        } catch (Exception e) {
            logger.error("AI не смог пересказать историю чата ={}", e.getMessage());
        }
    }

    private String sendRequestToAi(String content, String userQuestion) {
        try {
            CompletionRequest request = CompletionRequest.builder()
                    .model(ModelName.GIGA_CHAT)
                    .message(ChatMessage.builder()
                            .role(ChatMessageRole.SYSTEM)
                            .content(content)
                            .build())
                    .message(ChatMessage.builder()
                            .role(ChatMessageRole.USER)
                            .content(userQuestion)    //запрос пользователя
                            .build())
                    .temperature(temperature)  // чуть больше креатива
                    .maxTokens(maxTokens)      // длинна ответа
                    .build();

            CompletionResponse response = aiClient.completions(request);

            return response.choices()
                    .get(0)
                    .message()
                    .content()
                    .trim();
        } catch (Exception e) {
            logger.error("AI не смог ответить '{}': {}", userQuestion, e.getMessage());
            return null;
        }
    }
}
