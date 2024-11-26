package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.*;
import java.sql.Date;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.Random;

public class GameBot extends TelegramLongPollingBot {

    private final String botToken;

    private final DataSourceConfig dataSourceConfig;


    public GameBot(String botToken, DataSourceConfig dataSourceConfig) {
        this.botToken = botToken;
        this.dataSourceConfig = dataSourceConfig;
    }
    private static final String RESPONSES_FILE = "src/main/resources/responses.json"; // Файл с фразами
    //private final Map<Long, Set<String>> players = new HashMap<>(); // Участники по чатам

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Random random = new Random();


    @Override
    public String getBotUsername() {
        return "Pidor Bot Game"; // Замените на имя вашего бота
    }

    @Override
    public String getBotToken() {
        //TODO: сделать токен бота параметром при запуске
        return botToken; // Замените на токен вашего бота
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
                case "/reg_me" -> registerPlayer(chatId, message.getFrom().getUserName());
                case "/reg_me@ChatGamePidor_Bot" -> registerPlayer(chatId, message.getFrom().getUserName());
                case "/stats" -> sendStats(chatId);
                case "/stats@ChatGamePidor_Bot" -> sendStats(chatId);
                case "/start" -> startGame(chatId);
                case "/start@ChatGamePidor_Bot" -> startGame(chatId);
                case "/bot_info" -> botInfo(chatId);
                case "/bot_info@ChatGamePidor_Bot" -> botInfo(chatId);
                default -> sendMessage(chatId, "Неизвестная команда.");
            }
        }
    }

    private void registerPlayer(String chatId, String username) {
        String insertQuery = "INSERT INTO public.pidor_players (chat_id, user_name) VALUES (?, ?) ON CONFLICT (chat_id, user_name) DO NOTHING";
        try (Connection connection = dataSourceConfig.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);

            preparedStatement.setString(1, chatId);
            preparedStatement.setString(2, "@" + username);
            preparedStatement.executeUpdate();
            //connection.commit(); // Фиксируем изменения

            sendMessage(chatId, "Игрок @" + username + " зарегистрирован!");
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(chatId, "Произошла ошибка при регистрации игрока @" + username);
        }

    }

    private void sendStats(String chatId) {
        File file = getStatsFile(chatId);
        if (!file.exists()) {
            sendMessage(chatId, "Статистика пуста.");
            return;
        }

        try {
            ArrayNode stats = (ArrayNode) objectMapper.readTree(file);
            StringBuilder statsMessage = new StringBuilder("Статистика:\n");
            Map<String, Integer> winnersCount = new HashMap<>();

            for (var stat : stats) {
                String winner = "@"+stat.get("победитель").asText();
                winnersCount.put(winner, winnersCount.getOrDefault(winner, 0) + 1);
            }

            winnersCount.forEach((winner, count) ->
                    statsMessage.append(winner).append(": ").append(count).append("\n")
            );

            sendMessage(chatId, statsMessage.toString());
        } catch (IOException e) {
            sendMessage(chatId, "Ошибка при чтении статистики.");
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

        try (Connection connection = dataSourceConfig.getDataSource().getConnection()) {
            // Проверяем, была ли уже игра сегодня
            String checkQuery = "SELECT winner_user_name FROM public.pidor_stats WHERE chat_id = ? AND date = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, chatId);
                checkStmt.setDate(2, Date.valueOf(today));
                try (ResultSet resultSet = checkStmt.executeQuery()) {
                    if (resultSet.next()) {
                        String winner = resultSet.getString("winner_user_name");
                        sendMessage(chatId, "Сегодня игра уже состоялась. Победитель: " + winner);
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(chatId, "Произошла ошибка при запуске игры.");
            }

        // Проверяем наличие зарегистрированных игроков

        try (Connection getPlayersConn = dataSourceConfig.getDataSource().getConnection()) {
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
                e.printStackTrace();
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
                //connection.commit(); // Фиксируем изменения
            }

            // Сообщаем о победителе
            sendMessage(chatId, "Победитель сегодняшней игры: " + winner + "!");
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "Произошла ошибка при запуске игры.");
        }
    }

    private List<String> getRandomResponses() {
        //TODO: перенести на postgress
        // SQL запрос для выборки случайного текста из группы
        String sql = "SELECT text FROM public.messages WHERE group_num = ? ORDER BY RANDOM() LIMIT 1";

        List<String> responses = new ArrayList<>();
        //        Random random = new Random();

        try (Connection conn = dataSourceConfig.getDataSource().getConnection()) {
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
            // В случае ошибки возвращаем предопределённые ответы
            return List.of("Подготовка...", "Скоро узнаем...", "Держитесь крепче...");
        }

        return responses;
    }

    private File getStatsFile(String chatId) {
        return new File("stats_" + chatId + ".json");
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
