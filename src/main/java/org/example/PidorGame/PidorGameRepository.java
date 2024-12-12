package org.example.PidorGame;

import org.example.DataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.example.TablesDB.*;

public class PidorGameRepository {
    private static final Logger logger = LoggerFactory.getLogger(PidorGameRepository.class);
    public void setPidorWinner(Long chatID, Long userID) {
        LocalDate today = LocalDate.now();
        String insertQuery = "INSERT INTO " + PIDOR_STATS_TABLE + " (chat_id, date, user_id) VALUES (?, ?, ?)";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setLong(1, chatID);
                insertStmt.setDate(2, Date.valueOf(today));
                insertStmt.setLong(3, userID);
                insertStmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Ошибка при приверке состоявшейся игры: ", e);
        }
    }

    public Set<Long> getPidorGamePlayers(Long chatID) {
        Set<Long> chatPlayers = new HashSet<>();
        try (Connection getPlayersConn = DataSourceConfig.getDataSource().getConnection()) {
            String query = "SELECT user_id FROM " + PIDOR_PLAYERS_TABLE + " WHERE chat_id = ?";
            PreparedStatement stmt = getPlayersConn.prepareStatement(query);
            stmt.setLong(1, chatID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    chatPlayers.add(rs.getLong("user_id"));
            }
        } catch (Exception e) {
            logger.error("Ошибка получения списка игроков из БД: ", e);
        }
        return chatPlayers;
    }

    public Long getTodayWinner(Long chatID) {
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            LocalDate today = LocalDate.now();

            String checkQuery = "SELECT user_id FROM " + PIDOR_STATS_TABLE + " WHERE chat_id = ? AND date = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setLong(1, chatID);
                checkStmt.setDate(2, Date.valueOf(today));
                try (ResultSet resultSet = checkStmt.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getLong("user_id");
                    }
                }
            } catch (Exception e) {
                logger.error("Ошибка при приверке состоявшейся игры: ", e);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при приверке состоявшейся игры: ", e);
        }
        return null;
    }

    public String getWinnerResponce() {
        String sql = "SELECT text FROM " + MESSAGES_TABLE + " WHERE group_num = 100 ORDER BY RANDOM() LIMIT 1";

        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("text");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при поулчении текста сообщений из БД: ", e);
            // В случае ошибки возвращаем предопределённые ответы
            return "Победитель сегодняшней игры: ";
        }
        return "Победитель сегодняшней игры: ";
    }

    public List<String> getRandomResponses() {
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
}
