package org.example.PidorGame;

import com.vdurmont.emoji.EmojiParser;
import org.example.TelegramBot;
import org.example.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PidorGame {
        private final TelegramBot bot;
        private static final Logger logger = LoggerFactory.getLogger(PidorGame.class);
        private final PidorGameRepository repo = new PidorGameRepository();
        private final UsersService usersService = new UsersService();
        public ExecutorService executor = Executors.newSingleThreadExecutor();

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
                //String - имя пользователя, Integer - количество побед
                Map<String, Integer> stats = repo.getPidorStats(chatID);

                // Преобразуем Map в List<Entry> и сортируем по убыванию значений
                List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(stats.entrySet());
                sortedList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

                // Если нужно, можно вернуть отсортированную карту
                Map<String, Integer> sortedMap = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> entry : sortedList) {
                        sortedMap.put(entry.getKey(), entry.getValue());
                }
                StringBuilder statsMessage = new StringBuilder(EmojiParser.parseToUnicode(":rainbow_flag: Статистика викторины:\n"));
                sortedMap.forEach((userName, score) ->
                                statsMessage.append(userName.startsWith("@") ? EmojiParser.parseToUnicode(":rainbow_flag:") + userName.substring(1) : EmojiParser.parseToUnicode(":rainbow_flag:") + userName)
                                .append(": ").append(score).append(" побед\n")
                );
                bot.sendMessage(chatID, statsMessage.toString());
        }
        public void startPidorGame(Long chatID, Long userID) {
                logger.debug("Запущена игра пидорвикторина в чате: " + chatID);
                if (!repo.getPidorGamePlayers(chatID).contains(userID)) {
                        bot.sendMessage(chatID, "Игру может начать только зарегистрированный игрок. Зарегистрируйтесь командой /pidor_reg");
                        return;
                }

                Long winnerID = repo.getTodayWinner(chatID);
                if (winnerID != null) {
                        bot.sendMessage(chatID,
                                EmojiParser.parseToUnicode((":rainbow_flag: Сегодня пидора уже выбрали. Пидор дня: " + usersService.getUserNameByID(winnerID))));
                        return;
                }
                Set<Long> chatPlayers = repo.getPidorGamePlayers(chatID);
                logger.debug("Количество игроков: " + chatPlayers.size());
                if (chatPlayers.isEmpty()) {
                        bot.sendMessage(chatID, "Нет зарегистрированных игроков.");
                        return;
                }

                if (chatPlayers.size() < 2) {
                        bot.sendMessage(chatID, "Для игры необходимо хотя бы два игрока. Зарегистрируйтесь командой /pidor_reg");
                        return;
                }

                List<String> responses = repo.getRandomResponses();
                try {
                        for (String response : responses) {
                                bot.sendMessage(chatID, response);
                                Thread.sleep(2000);
                        }
                } catch (Exception e) {
                        bot.sendMessage(chatID, "Ищем пидора запасным вариантом...");
                        logger.error("Произошла ошибка при получении из БД списка соощбений: ", e);
                }
                winnerID = new ArrayList<>(chatPlayers).get(new Random().nextInt(chatPlayers.size()));
                repo.setPidorWinner(chatID, winnerID);
                bot.sendMessage(chatID, EmojiParser.parseToUnicode(":rainbow_flag:") + " " + repo.getWinnerResponce() + usersService.getUserNameByID(winnerID) + "!");
        }
}
