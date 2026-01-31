package org.example.AiChat;

import chat.giga.client.GigaChatClient;
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
        bot.sendMessage(chatId, text);
    }
}
