package org.example.pidorGame;

import org.example.DataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.example.GameBot.sendMessage;
import static org.example.TablesDB.*;
import static org.example.quizGame.QuizRepository.getUserNameByID;

public class PidorGameService {
        private static final Logger logger = LoggerFactory.getLogger(PidorGameService.class);
        private final PidorGameRepository repo = new PidorGameRepository();
        public void registerPlayer(Long chatID, Long userID) {
                String userName = getUserNameByID(userID);

                String insertQuery = "INSERT INTO " + PIDOR_PLAYERS_TABLE + " (chat_id, user_id) VALUES (?, ?) ON CONFLICT (chat_id, user_id) DO NOTHING";
                try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
                        PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);

                        preparedStatement.setLong(1, chatID);
                        preparedStatement.setLong(2, userID);
                        preparedStatement.executeUpdate();

                        sendMessage(chatID, "Игрок @" + userName + " зарегистрирован! ");
                } catch (SQLException e) {
                        logger.error("Ошибка при регистрации игрока в БД: " + e);
                        sendMessage(chatID, "Произошла ошибка при регистрации игрока @" + userName + "\n" + e.getMessage());
                }
        }
        public void sendPidorStats(Long chatID) {
                Thread thread = new Thread(() -> {
                        String query = "SELECT pst.user_id, COUNT(*) AS count " +
                                "FROM " + PIDOR_STATS_TABLE +
                                " AS pst JOIN " + TG_USERS_TABLE +
                                " AS tut ON pst.user_id = tut.user_id " +
                                "WHERE pst.chat_id = ? " +
                                "GROUP BY pst.user_id";

                        Map<String, Integer> winnersList = new HashMap<>();

                        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
                                PreparedStatement preparedStatement = connection.prepareStatement(query);
                                preparedStatement.setLong(1, chatID);
                                ResultSet resultSet = preparedStatement.executeQuery();

                                while (resultSet.next())
                                        winnersList.put(
                                                getUserNameByID(resultSet.getLong("user_id")),
                                                resultSet.getInt("count")
                                        );

                                if (winnersList.isEmpty()) {
                                        sendMessage(chatID, "Статистика пуста.");
                                        return;
                                }

                                StringBuilder statsMessage = new StringBuilder("Статистика пидоров:\n");
                                winnersList.forEach((winner, count) ->
                                        statsMessage.append(winner).append(": ").append(count).append("\n")
                                );

                                sendMessage(chatID, statsMessage.toString());

                        } catch (SQLException e) {
                                logger.error("Ошибка при получении статистики из БД: ", e);
                                sendMessage(chatID, "Ошибка при получении статистики.");
                        }
                });
                thread.start();
        }
        public void startPidorGame(Long chatID) {
                //TODO: если отправить две команды подряд то будет выбрано два победителя. Блокировать поиск нового пока не завершился первый
                Thread thread = new Thread(() -> {
                        Long winnerID = repo.getTodayWinner(chatID);

                        if (winnerID != null) {
                                sendMessage(chatID, "Сегодня пидора уже выбрали. Пидор дня: " + getUserNameByID(winnerID));
                                return;
                        }

                        Set<Long> chatPlayers = repo.getPidorGamePlayers(chatID);
                        if (chatPlayers.isEmpty()) {
                                sendMessage(chatID, "Нет зарегистрированных игроков.");
                                return;
                        }

                        List<String> responses = repo.getRandomResponses();
                        try {
                                for (String response : responses) {
                                        sendMessage(chatID, response);
                                        Thread.sleep(1000);
                                }
                        } catch (Exception e) {
                                sendMessage(chatID, "Ищем пидора запасным вариантом...");
                                logger.error("Произошла ошибка при получении из БД списка соощбений: ", e);
                        }

                        winnerID = new ArrayList<>(chatPlayers).get(new Random().nextInt(chatPlayers.size()));
                        repo.setPidorWinner(chatID, winnerID);
                        sendMessage(chatID, repo.getWinnerResponce() + getUserNameByID(winnerID) + "!");
                });
                thread.start();
        }
}
