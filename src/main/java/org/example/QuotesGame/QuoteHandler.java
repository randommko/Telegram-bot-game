package org.example.QuotesGame;

import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import org.example.TelegramBot;
import org.telegram.telegrambots.meta.api.objects.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

import static org.example.TelegramBot.aiClient;


public class QuoteHandler {
    private final QuoteRepository repo = new QuoteRepository();
    private static final Logger logger = LoggerFactory.getLogger(QuoteHandler.class);
    private final TelegramBot bot;

    public QuoteHandler() {
        bot = TelegramBot.getInstance();
    }

    private void analyzeAndSaveQuoteIfWorth(Message message) {

        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        // Лимит: 1 цитата в час от пользователя
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
            ChatCompletionRequest request = ChatCompletionRequest.builder()
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
                    .temperature(0.1)  // мало рандома
                    .build();

            ChatCompletionResult result = aiClient.chat().completions().create(request);
            String aiAnswer = result.getChoices().get(0).getMessage().getContent().trim().toUpperCase();

            if ("ДА".equals(aiAnswer)) {
                repo.saveQuote(text, chatId, userId);
                bot.sendMessage(chatId, "🤖 ИИ сохранил мудрую цитату: «" + text + "» ✨");
            }

        } catch (Exception e) {
            logger.error("AI анализ не удался: " + e.getMessage());
        }
    }

    private void handleSaveQuote(Message message) {

        if (message.hasText()) {
            String text = message.getText();
            if (text.length() > 10 && text.length() < 300 && !isBotCommand(text)) {
                analyzeAndSaveQuoteIfWorth(message);
            }

        Message reply = message.getReplyToMessage();
        if (reply == null) {
            sendMessage(message.getChatId(), "Ответь этой командой на сообщение с цитатой 🙃");
            return;
        }

        String quoteText = reply.getText();
        if (quoteText == null || quoteText.trim().isEmpty()) {
            sendMessage(message.getChatId(), "В этом сообщении нет текста, нечего сохранять 🤔");
            return;
        }

        Long chatId = message.getChatId();
        Long authorId = reply.getFrom().getId();
        Long saverId = message.getFrom().getId();

        //тут вызов saveQoute()
    }


}
