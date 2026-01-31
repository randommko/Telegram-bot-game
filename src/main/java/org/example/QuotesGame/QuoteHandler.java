package org.example.QuotesGame;

import chat.giga.client.GigaChatClient;
import chat.giga.model.ModelName;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import org.example.DTO.QuoteDTO;
import org.example.TelegramBot;
import org.telegram.telegrambots.meta.api.objects.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

public class QuoteHandler {
    private final QuoteRepository repo = new QuoteRepository();
    private static final Logger logger = LoggerFactory.getLogger(QuoteHandler.class);
    private final TelegramBot bot;
    private final GigaChatClient aiClient;

    public QuoteHandler() {
        bot = TelegramBot.getInstance();
        aiClient = TelegramBot.getAi();
    }
    private String analyzeAndSaveQuoteIfWorth(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!repo.canSaveQuote(chatId, userId)) {
            logger.debug("Лимит цитат для chat={}, user={}", chatId, userId);
            return null;
        }

        String text = message.getText();
        String prompt = """
        Это сообщение из чата друзей: "%s".
        Стоит ли его сохранить как смешную цитату на память?
        Ответь в формате:
        ДА: [короткая причина, почему смешно/мудро]
        НЕТ: [короткая причина]
        """.formatted(text);

        try {
            CompletionRequest request = CompletionRequest.builder()
                    .model(ModelName.GIGA_CHAT)
                    .message(ChatMessage.builder()
                            .role(ChatMessageRole.SYSTEM)
                            .content("""
                    Ты очень смешной комик. Выбирай и сохраняй смешные и классные цитаты дружеского чата.
                    Нецензурные выражения сохранять можно и даже приветствуется.
                    Объясняй коротко причину своего решения в формате ДА: или НЕТ:.
                    """)
                            .build())
                    .message(ChatMessage.builder()
                            .role(ChatMessageRole.USER)
                            .content(prompt)
                            .build())
                    .temperature(0.3F)  // чуть больше креатива
                    .maxTokens(30)      // хватит на ответ + причину
                    .build();

            CompletionResponse response = aiClient.completions(request);
            String fullAnswer = response.choices()
                    .get(0)
                    .message()
                    .content()
                    .trim();

            logger.info("GigaChat анализ цитаты '{}': {}", text, fullAnswer);

            if (fullAnswer.toUpperCase().startsWith("ДА")) {
                logger.info("СОХРАНЯЕМ цитату: {}", fullAnswer);
                return "ДА";
            } else {
                logger.info("ОТКЛОНЕНО: {}", fullAnswer);
                return null;
            }

        } catch (Exception e) {
            logger.error("AI анализ не удался для '{}': {}", text, e.getMessage());
            return null;
        }
    }


//    private String analyzeAndSaveQuoteIfWorth(Message message) {
//
//        Long chatId = message.getChatId();
//        Long userId = message.getFrom().getId();
//
//        if (!repo.canSaveQuote(chatId, userId)) {
//            return null;
//        }
//
//        String text = message.getText();
//        String prompt = """
//        Это сообщение из чата друзей: "%s".
//        Стоит ли его сохранить как смешную цитату на память?
//        Ответь ТОЛЬКО 'ДА' или 'НЕТ'.
//        """.formatted(text);
//
//        try {
//            CompletionRequest request = CompletionRequest.builder()
//                    .model(ModelName.GIGA_CHAT)          // или другой доступный
//                    .message(ChatMessage.builder()
//                            .role(ChatMessageRole.SYSTEM)
//                            .content("Ты очень смешной комик. Выбирай и сохраняй смешные и классные цитаты дружеского чата.")
//                            .build())
//                    .message(ChatMessage.builder()
//                            .role(ChatMessageRole.USER)
//                            .content(prompt)
//                            .build())
//                    .temperature(0.1F)
//                    .maxTokens(8)
//                    .build();
//
//            CompletionResponse response = aiClient.completions(request);
//            return response.choices()
//                    .get(0)
//                    .message()
//                    .content()
//                    .trim()
//                    .toUpperCase();
//
//
//        } catch (Exception e) {
//            logger.error("AI анализ не удался: " + e.getMessage());
//            return null;
//        }
//    }

    public void handleSaveQuote(Message message) {

        String text = message.getText();
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (message.hasText()) {
            if (text.length() > 10 && text.length() < 300 && !isBotCommand(message)) {
                String aiResult = analyzeAndSaveQuoteIfWorth(message);
                if ("ДА".equals(aiResult)) {
                    repo.saveQuote(text, chatId, userId);
//                    bot.sendMessage(chatId, "🤖 ИИ сохранил мудрую цитату: «" + text + "» ✨");
                    logger.info("ИИ сохранил мудрую цитату: «" + text + "»");
                }
            }
        }
    }

    public void getRandomQuote(Long chatId) {
        QuoteDTO quoteDTO;
        quoteDTO = repo.handleRandomQuote(chatId);
        if (quoteDTO != null) {
            String text = quoteDTO.date.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                    + ", " + quoteDTO.userName + " сказал: \"" + quoteDTO.text + "\"";
            bot.sendMessage(chatId, text);
            return;
        }
        bot.sendMessage(chatId, "Нет сохраненных цитат");
    }
    private boolean isBotCommand(Message message) {
        String text = message.getText();
        Long userId = message.getFrom().getId();
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.trim().startsWith("/");
    }
}
