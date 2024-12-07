package org.example;

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
import java.util.Random;
import java.util.regex.Pattern;

import static org.example.TablesDB.*;
import static org.example.Utils.*;

public class GameBot extends TelegramLongPollingBot {
    private final String botToken;
    private Map<Long, String> usersSet = new HashMap<>();
    private Map<Long, String> chatSet = new HashMap<>();
    private final Map<Long, Quiz> quizMap = new HashMap<>(); //ключ ID чата, значение экземпляр Quiz
    private static final String RESOURCES_PATH = "/bin/tg_bot/resources";
    private static final Logger logger = LoggerFactory.getLogger(GameBot.class);
    private static final int quizClueTimer = 10000;
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
                case "/cocksize", "/cocksize@ChatGamePidor_Bot" -> cockSize(message);
                case "/quiz_start", "/quiz_start@ChatGamePidor_Bot" -> startQuizGame(message);
                case "/quiz_stop", "/quiz_stop@ChatGamePidor_Bot" -> stopQuiz(message);
                case "/quiz_stats", "/quiz_stats@ChatGamePidor_Bot" -> getQuizStats(message);
                default -> checkQuizAnswer(message);
            }
        }
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
    private void getQuizStats(Message message) {
        Long chatID = message.getChatId();
        Map<String, Integer> stats;
        stats = quizMap.get(chatID).getScore(chatID);

        StringBuilder statsMessage = new StringBuilder("Статистика викторины:\n");
        stats.forEach((userName, score) ->
            statsMessage.append(userName).append(": ").append(score).append(" очков\n")
        );
        sendMessage(chatID, statsMessage.toString());
    }

    private void startQuizGame(Message message) {
        Long chatID = message.getChatId();
        quizMap.put(chatID, new Quiz());
        quizMap.get(chatID).isQuizStarted = true;
        Thread thread = new Thread(() -> {
            do {
                //TODO: подумать как правильный отправлять подсказки -
                // сейчас если был дан верный ответ счетчик подсказок не сбрасывается
                quizMap.get(chatID).newQuestion();
                sendQuestion(chatID);
                do {
                    try {
                        Thread.sleep(quizClueTimer);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    quizMap.get(chatID).updateClue();
                    if (quizMap.get(chatID).isQuizStarted)
                        sendMessage(chatID, "Подсказка: " + quizMap.get(chatID).clue);
                } while ((quizMap.get(chatID).getRemainingNumberOfClue() > 2) & (quizMap.get(chatID).isQuizStarted));

                if (quizMap.get(chatID).isQuizStarted) {
                    sendMessage(chatID, "Правильный ответ: " + quizMap.get(chatID).currentAnswer);
                    quizMap.get(chatID).noAnswerCount++;
                }
                if (quizMap.get(chatID).noAnswerCount >= 3)
                    stopQuiz(message);
            } while (quizMap.get(chatID).isQuizStarted);
        });
        thread.start();
    }

    private void stopQuiz (Message message) {
        Long chatID = message.getChatId();
        quizMap.get(chatID).isQuizStarted = false;
        sendMessage(chatID, "Викторина завершена");
    }
    private void checkQuizAnswer(Message message) {
        Long chatID = message.getChatId();
        String answer = message.getText();
        Long userID = message.getFrom().getId();

        if (!quizMap.get(chatID).isQuizStarted)
            return;
        obsceneAnswer(message);
        if (quizMap.get(chatID).currentAnswer.equalsIgnoreCase(answer)) {
            quizMap.get(chatID).noAnswerCount = 0;
            Integer points = quizMap.get(chatID).calculatePoints(answer.toLowerCase());
            quizMap.get(chatID).setScore(userID, points, chatID);
            sendReplyMessage(message, "Правильный ответ! Вы заработали " + points.toString() + " очков!");
            quizMap.get(chatID).newQuestion();
            sendQuestion(chatID);
        }
    }

    private void obsceneAnswer(Message message) {
        String answer = message.getText();
        Long chatID = message.getChatId();
        Integer messageID = message.getMessageId();

        Set<String> obscenePatterns = new HashSet<>();
        obscenePatterns.add("^хуй.*");
        obscenePatterns.add(".*хуй.*");
        obscenePatterns.add("^хуе.*");
        obscenePatterns.add("^хуё.*");
        obscenePatterns.add("^хуи.*");
        obscenePatterns.add("^Пидор.*");
        obscenePatterns.add("^пидор.*");
        obscenePatterns.add(".*ну и пошёл ты нахуй.*");
        obscenePatterns.add(".*да идиты в жопу.*");
        obscenePatterns.add(".*пиздец.*");

        List<String> botAnswerList = new ArrayList<>();
        botAnswerList.add("Пошел на хер");

        String botAnswer = "Сам такой";
        Random random = new Random();

        for (String item : obscenePatterns) {
                if (Pattern.matches(item, answer.toLowerCase())) {
                    int randomIndex = random.nextInt(botAnswerList.size());
                    botAnswer = botAnswerList.get(randomIndex);
                }
            }

        int randomNum = random.nextInt(10);
        if (randomNum == 5)
            sendReplyMessage(message, botAnswer);

    }

    private void sendQuestion(Long chatID) {
        if (!quizMap.get(chatID).currentQuestionText.isEmpty()) {
            sendMessage(chatID, quizMap.get(chatID).currentQuestionText);
            sendClue(chatID);
        }
        else {
            sendMessage(chatID, "В БД нет вопросов");
            quizMap.get(chatID).isQuizStarted = false;
        }
    }


    private void sendClue(Long chatID) {
        sendMessage(chatID, "Подсказка: " + quizMap.get(chatID).clue);
    }

    private void cockSize(Message message) {
        Long userID = message.getFrom().getId();
        String userName = message.getFrom().getUserName();
        Long chatID = message.getChatId();

        int playerCockSize = getPlayerCockSize(userID);
        if (playerCockSize != -1) {
            if (!sendImgMessage(chatID, phraseSelection(playerCockSize, userName), playerCockSize))
                sendMessage(chatID, phraseSelection(playerCockSize, userName));
            return;
        }

        // Если записи нет, генерируем случайный размер и сохраняем его
        int newRandomSize = getCockSize();

        setCockSizeWinner(userID, newRandomSize);
        if (!sendImgMessage(chatID, phraseSelection(newRandomSize, userName), newRandomSize))
            sendMessage(chatID, phraseSelection(newRandomSize, userName));
    }

    private void registerPlayer(Message message) {
        Long chatID = message.getChatId();
        String userName = message.getFrom().getUserName();
        Long userID = message.getFrom().getId();

        String insertQuery = "INSERT INTO " + PIDOR_PLAYERS_TABLE + " (chat_id, user_id) VALUES (?, ?) ON CONFLICT (chat_id, user_id) DO NOTHING";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);

            preparedStatement.setLong(1, chatID);
            preparedStatement.setLong(2, userID);
            preparedStatement.executeUpdate();

            sendMessage(chatID, "Игрок @" + userName + " зарегистрирован! ");
        } catch (SQLException e) {
            logger.error("Ошибка при регистрации игрока в БД: " + e);
            sendMessage(chatID, "Произошла ошибка при регистрации игрока @" + userName + "\n" + e.getMessage());
        }
    }

    private void sendPidorStats(Message message) {
        Long chatId = message.getChatId();
        Thread thread = new Thread(() -> {
            String query = "SELECT pst.user_id, COUNT(*) AS count " +
                    "FROM " + PIDOR_STATS_TABLE +
                    " AS pst JOIN " + TG_USERS_TABLE +
                    " AS tut ON pst.user_id = tut.user_id " +
                    "WHERE pst.chat_id = ? " +
                    "GROUP BY pst.user_id";

            Map<String, Integer> winnersCount = new HashMap<>();

            try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
                PreparedStatement preparedStatement = connection.prepareStatement(query);

                preparedStatement.setLong(1, chatId);
                ResultSet resultSet = preparedStatement.executeQuery();

                while (resultSet.next())
                    winnersCount.put(usersSet.get(resultSet.getLong("user_id")), resultSet.getInt("count"));

                if (winnersCount.isEmpty()) {
                    sendMessage(chatId, "Статистика пуста.");
                    return;
                }

                StringBuilder statsMessage = new StringBuilder("Статистика пидоров:\n");
                winnersCount.forEach((winner, count) ->
                        statsMessage.append(winner).append(": ").append(count).append("\n")
                );

                sendMessage(chatId, statsMessage.toString());

            } catch (SQLException e) {
                logger.error("Ошибка при получении статистики из БД: ", e);
                sendMessage(chatId, "Ошибка при получении статистики.");
            }
        });
        thread.start();
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

    private void startPidorGame(Message message) {
        Long chatID = message.getChatId();
        Thread thread = new Thread(() -> {
            Set<Long> chatPlayers;
            Long winnerID = getTodayWinner(chatID);

            if (winnerID != null) {
                sendMessage(chatID, "Сегодня пидора уже выбрали. Пидор дня: " + getUserNameByID(winnerID));
                return;
            }

            chatPlayers = getPidorGamePlayers(chatID);
            if (chatPlayers.isEmpty()) {
                sendMessage(chatID, "Нет зарегистрированных игроков.");
                return;
            }

            List<String> responses = getRandomResponses();
            try {
                for (String response : responses) {
                    sendMessage(chatID, response);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                logger.error("Произошла ошибка при сохранении победителя в БД: ", e);
            }

            winnerID = new ArrayList<>(chatPlayers).get(new Random().nextInt(chatPlayers.size()));

            setPidorWinner(chatID, winnerID);

            sendMessage(chatID, getWinnerResponce() + getUserNameByID(winnerID) + "!");
        });
        thread.start();
    }

    private void sendMessage(Long chatID, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatID);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
        }
    }

    private boolean sendImgMessage (Long chatId, String text, Integer size) {
        String imgName = getCockSizeImage(size);
        if (imgName == null)
            return false;

        File imageFile = new File(RESOURCES_PATH, imgName);

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

    private boolean sendReplyMessage(Message message, String messageText) {
        SendMessage response = new SendMessage();
        response.setChatId(message.getChatId());
        response.setText(messageText);
        response.setReplyToMessageId(message.getMessageId()); // Привязываем к конкретному сообщению

        try {
            execute(response);
            return true;
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
            return false;
        }

    }
}
