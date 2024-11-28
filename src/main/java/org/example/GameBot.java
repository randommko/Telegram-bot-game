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
    private static final String RESOURCES_PATH = "/bin/tg_bot/resources";
    private static final Logger logger = LoggerFactory.getLogger(GameBot.class);

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
        System.out.println("Получено сообщение из чата " + message.getChat().getTitle() +": "+ message.getText());
        if (update.hasMessage() & CheckMessage(message.getText())) {
            String command = message.getText();
            String chatId = String.valueOf(message.getChatId());
            String chatName = String.valueOf(message.getChat().getTitle());
            String userName = String.valueOf(message.getFrom().getUserName());

            switch (command) {
                case "/reg_me", "/reg_me@ChatGamePidor_Bot" -> registerPlayer(chatId, userName, chatName);
                case "/stats", "/stats@ChatGamePidor_Bot" -> sendStats(chatId);
                case "/start", "/start@ChatGamePidor_Bot" -> startGame(chatId, chatName);
                case "/bot_info", "/bot_info@ChatGamePidor_Bot", "/help", "/help@ChatGamePidor_Bot" -> botInfo(chatId);
                case "/cocksize", "/cocksize@ChatGamePidor_Bot" -> cockSize(chatId, userName);
                default -> sendMessage(chatId, "Неизвестная команда.");
            }
        }
    }



    private void cockSize(String chatId, String username) {
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

    private void registerPlayer(String chatId, String username, String chatName) {
        String insertQuery = "INSERT INTO " + PIDOR_PLAYERS_TABLE + " (chat_id, user_name, chat_name) VALUES (?, ?, ?) ON CONFLICT (chat_id, user_name) DO NOTHING";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);

            preparedStatement.setString(1, chatId);
            preparedStatement.setString(2, "@" + username);
            preparedStatement.setString(3, Objects.requireNonNullElse(chatName, "-"));
            preparedStatement.executeUpdate();

            sendMessage(chatId, "Игрок @" + username + " зарегистрирован! ");
        } catch (SQLException e) {
            logger.error("Ошибка при регистрации игрока в БД: " + e);
            sendMessage(chatId, "Произошла ошибка при регистрации игрока @" + username + "\n" + e.getMessage());
        }
    }

    private void sendStats(String chatId) {
        String query = "SELECT winner_user_name, COUNT(*) AS count " +
                "FROM " + PIDOR_STATS_TABLE +
                " WHERE chat_id = ? " +
                "GROUP BY winner_user_name";

        Map<String, Integer> winnersCount = new HashMap<>();

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
             PreparedStatement preparedStatement = connection.prepareStatement(query);

            preparedStatement.setString(1, chatId);
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
    }

    private void botInfo(String chatId) {
        sendMessage(chatId, """
                Этот бот создан для определения пидора дня в чате! Команды:
                /cocksize - проверь длинну своего члена
                /reg_me - добавляет пользователя в пидорвикторину
                /stats - статистика пидорвикторины за все время
                /start - запускает пидорвикторину""");
    }

    private void startGame(String chatId, String chatName) {
        Set<String> chatPlayers;
        String winner = getTodayWinner(chatId);

        if (winner != null) {
            sendMessage(chatId, "Сегодня пидора уже выбрали. Пидор дня: " + winner);
            return;
        }

        chatPlayers = getCockSizePlayers(chatId);

        // Проверяем, есть ли зарегистрированные игроки
        if (chatPlayers.isEmpty()) {
            sendMessage(chatId, "Нет зарегистрированных игроков.");
            return;
        }

        // Отправляем случайные ответы
        List<String> responses = getRandomResponses();
        try {
            for (String response : responses) {
                sendMessage(chatId, response);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при сохранении победителя в БД: ", e);
        }

        // Определяем победителя
        winner = new ArrayList<>(chatPlayers).get(new Random().nextInt(chatPlayers.size()));
        // Сохраняем результат в БД
        setPidorWinner(chatId, winner, chatName);

        // Сообщаем о победителе
        //TODO: поправить сообщение, брать из БД
        sendMessage(chatId, getWinnerResponce() + winner + "!");
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
        }
    }

    private boolean sendImgMessage (String chatId, String text, Integer size) {
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
