package org.example;

import org.example.cockSize.CockSize;
import org.example.pidorGame.PidorGameService;
import org.example.quizGame.Quiz;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import java.io.File;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.example.Utils.*;
import static org.glassfish.grizzly.ProcessorExecutor.execute;

public class GameBot extends TelegramLongPollingBot {
    private final String botToken;
    private Map<Long, String> usersSet = new HashMap<>();
    private Map<Long, String> chatSet = new HashMap<>();
    private PidorGameService pidorService = new PidorGameService();
    private final Map<Long, Quiz> quizMap = new HashMap<>(); //ключ ID чата, значение экземпляр Quiz
    private CockSize cockSize = new CockSize();
    private static final String RESOURCES_PATH = "/bin/tg_bot/resources";
    private static final Logger logger = LoggerFactory.getLogger(GameBot.class);
    private static final int quizClueTimer = 5000;
    //TODO: вынести работу с пользователями и чатами в отдельные классы

    public GameBot(String botToken) {
        this.botToken = botToken;
        iniQuiz();
        usersSet = initUsers();
        chatSet = initChats();
    }
    @Override
    public String getBotUsername() {
        return "Пидорвикторина"; // Замените на имя вашего бота
    }
    @Override
    public String getBotToken() {
        return botToken;
    }
    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (!usersSet.containsKey(message.getFrom().getId()))
            insertUserInDB(message);
        if (!chatSet.containsKey(message.getChatId()))
            insertChatInDB(message);

        System.out.println("Получено сообщение из чата " + message.getChat().getId().toString() +": "+ message.getText());
        if (update.hasMessage()) {
            String command = message.getText();
            switch (command) {
                case "/pidor_reg", "/pidor_reg@ChatGamePidor_Bot" -> registerPlayer(message);
                case "/pidor_stats", "/pidor_stats@ChatGamePidor_Bot" -> sendPidorStats(message);
                case "/pidor_start", "/pidor_start@ChatGamePidor_Bot" -> startPidorGame(message);
                case "/bot_info", "/bot_info@ChatGamePidor_Bot", "/help", "/help@ChatGamePidor_Bot" -> botInfo(message);
                case "/cocksize", "/cocksize@ChatGamePidor_Bot" -> cockSizeStart(message);
                case "/quiz_start", "/quiz_start@ChatGamePidor_Bot" -> startQuizGame(message);
                case "/quiz_stop", "/quiz_stop@ChatGamePidor_Bot" -> stopQuiz(message.getChatId());
                case "/quiz_stats", "/quiz_stats@ChatGamePidor_Bot" -> getQuizStats(message);
                default -> checkQuizAnswer(message);
            }
        }
    }
    private void cockSizeStart(Message message) {
        Integer userSize = null;
        String messageText = null;
        Map<Integer, String> sizeMap = cockSize.getCockSize(message.getFrom().getId());
        for (Map.Entry<Integer, String> entry : sizeMap.entrySet()) {
            userSize = entry.getKey();
            messageText = entry.getValue();
        }

        if (!sendImgMessage(message.getChatId(), messageText, userSize))
            sendMessage(message.getChatId(), messageText);
    }
    private void iniQuiz() {
        List<Long> quizChatIDs = new ArrayList<>();
        String QUIZ_STATS_TABLE = "public.quiz_stats";
        String getScoreQuery = "SELECT DISTINCT chat_id FROM " + QUIZ_STATS_TABLE;
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(getScoreQuery)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        quizChatIDs.add(rs.getLong("chat_id"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при получения счета в БД: ", e);
        }
        quizChatIDs.forEach((chatID) ->
            quizMap.put(chatID, new Quiz())
        );
    }
    private void startQuizGame(Message message) {
        Long chatID = message.getChatId();

        quizMap.put(chatID, new Quiz());
        quizMap.get(chatID).isQuizStarted = true;

        Thread thread = new Thread(() -> {
            do {
                sendQuestion(chatID);
            } while (quizMap.get(chatID).isQuizStarted);
        });
        thread.start();
    }
    private void stopQuiz (Long chatID) {
        quizMap.get(chatID).isQuizStarted = false;
        sendMessage(chatID, "Викторина завершена");
    }

    private void registerPlayer(Message message) {
        pidorService.registerPlayer(message.getChatId(), message.getFrom().getId());
    }
    private void sendPidorStats(Message message) {
        pidorService.sendPidorStats(message.getChatId());
    }
    private void startPidorGame(Message message) {
        //TODO: если отправить две команды подряд то будет выбрано два победителя. Блокировать поиск нового пока не завершился первый
        pidorService.startPidorGame(message.getChatId());
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


    public static Message sendMessage(Long chatID, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatID);
        message.setText(text);
        try {
            return execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
            return null;
        }
    }
    public static boolean sendImgMessage (Long chatId, String text, File imageFile) {

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
    public static boolean sendReplyMessage(Long chatId, Integer replyMessageID, String messageText) {
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
}
