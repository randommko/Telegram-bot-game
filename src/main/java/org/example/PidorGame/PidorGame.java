package org.example.PidorGame;

import org.example.TelegramBot;
import org.example.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.example.Emodji.*;

public class PidorGame {
        private final TelegramBot bot;
        private static final Logger logger = LoggerFactory.getLogger(PidorGame.class);
        private final PidorGameRepository repo = new PidorGameRepository();
        private final UsersService usersService = new UsersService();

        public PidorGame() {
                bot = TelegramBot.getInstance();
        }
        public void registerPlayer(Long chatID, Long userID) {
                String userName = usersService.getUserNameByID(userID);
                Integer numOfChanges = repo.registerPlayer(chatID, userID);
                if (numOfChanges != 0)
                        bot.sendMessage(chatID, "Игрок " + userName + " зарегистрирован!");
                else
                        bot.sendMessage(chatID, "Игрок " + userName + " был зарегистрирован ранее ");
        }
        public void sendPidorStats(Long chatID) {
//                Thread thread = new Thread(() -> {
//                        Map<String, Integer> winnersList = repo.getPidorStats(chatID);
//                        if (winnersList.isEmpty()) {
//                                bot.sendMessage(chatID, "Статистика пуста.");
//                                return;
//                        }
//                        StringBuilder statsMessage = new StringBuilder("Статистика пидоров:\n");
//                        winnersList.forEach((winner, count) ->
//                                statsMessage.append(winner).append(": ").append(count).append("\n")
//                        );
//                        bot.sendMessage(chatID, statsMessage.toString());
//                });
//                thread.start();

                Thread thread = new Thread(() -> {
                        Map<String, Integer> stats = repo.getPidorStats(chatID);

                        // Преобразуем Map в List<Entry> и сортируем по убыванию значений
                        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(stats.entrySet());
                        sortedList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

                        // Если нужно, можно вернуть отсортированную карту
                        Map<String, Integer> sortedMap = new LinkedHashMap<>();
                        for (Map.Entry<String, Integer> entry : sortedList) {
                                sortedMap.put(entry.getKey(), entry.getValue());
                        }


                        StringBuilder statsMessage = new StringBuilder("Статистика викторины:\n");
                        sortedMap.forEach((userName, score) ->
                                statsMessage.append(userName.startsWith("@") ? userName.substring(1) : userName)
                                        .append(": ").append(score).append(" очков\n")
                        );
                        bot.sendMessage(chatID, statsMessage.toString());
                });
                thread.start();

        }
        public void startPidorGame(Long chatID) {
                Long winnerID = repo.getTodayWinner(chatID);
                if (winnerID != null) {
                        bot.sendMessage(chatID, RAINBOW_FLAG_EMODJI + " Сегодня пидора уже выбрали. Пидор дня: " + usersService.getUserNameByID(winnerID));
                        return;
                }
                Set<Long> chatPlayers = repo.getPidorGamePlayers(chatID);
                if (chatPlayers.isEmpty()) {
                        bot.sendMessage(chatID, "Нет зарегистрированных игроков.");
                        return;
                }
                List<String> responses = repo.getRandomResponses();
                try {
                        for (String response : responses) {
                                bot.sendMessage(chatID, response);
                                Thread.sleep(1000);
                        }
                } catch (Exception e) {
                        bot.sendMessage(chatID, "Ищем пидора запасным вариантом...");
                        logger.error("Произошла ошибка при получении из БД списка соощбений: ", e);
                }
                winnerID = new ArrayList<>(chatPlayers).get(new Random().nextInt(chatPlayers.size()));
                repo.setPidorWinner(chatID, winnerID);
                bot.sendMessage(chatID, RAINBOW_FLAG_EMODJI + " " + repo.getWinnerResponce() + usersService.getUserNameByID(winnerID) + "!");
        }
}
