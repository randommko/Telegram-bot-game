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

import java.util.List;



public class QuoteHandler {
    private final QuoteRepository repo = new QuoteRepository();
    private static final Logger logger = LoggerFactory.getLogger(QuoteHandler.class);
    private final TelegramBot bot;
    private final GigaChatClient aiClient;

    public QuoteHandler() {
        bot = TelegramBot.getInstance();
        aiClient = TelegramBot.getAi();
    }

    private void analyzeAndSaveQuoteIfWorth(Message message) {

        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!repo.canSaveQuote(chatId, userId)) {
            return;
        }

        String text = message.getText();
        String prompt = """
        Это сообщение из чата друзей: "%s".
        Стоит ли его сохранить как смешную цитату? 
        Ответь ТОЛЬКО 'ДА' или 'НЕТ'.
        """.formatted(text);

        try {
            CompletionRequest request = CompletionRequest.builder()
                    .model(ModelName.GIGA_CHAT)          // или другой доступный
                    .message(ChatMessage.builder()
                            .role(ChatMessageRole.SYSTEM)
                            .content("Ты очень смешной комик. Выбирай и сохраняй смешные и классные цитаты дружеского чата.")
                            .build())
                    .message(ChatMessage.builder()
                            .role(ChatMessageRole.USER)
                            .content(prompt)
                            .build())
                    .temperature(0.1F)
                    .maxTokens(8)
                    .build();

            CompletionResponse response = aiClient.completions(request);
            String aiAnswer = response.choices()
                    .get(0)
                    .message()
                    .content()
                    .trim()
                    .toUpperCase();

            if ("ДА".equals(aiAnswer)) {
                repo.saveQuote(text, chatId, userId);
                bot.sendMessage(chatId, "🤖 ИИ сохранил мудрую цитату: «" + text + "» ✨");
            }

        } catch (Exception e) {
            logger.error("AI анализ не удался: " + e.getMessage());
        }
    }

    public void handleSaveQuote(Message message) {

        String text = message.getText();
        if (message.hasText()) {
            if (text.length() > 10 && text.length() < 300 && !isBotCommand(message)) {
                analyzeAndSaveQuoteIfWorth(message);
            }
        }
    }

    public void getRandomQoute(Long chatId) {
        QuoteDTO quoteDTO;
        quoteDTO = repo.handleRandomQuote(chatId);
        String text = "Цитата от " + quoteDTO.userName +"\n \"" + quoteDTO.text + "\"";
        bot.sendMessage(chatId, text);
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
