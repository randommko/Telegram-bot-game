package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.File;

public class MessageSender {
    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);
    private final TelegramLongPollingBot bot;

    public MessageSender(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    public Integer sendMessage(Long chatId, String text) {
        SendMessage msg = SendMessage.builder().chatId(chatId).text(text).build();
        try {
            return bot.execute(msg).getMessageId();
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки: ", e);
            return null;
        }
    }

    public Integer sendReplyMessage(Long chatId, Integer replyId, String text) {
        SendMessage msg = SendMessage.builder().chatId(chatId).text(text).replyToMessageId(replyId).build();
        try {
            return bot.execute(msg).getMessageId();
        } catch (TelegramApiException e) {
            logger.error("Ошибка reply: ", e);
            return null;
        }
    }

    public boolean sendImage(Long chatId, String caption, File image) {
        if (!image.exists()) {
            logger.error("Файл не найден: {}", image.getPath());
            return false;
        }
        SendPhoto photo = SendPhoto.builder().chatId(chatId).photo(new InputFile(image)).caption(caption).build();
        try {
            bot.execute(photo);
            return true;
        } catch (TelegramApiException e) {
            logger.error("Ошибка фото: ", e);
            return false;
        }
    }

    public boolean editMessage(Long chatId, Integer msgId, String text) {
        EditMessageText edit = EditMessageText.builder().chatId(chatId).messageId(msgId).text(text).build();
        try {
            bot.execute(edit);
            return true;
        } catch (TelegramApiException e) {
            logger.error("Ошибка edit: ", e);
            return false;
        }
    }

    public void sendMessageWithKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage msg = SendMessage.builder().chatId(chatId).text(text).replyMarkup(keyboard).build();
        // execute...
    }
}
