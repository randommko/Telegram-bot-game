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

import static org.example.Utils.*;

public class GameBot extends TelegramLongPollingBot {
    private final String botToken;
    private final Map<Long, Quiz> quizMap = new HashMap<>(); //ключ ID чата, значение экземпляр Quiz
    private static final String RESOURCES_PATH = "/bin/tg_bot/resources";
    private static final Logger logger = LoggerFactory.getLogger(GameBot.class);
    private static final int quizClueTimer = 10000;

    public GameBot(String botToken) {
        this.botToken = botToken;
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
        Long chatID = message.getChatId();
        System.out.println("Получено сообщение из чата " + message.getChat().getTitle() +": "+ message.getText());
        if (update.hasMessage()) {
//        if (update.hasMessage() & CheckMessage(message.getText(), quizMap.get(chatID).isQuizStarted)) {
            String command = message.getText();

            String chatName = String.valueOf(message.getChat().getTitle());
            String userName = String.valueOf(message.getFrom().getUserName());

            switch (command) {
                case "/reg_me", "/reg_me@ChatGamePidor_Bot" -> registerPlayer(chatID, userName, chatName);
                case "/stats", "/stats@ChatGamePidor_Bot" -> sendStats(chatID);
                case "/start", "/start@ChatGamePidor_Bot" -> startGame(chatID, chatName);
                case "/bot_info", "/bot_info@ChatGamePidor_Bot", "/help", "/help@ChatGamePidor_Bot" -> botInfo(chatID);
                case "/cocksize", "/cocksize@ChatGamePidor_Bot" -> cockSize(chatID, userName);
                case "/start_quiz", "/start_quiz@ChatGamePidor_Bot" -> startQuizGame(chatID);
                case "/stop_quiz", "/stop_quiz@ChatGamePidor_Bot" -> stopQuiz(chatID);
                default -> checkQuizAnswer(command, userName, chatName, chatID);
            }
        }
    }

    private void startQuizGame(Long chatID) {
        quizMap.put(chatID, new Quiz());
        quizMap.get(chatID).isQuizStarted = true;
        Thread thread = new Thread(() -> {
            do {
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

                if (quizMap.get(chatID).isQuizStarted)
                    sendMessage(chatID, "Правильный ответ: " + quizMap.get(chatID).currentAnswer);
            } while (quizMap.get(chatID).isQuizStarted);
        });
        thread.start();
    }

    private void stopQuiz (Long chatID) {
        quizMap.get(chatID).isQuizStarted = false;
        sendMessage(chatID, "Викторина завершена");
    }
    private void checkQuizAnswer(String answer, String userName, String chatName, Long chatID) {
        if (!quizMap.get(chatID).isQuizStarted)
            sendMessage(chatID, "Викторина не запущена");
        if (quizMap.get(chatID).currentAnswer.equalsIgnoreCase(answer)) {
            Integer points = quizMap.get(chatID).calculatePoints(answer.toLowerCase());
            quizMap.get(chatID).setScore(userName, points, chatID, chatName);
            quizMap.get(chatID).newQuestion();
            sendQuestion(chatID);
        }
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

    private void cockSize(Long chatId, String username) {
        int playerCockSize = getPlayerCockSize(username);
        if (playerCockSize != -1) {

            if (!sendImgMessage(chatId, phraseSelection(playerCockSize, username), playerCockSize))
                sendMessage(chatId, phraseSelection(playerCockSize, username));
            return;
        }

        // Если записи нет, генерируем случайный размер и сохраняем его
        int newRandomSize = getCockSize();

        setCockSizeWinner(username, newRandomSize);
        if (!sendImgMessage(chatId, phraseSelection(newRandomSize, username), newRandomSize))
            sendMessage(chatId, phraseSelection(newRandomSize, username));
    }

    private void registerPlayer(Long chatID, String username, String chatName) {
        String insertQuery = "INSERT INTO " + PIDOR_PLAYERS_TABLE + " (chat_id, user_name, chat_name) VALUES (?, ?, ?) ON CONFLICT (chat_id, user_name) DO NOTHING";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);

            preparedStatement.setLong(1, chatID);
            preparedStatement.setString(2, "@" + username);
            preparedStatement.setString(3, Objects.requireNonNullElse(chatName, "-"));
            preparedStatement.executeUpdate();

            sendMessage(chatID, "Игрок @" + username + " зарегистрирован! ");
        } catch (SQLException e) {
            logger.error("Ошибка при регистрации игрока в БД: " + e);
            sendMessage(chatID, "Произошла ошибка при регистрации игрока @" + username + "\n" + e.getMessage());
        }
    }

    private void sendStats(Long chatId) {
        Thread thread = new Thread(() -> {
            String query = "SELECT winner_user_name, COUNT(*) AS count " +
                    "FROM " + PIDOR_STATS_TABLE +
                    " WHERE chat_id = ? " +
                    "GROUP BY winner_user_name";

            Map<String, Integer> winnersCount = new HashMap<>();

            try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
                PreparedStatement preparedStatement = connection.prepareStatement(query);

                preparedStatement.setLong(1, chatId);
                ResultSet resultSet = preparedStatement.executeQuery();

                while (resultSet.next()) {
                    String winner = resultSet.getString("winner_user_name");
                    int count = resultSet.getInt("count");
                    winnersCount.put(winner, count);
                }

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

    private void botInfo(Long chatId) {
        sendMessage(chatId, """
                Этот бот создан для определения пидора дня в чате! Команды:
                /cocksize - проверь длинну своего члена
                /reg_me - добавляет пользователя в пидорвикторину
                /stats - статистика пидорвикторины за все время
                /start - запускает пидорвикторину""");
    }

    private void startGame(Long chatID, String chatName) {
        Thread thread = new Thread(() -> {
            Set<String> chatPlayers;
            String winner = getTodayWinner(chatID);

            if (winner != null) {
                sendMessage(chatID, "Сегодня пидора уже выбрали. Пидор дня: " + winner);
                return;
            }

            chatPlayers = getCockSizePlayers(chatID);
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

            winner = new ArrayList<>(chatPlayers).get(new Random().nextInt(chatPlayers.size()));
            setPidorWinner(chatID, winner, chatName);
            sendMessage(chatID, getWinnerResponce() + winner + "!");
        });
        thread.start();
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
        }
    }

    private boolean sendImgMessage (Long chatId, String text, Integer size) {
        //TODO: добавить отправку картинок
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
            System.err.println("Image file not found: " + imageFile.getPath());
            return false;
        }
        return false;
    }
}
