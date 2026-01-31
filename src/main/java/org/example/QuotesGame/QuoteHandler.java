package org.example.QuotesGame;

import chat.giga.client.GigaChatClient;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
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
        Стоит ли сохранить как мудрую/смешную цитату? 
        Ответь ТОЛЬКО 'ДА' или 'НЕТ'.
        """.formatted(text);

        try {
            CompletionRequest request = CompletionRequest.builder()
                    .model("gpt-4o-mini")
                    .messages(List.of(ChatMessage.builder()
                                    .role(ChatMessageRole.SYSTEM)
                                    .content("Ты строгий критик цитат. Сохраняй только действительно мудрые или очень смешные.")
                                    .build(),
                            ChatMessage.builder()
                                    .role(ChatMessageRole.USER)
                                    .content(prompt)
                                    .build()))
                    .maxTokens(5)
                    .temperature(0.1F)  // мало рандома
                    .build();

            CompletionResponse response  = aiClient.completions(request);
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

        if (message.hasText()) {
            String text = message.getText();
            if (text.length() > 10 && text.length() < 300 && !isBotCommand(text)) {
                analyzeAndSaveQuoteIfWorth(message);
            }

            Message reply = message.getReplyToMessage();
            if (reply == null) {
                bot.sendMessage(message.getChatId(), "Ответь этой командой на сообщение с цитатой 🙃");
                return;
            }

            String quoteText = reply.getText();

            if (quoteText == null || quoteText.trim().isEmpty()) {
                bot.sendMessage(message.getChatId(), "В этом сообщении нет текста, нечего сохранять 🤔");
                return;
            }

            Long chatId = message.getChatId();
            Long authorId = reply.getFrom().getId();

            repo.saveQuote(text, chatId, authorId);

        }
    }

    private boolean isBotCommand(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.trim().startsWith("/");
    }
}
