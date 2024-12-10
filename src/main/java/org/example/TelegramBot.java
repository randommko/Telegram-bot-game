package org.example;

import org.example.Chats.ChatsService;
import org.example.CockSize.CockSizeGame;
import org.example.PidorGame.PidorGame;
import org.example.QuizGame.QuizGame;
import org.example.Users.UsersService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public class TelegramBot extends TelegramLongPollingBot {
    private final String botToken;
    private static TelegramBot instance;
    private final UsersService usersService = new UsersService();
    private final ChatsService chatsService = new ChatsService();
    private final CockSizeGame cockSizeGame = new CockSizeGame();
    private final PidorGame pidorGame = new PidorGame();
    private final QuizGame quizGame = new QuizGame();
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    public TelegramBot(String botToken) {
        this.botToken = botToken;
        instance = this;
    }
    @Override
    public String getBotUsername() {
        return "Викторина бот"; // Замените на имя вашего бота
    }
    @Override
    public String getBotToken() {
        return botToken;
    }
    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (!usersService.checkUser(message.getFrom().getId()))
            usersService.addUser(
                    message.getFrom().getUserName(),
                    message.getFrom().getId()
            );

        if (!chatsService.checkChat(message.getChatId()))
            chatsService.addChat(
                    message.getChatId(),
                    message.getChat().getTitle()
            );

        logger.info("Получено сообщение из чата " + message.getChat().getId().toString() +": "+ message.getText());
        if (update.hasMessage()) {
            String command = message.getText();
            switch (command) {
                case "/bot_info", "/bot_info@ChatGamePidor_Bot", "/help", "/help@ChatGamePidor_Bot" -> botInfo(message);
                case "/cocksize", "/cocksize@ChatGamePidor_Bot" -> cockSizeGame.cockSizeStart(message);
                case "/pidor_reg", "/pidor_reg@ChatGamePidor_Bot" -> pidorGame.registerPlayer(message.getChatId(), message.getFrom().getId());
                case "/pidor_stats", "/pidor_stats@ChatGamePidor_Bot" -> pidorGame.sendPidorStats(message.getChatId());
                case "/pidor_start", "/pidor_start@ChatGamePidor_Bot" -> pidorGame.startPidorGame(message.getChatId());
                case "/quiz_start", "/quiz_start@ChatGamePidor_Bot" -> quizGame.startQuizGame(message);
                case "/quiz_stop", "/quiz_stop@ChatGamePidor_Bot" -> quizGame.stopQuiz(message.getChatId());
                case "/quiz_stats", "/quiz_stats@ChatGamePidor_Bot" -> quizGame.getQuizStats(message);
                default -> quizGame.checkQuizAnswer(message);
            }
        }
    }
    public static TelegramBot getInstance() {
        return instance;
    }
    private void botInfo(Message message) {
        Long chatID = message.getChatId();
        sendMessage(chatID, """
                 Этот бот создан для определения пидора дня в чате! Команды:
                 /cocksize - Измерить причиндалы
                 /quiz_start - Запустить викторину
                 /quiz_stop - Остановить викторину
                 /quiz_stats - Статистика викторина
                 /pidor_start - Найти пидора дня
                 /pidor_reg - Добавиться в игру поиска пидоров
                 /pidor_stats - Статистика пидоров""");
    }
    public Integer sendMessage(Long chatID, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatID);
        message.setText(text);
        try {
            return execute(message).getMessageId();
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
        }
        return null;
    }
    public boolean sendReplyMessage(Long chatId, Integer replyMessageID, String messageText) {
        SendMessage response = new SendMessage();
        response.setChatId(chatId);
        response.setText(messageText);
        response.setReplyToMessageId(replyMessageID); // Привязываем к конкретному сообщению

        try {
            execute(response);
            return true;
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
            return false;
        }
    }
    public boolean sendImgMessage (Long chatId, String text, File imageFile) {

        if (imageFile.exists()) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(new InputFile(imageFile));
            sendPhoto.setCaption(text);

            try {
                execute(sendPhoto);
                return true;
            } catch (TelegramApiException e) {
                logger.error("Ошибка при отправке сообщения: ", e);
            }
        } else {
            logger.error("Image file not found: " + imageFile.getPath());
            return false;
        }
        return false;
    }


}
