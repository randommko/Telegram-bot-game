package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;


import java.io.ByteArrayInputStream;
import java.sql.*;
import java.sql.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.Random;
import java.util.Base64;

import static org.example.Utils.*;

public class GameBot extends TelegramLongPollingBot {
    private final String botToken;
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
        LocalDate currentDate = LocalDate.now();

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            // Проверяем, есть ли запись для текущей даты
            String checkQuery = "SELECT size FROM " + COCKSIZE_STATS_TABLE + " WHERE user_name = ? AND date = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, username);
                checkStmt.setDate(2, Date.valueOf(currentDate));
                ResultSet resultSet = checkStmt.executeQuery();

                if (resultSet.next()) {
                    // Если запись найдена, возвращаем существующее значение
                    int size = resultSet.getInt("size");
                    sendMessage(chatId, phraseSelection(size, username));
                } else {
                    // Если записи нет, генерируем случайный размер и сохраняем его
                    int randomSize = new Random().nextInt(50); // Генерация числа от 0 до 49
                    if (username.equals("vajnaya_sobaka") || username.equals("@vajnaya_sobaka"))
                        randomSize = 18;
                    //TODO: сохранять ИД пользователя, точную дату
                    String insertQuery = "INSERT INTO " + COCKSIZE_STATS_TABLE + " (user_name, size, date) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setString(1, username);
                        insertStmt.setInt(2, randomSize);
                        insertStmt.setDate(3, Date.valueOf(currentDate));
                        insertStmt.executeUpdate();
                    }
                    //TODO: добавить отправку картинок
                    //Кодирование base64: https://base64.guru/converter/encode/image
                    String sizeImgQuery = "SELECT img FROM " + COCKSIZE_IMAGES_TABLE + " WHERE cock_size = ?";
                    PreparedStatement imageStmt = connection.prepareStatement(sizeImgQuery);
                    imageStmt.setInt(1, randomSize);
                    ResultSet resultImageSet = imageStmt.executeQuery();
                    if (resultImageSet.next()) {
                        // Декодируем Base64 в массив байтов
                        String base64String = String.valueOf(resultImageSet);
                        if (base64String.contains(",")) {
                            base64String = base64String.split(",")[1];
                            base64String = base64String.replaceAll("\\s", "");
                        }
                        byte[] imageBytes = Base64.getDecoder().decode(base64String);
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

                        SendPhoto sendPhoto = new SendPhoto();
                        sendPhoto.setChatId(chatId); // ID чата или пользователя
                        sendPhoto.setPhoto(new InputFile(inputStream, "image.png")); // Путь к изображению
                        sendPhoto.setCaption(phraseSelection(randomSize, username)); // Текстовое сообщение

                        try {
                            execute(sendPhoto); // Отправка сообщения
                            System.out.println("Сообщение отправлено успешно!");
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                            System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
                        }
                    } else {
                        sendMessage(chatId, phraseSelection(randomSize, username));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при записи в БД длинны члена: ", e);
        }
        catch (Exception e) {
            logger.error("Ошибка: ", e);
            System.out.println("Ошибка: " + e);
        }
    }

    private void registerPlayer(String chatId, String username, String chatName) {
        String insertQuery = "INSERT INTO " + PIDOR_PLAYERS_TABLE + " (chat_id, user_name, chat_name) VALUES (?, ?, ?) ON CONFLICT (chat_id, user_name) DO NOTHING";
        //TODO: Сохранять название чата, ид пользователя
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);

            preparedStatement.setString(1, chatId);
            preparedStatement.setString(2, "@" + username);
            if (chatName != null)
                preparedStatement.setString(3, chatName);
            else preparedStatement.setString(3, "-");
            preparedStatement.executeUpdate();

            sendMessage(chatId, "Игрок @" + username + " зарегистрирован! ");
        } catch (SQLException e) {
            logger.error("Ошибка при регистрации игрока в БД: ", e.toString());
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
                /reg_me - добаавляет пользователя в игру
                /stats - статистика за все время
                /start - запускает поиска пидора в чате""");
    }

    private void startGame(String chatId, String chatName) {
        LocalDate today = LocalDate.now();
        Set<String> chatPlayers = new HashSet<>();
        String winner = getTodayWinner(chatId);

        if (winner != null) {
            sendMessage(chatId, "Сегодня игра уже состоялась. Пидор дня: " + winner);
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
        setCockSizeWinner(chatId, winner, chatName);

        // Сообщаем о победителе
        //TODO: поправить сообщение, брать из БД
        sendMessage(chatId, "Победитель сегодняшней игры: " + winner + "!");
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
}
