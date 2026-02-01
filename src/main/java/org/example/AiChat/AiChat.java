package org.example.AiChat;

import chat.giga.client.GigaChatClient;
import chat.giga.model.ModelName;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import org.example.QuotesGame.QuoteHandler;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

public class AiChat {
    private static final Logger logger = LoggerFactory.getLogger(QuoteHandler.class);
    private final TelegramBot bot;
    private final GigaChatClient aiClient;

    public AiChat() {
        bot = TelegramBot.getInstance();
        aiClient = TelegramBot.getAi();
    }

    public void askAi(Message message) {
        String[] parts = message.getText().split(" ", 2);
        String text = parts[1];
        Long chatId = message.getChatId();

//        String prompt = """
//        Это сообщение из чата друзей: "%s".
//        Стоит ли его сохранить как смешную цитату на память?
//        Ответь в формате:
//        ДА: [короткая причина, почему смешно/мудро]
//        НЕТ: [короткая причина]
//        """.formatted(text);


        try {
            CompletionRequest request = CompletionRequest.builder()
                    .model(ModelName.GIGA_CHAT)
                    .message(ChatMessage.builder()
                            .role(ChatMessageRole.SYSTEM)
                            .content("""
                    Ты очень смешной комик. Дай очень смешной ответ. Можно грубить и материться.
                    """)
                            .build())
                    .message(ChatMessage.builder()
                            .role(ChatMessageRole.USER)
                            .content(text)
                            .build())
                    .temperature(0.8F)  // чуть больше креатива
                    .maxTokens(300)      // хватит на ответ + причину
                    .build();

            CompletionResponse response = aiClient.completions(request);
            String fullAnswer = response.choices()
                    .get(0)
                    .message()
                    .content()
                    .trim();

//            logger.info("GigaChat анализ цитаты '{}': {}", text, fullAnswer);
            bot.sendMessage(chatId, fullAnswer);
        } catch (Exception e) {
            logger.error("AI анализ не удался для '{}': {}", text, e.getMessage());
        }



    }
}
