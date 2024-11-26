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
    private final String MESSAGES_TABLE = "public.messages";
    private final String PIDOR_PLAYERS_TABLE = "public.pidor_players";
    private final String PIDOR_STATS_TABLE = "public.pidor_stats";
    private final String COCKSIZE_STATS_TABLE = "public.cocksize_stats";
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
        if (text.equals("/cocksize") || text.equals("/cocksize@ChatGamePidor_Bot")) {
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
                    //TODO: сохранять ИД пользователя, точную дату
                    String insertQuery = "INSERT INTO " + COCKSIZE_STATS_TABLE + " (user_name, size, date) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setString(1, username);
                        insertStmt.setInt(2, randomSize);
                        insertStmt.setDate(3, Date.valueOf(currentDate));
                        insertStmt.executeUpdate();
                    }
                    sendMessage(chatId, phraseSelection(randomSize, username));
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при записи в БД длинны члена: ", e);
        }
    }

    private String phraseSelection(int size, String username) {
        if (size >= 0 && size <= 5) {
            return "The legendary cocksize of @" + username + " is " + size + "cm\uD83D\uDC4E";
        } else if (size >= 6 && size <= 10) {
            return "The mighty cocksize of @" + username + " is " + size + "cm\uD83D\uDE22";
        } else if (size >= 11 && size <= 20) {
            return "The epic cocksize of @" + username + " is " + size + "cm\uD83D\uDE0D";
        } else if (size >= 21 && size <= 30) {
            return "The majestic cocksize of @" + username + " is " + size + "cm\uD83D\uDE0E";
        } else if (size >= 31 && size <= 40) {
            return "The legendary cocksize of @" + username + " is " + size + "cm\uD83E\uDD21";
        } else if (size >= 41 && size <= 50) {
            return "The mythical cocksize of @" + username + " is " + size + "cm\uD83D\uDD25";
        } else return "NO FUCKING WAY! Cocksize @" + username + " is " + size + "cm\uD83D\uDC80";
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

            sendMessage(chatId, "Игрок @" + username + " зарегистрирован!");
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

        Set<String> chatPlayers;
        chatPlayers = new HashSet<>();

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            // Проверяем, была ли уже игра сегодня
            String checkQuery = "SELECT winner_user_name FROM " + PIDOR_STATS_TABLE + " WHERE chat_id = ? AND date = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, chatId);
                checkStmt.setDate(2, Date.valueOf(today));
                try (ResultSet resultSet = checkStmt.executeQuery()) {
                    if (resultSet.next()) {
                        String winner = resultSet.getString("winner_user_name");
                        //TODO: поправить сообщение, брать из БД
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
            String query = "SELECT user_name FROM " + PIDOR_PLAYERS_TABLE + " WHERE chat_id = ?";

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
            String insertQuery = "INSERT INTO " + PIDOR_STATS_TABLE + " (chat_id, date, winner_user_name, chat_name) VALUES (?, ?, ?, ?)";
            //TODO: сохранять ИД участника, название чата, точное время розыгрыша
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setString(1, chatId);
                insertStmt.setDate(2, Date.valueOf(today));
                insertStmt.setString(3, winner);
                insertStmt.setString(4, chatName);
                insertStmt.executeUpdate();
            }

            // Сообщаем о победителе
            //TODO: поправить сообщение, брать из БД
            sendMessage(chatId, "Победитель сегодняшней игры: " + winner + "!");
        } catch (Exception e) {
            logger.error("Произошла ошибка при сохранении победителя в БД: ", e);
            sendMessage(chatId, "Произошла ошибка при запуске игры.");
        }
    }

    private List<String> getRandomResponses() {
        // SQL запрос для выборки случайного текста из группы
        String sql = "SELECT text FROM " + MESSAGES_TABLE + " WHERE group_num = ? ORDER BY RANDOM() LIMIT 1";

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
