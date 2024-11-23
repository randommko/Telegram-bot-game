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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.Random;

public class GameBot extends TelegramLongPollingBot {

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
        return "7332966399:AAFegCTK2sv6sw3KOrEuEvHXU2Lsx55tFoY"; // Замените на токен вашего бота
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
        players.computeIfAbsent(chatId, k -> new HashSet<>()).add(username);
        sendMessage(chatId, "Игрок @" + username + " зарегистрирован!");
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
        File statsFile = getStatsFile(chatId);
        LocalDate today = LocalDate.now();

        try {
            ArrayNode stats = statsFile.exists()
                    ? (ArrayNode) objectMapper.readTree(statsFile)
                    : objectMapper.createArrayNode();

            for (var stat : stats) {
                if (stat.get("дата").asText().equals(today.toString())) {
                    sendMessage(chatId, "Сегодня игра уже состоялась. Победитель: " + stat.get("победитель").asText());
                    return;
                }
            }

            Set<String> chatPlayers = players.get(chatId);
            if (chatPlayers == null || chatPlayers.isEmpty()) {
                sendMessage(chatId, "Нет зарегистрированных игроков.");
                return;
            }

            List<String> responses = getRandomResponses();
            for (String response : responses) {
                sendMessage(chatId, response);
                Thread.sleep(1000);
            }

            String winner = "@" + new ArrayList<>(chatPlayers).get(new Random().nextInt(chatPlayers.size()));
            ObjectNode newStat = objectMapper.createObjectNode();
            newStat.put("дата", today.toString());
            newStat.put("победитель", winner);

            stats.add(newStat);
            objectMapper.writeValue(statsFile, stats);

            sendMessage(chatId, "Победитель сегодняшней игры: " + winner + "!");
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка при запуске игры.");
        }
    }

    private List<String> getRandomResponses() {
        //TODO: вынести в отдельный класс с утилитами второстепенные функции
        try {
//            ArrayNode responses = (ArrayNode) objectMapper.readTree(new File(RESPONSES_FILE));
//            List<String> randomResponses = new ArrayList<>();
//            for (int i = 0; i < 3; i++) {
//                randomResponses.add(responses.get(new Random().nextInt(responses.size())).asText());
//            }
//            return randomResponses;

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
