package org.example.AiChat;

import chat.giga.client.GigaChatClient;
import chat.giga.model.ModelName;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import org.example.QuotesGame.QuoteHandler;
import org.example.Settings.SettingsService;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import static org.example.Settings.Settings.*;

public class AiChat {
    private static final Logger logger = LoggerFactory.getLogger(QuoteHandler.class);
    private final TelegramBot bot;
    private final GigaChatClient aiClient;
    private final SettingsService settings = new SettingsService();

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
        Float temperature = Float.valueOf(settings.getSettingValue(AI_CREATIVE_TEMPERATURE));
        Integer maxTokens = Integer.valueOf(settings.getSettingValue(AI_MAX_TOKENS_ANSWER_QUESTION));

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
            String fullAnswer = response.choices()
                    .get(0)
                    .message()
                    .content()
                    .trim();

            bot.sendMessage(chatId, fullAnswer);
        } catch (Exception e) {
            logger.error("AI не смог ответить '{}': {}", userQuestion, e.getMessage());
        }
    }
}
