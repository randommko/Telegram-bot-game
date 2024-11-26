package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.*;
import java.sql.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.Random;

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

    private boolean CheckMessage(String text) {

        if (text.equals("/start") || text.equals("/start@ChatGamePidor_Bot")) {
            return true;
        }

        if (text.equals("/stats") || text.equals("/stats@ChatGamePidor_Bot")) {
            return true;
        }

        if (text.equals("/reg_me") || text.equals("/reg_me@ChatGamePidor_Bot")) {
            return true;
        }

        return (text.equals("/bot_info") || text.equals("/bot_info@ChatGamePidor_Bot"));
    }
    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        System.out.println("Получено сообщение из чата " + message.getChat().getTitle() +": "+ message.getText());
        if (update.hasMessage() & CheckMessage(message.getText())) {
            String command = message.getText();
            String chatId = String.valueOf(message.getChatId());

            switch (command) {
                case "/reg_me", "/reg_me@ChatGamePidor_Bot" -> registerPlayer(chatId, message.getFrom().getUserName());
                case "/stats", "/stats@ChatGamePidor_Bot" -> sendStats(chatId);
                case "/start", "/start@ChatGamePidor_Bot" -> startGame(chatId);
                case "/bot_info", "/bot_info@ChatGamePidor_Bot", "/help", "/help@ChatGamePidor_Bot" -> botInfo(chatId);
                default -> sendMessage(chatId, "Неизвестная команда.");
            }
        }
    }

    private void registerPlayer(String chatId, String username) {
        String insertQuery = "INSERT INTO public.pi dor_players (chat_id, user_name) VALUES (?, ?) ON CONFLICT (chat_id, user_name) DO NOTHING";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);

            preparedStatement.setString(1, chatId);
            preparedStatement.setString(2, "@" + username);
            preparedStatement.executeUpdate();

            sendMessage(chatId, "Игрок @" + username + " зарегистрирован!");
        } catch (SQLException e) {
            logger.error("Ошибка при регистрации игрока в БД: ", e);
            sendMessage(chatId, "Произошла ошибка при регистрации игрока @" + username);
        }

    }

    private void sendStats(String chatId) {
        String query = "SELECT winner_user_name, COUNT(*) AS count " +
                "FROM public.pi dor_stats " +
                "WHERE chat_id = ? " +
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

    private void startGame(String chatId) {

        LocalDate today = LocalDate.now();

        Set<String> chatPlayers;
        chatPlayers = new HashSet<>();

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            // Проверяем, была ли уже игра сегодня
            String checkQuery = "SELECT winner_user_name FROM public.pidor_stats WHERE chat_id = ? AND date = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, chatId);
                checkStmt.setDate(2, Date.valueOf(today));
                try (ResultSet resultSet = checkStmt.executeQuery()) {
                    if (resultSet.next()) {
                        String winner = resultSet.getString("winner_user_name");
                        sendMessage(chatId, "Сегодня игра уже состоялась. Пидор дня: " + winner);
                        return;
                    }
                }
            } catch (Exception e) {
                logger.error("Ошибка при приверке состоявшейся игры: ", e);
                sendMessage(chatId, "Произошла ошибка при запуске игры.");
            }

        // Проверяем наличие зарегистрированных игроков

        try (Connection getPlayersConn = DataSourceConfig.getDataSource().getConnection()) {
            // Запрос к базе данных для получения списка игроков по chat_id
            String query = "SELECT user_name FROM public.pidor_players WHERE chat_id = ?";

            PreparedStatement stmt = getPlayersConn.prepareStatement(query);

            // Устанавливаем параметр для chat_id
            stmt.setString(1, chatId);

            // Выполняем запрос
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Добавляем имя игрока в список
                    chatPlayers.add(rs.getString("user_name"));
                }
            }

            } catch (Exception e) {
                logger.error("Ошибка получения списка игроков из БД: ", e);
                sendMessage(chatId, "Произошла ошибка при запуске игры.");
            }

            // Проверяем, есть ли зарегистрированные игроки
            if (chatPlayers.isEmpty()) {
                sendMessage(chatId, "Нет зарегистрированных игроков.");
                return;
            }

            // Отправляем случайные ответы
            List<String> responses = getRandomResponses();
            for (String response : responses) {
                sendMessage(chatId, response);
                Thread.sleep(1000);
            }

            // Определяем победителя
            String winner = new ArrayList<>(chatPlayers).get(new Random().nextInt(chatPlayers.size()));

            // Сохраняем результат в БД
            String insertQuery = "INSERT INTO public.pidor_stats (chat_id, date, winner_user_name) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setString(1, chatId);
                insertStmt.setDate(2, Date.valueOf(today));
                insertStmt.setString(3, winner);
                insertStmt.executeUpdate();
            }

            // Сообщаем о победителе
            sendMessage(chatId, "Победитель сегодняшней игры: " + winner + "!");
        } catch (Exception e) {
            logger.error("Произошла ошибка при сохранении победителя в БД: ", e);
            sendMessage(chatId, "Произошла ошибка при запуске игры.");
        }
    }

    private List<String> getRandomResponses() {
        // SQL запрос для выборки случайного текста из группы
        String sql = "SELECT text FROM public.messages WHERE group_num = ? ORDER BY RANDOM() LIMIT 1";

        List<String> responses = new ArrayList<>();

        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            for (int groupNum = 1; groupNum <= 3; groupNum++) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, groupNum);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            responses.add(rs.getString("text"));
                        } else {
                            // Если в группе нет записей, добавляем запасной текст
                            responses.add("Запасной текст для группы " + groupNum);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при поулчении текста сообщений из БД: ", e);
            // В случае ошибки возвращаем предопределённые ответы
            return List.of("Подготовка...", "Скоро узнаем...", "Держитесь крепче...");
        }

        return responses;
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
