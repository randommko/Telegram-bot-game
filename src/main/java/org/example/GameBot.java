package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import static java.sql.DriverManager.getConnection;

public class GameBot extends TelegramLongPollingBot {

    private final String botToken;

    private DataSourceConfig dataSourceConfig;


    public GameBot(String botToken, String DB_SERVER_URL, String DB_USER, String DB_PASS) {
        this.botToken = botToken;
        dataSourceConfig = new DataSourceConfig(DB_SERVER_URL, DB_USER, DB_PASS);
    }
    private static final String RESPONSES_FILE = "src/main/resources/responses.json"; // Файл с фразами
    private final Map<Long, Set<String>> players = new HashMap<>(); // Участники по чатам

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

        if (text.equals("/start")) {
            return true;
        }

        if (text.equals("/stats")) {
            return true;
        }

        if (text.equals("/reg_me")) {
            return true;
        }

        return text.equals("/bot_info");
    }
    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        System.out.println("Получено сообщение из чата " + message.getChat().getTitle() +": "+ message.getText());
        if (update.hasMessage() & CheckMessage(message.getText())) {
            String command = message.getText();
            Long chatId = message.getChatId();

            switch (command) {
                case "/reg_me" -> registerPlayer(chatId, message.getFrom().getUserName());
                case "/stats" -> sendStats(chatId);
                case "/start" -> startGame(chatId);
                case "/bot_info" -> botInfo(chatId);
                default -> sendMessage(chatId, "Неизвестная команда.");
            }
        }
    }

    private void registerPlayer(Long chatId, String username) {
        String insertQuery = "INSERT INTO public.pidor_players (chat_id, user_name) VALUES (?, ?) ON CONFLICT (chat_id, user_name) DO NOTHING";
        try (Connection connection = dataSourceConfig.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);

            preparedStatement.setLong(1, chatId);
            preparedStatement.setString(2, "@" + username);
            preparedStatement.executeUpdate();
            //connection.commit(); // Фиксируем изменения

            sendMessage(chatId, "Игрок @" + username + " зарегистрирован!");
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(chatId, "Произошла ошибка при регистрации игрока @" + username);
        }

    }

    private void sendStats(Long chatId) {
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

    private void botInfo(Long chatId) {
        sendMessage(chatId, """
                Этот бот создан для определения пидора дня в чате! Команды:
                /reg_me - добаавляет пользователя в игру
                /stats - статистика за все время
                /start - запускает поиска пидора в чате""");
    }

    private void startGame(Long chatId) {

        LocalDate today = LocalDate.now();
        //        Set<String> chatPlayers = new HashSet<>();

        Set<String> chatPlayers;
        chatPlayers = new HashSet<>();

        try (Connection connection = dataSourceConfig.getDataSource().getConnection()) {
            // Проверяем, была ли уже игра сегодня
            String checkQuery = "SELECT winner_user_name FROM public.pidor_stats WHERE chat_id = ? AND date = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setLong(1, chatId);
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
            stmt.setLong(1, chatId);

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
                insertStmt.setLong(1, chatId);
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
        //TODO: вынести в отдельный класс с утилитами второстепенные функции
        try {
            // Читаем JSON из файла
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(Paths.get(RESPONSES_FILE).toFile());
            JsonNode firstMsg = root.get("firstMsg");
            JsonNode secondMsg = root.get("secondMsg");
            JsonNode thirdMsg = root.get("thirdMsg");
//            JsonNode winMsg = root.get("winMsg");

            // Список для хранения выбранных фраз
            List<String> responses = new ArrayList<>();

            int randomIndex;

            randomIndex = random.nextInt(firstMsg.size());
            responses.add(firstMsg.get(randomIndex).asText());

            randomIndex = random.nextInt(secondMsg.size());
            responses.add(secondMsg.get(randomIndex).asText());

            randomIndex = random.nextInt(thirdMsg.size());
            responses.add(thirdMsg.get(randomIndex).asText());

            return responses;

        } catch (IOException e) {
            return List.of("Подготовка...", "Скоро узнаем...", "Держитесь крепче...");
        }
    }

    private File getStatsFile(Long chatId) {
        return new File("stats_" + chatId + ".json");
    }

    private void sendMessage(Long chatId, String text) {
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
