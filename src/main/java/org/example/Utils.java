package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class Utils {
    public static final String MESSAGES_TABLE = "public_test.messages";
    public static final String PIDOR_PLAYERS_TABLE = "public_test.pidor_players";
    public static final String PIDOR_STATS_TABLE = "public_test.pidor_stats";
    public static final String COCKSIZE_STATS_TABLE = "public_test.cocksize_stats";
    public static final String COCKSIZE_IMAGES_TABLE = "public_test.cocksize_imgs";
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static boolean CheckMessage(String text) {

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

    public static String phraseSelection(int size, String username) {
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

    public static int getCockSize() {
        //TODO: Переписать выбор дллинны. Сделать через распределение.
        int randomNum = new Random().nextInt(100);        //Выбираем случайное число для попадания в распределение

        if (randomNum < 2)                                      //У 2% выборки
            return new Random().nextInt(3);              //длина от 0 до 3

        if (randomNum < 7)                                     //У 5% выборки
            return new Random().nextInt(5) + 3;           //длина от 3 до 8

        if (randomNum < 25)                                     //У 18% выборки
            return new Random().nextInt(7) + 8;           //длина от 8 до 15

        if (randomNum < 55)                                     //У 30% выборки
            return new Random().nextInt(5) + 15;          //длина от 15 до 20

        if (randomNum < 65)                                    //У 10% выборки
            return new Random().nextInt(5) + 20;        //длина от 20 до 25

        if (randomNum < 73)                                    //У 8% выборки
            return new Random().nextInt(5) + 25;        //длина от 25 до 30

        if (randomNum < 78)                                    //У 5% выборки
            return new Random().nextInt(3) + 25;        //длина от 25 до 28

        if (randomNum < 83)                                    //У 5% выборки
            return new Random().nextInt(3) + 28;        //длина от 25 до 31

        if (randomNum < 88)                                    //У 5% выборки
            return new Random().nextInt(3) + 31;        //длина от 31 до 34

        if (randomNum < 91)                                    //У 3% выборки
            return new Random().nextInt(3) + 34;        //длина от 34 до 37

        if (randomNum < 94)                                    //У 3% выборки
            return new Random().nextInt(3) + 37;        //длина от 37 до 40

        if (randomNum < 96)                                    //У 2% выборки
            return new Random().nextInt(3) + 40;        //длина от 40 до 43

        if (randomNum < 98)                                    //У 2% выборки
            return new Random().nextInt(5) + 40;        //длина от 40 до 45

        if (randomNum < 100)                                    //У 2% выборки
            return new Random().nextInt(5) + 45;        //длина от 45 до 50


        return -1;
    }

    public static List<String> getRandomResponses() {
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

    public static String getWinnerResponce() {
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
    public static String getTodayWinner(String chatId) {
        // Проверяем, была ли уже игра сегодня
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            LocalDate today = LocalDate.now();

            String checkQuery = "SELECT winner_user_name FROM " + PIDOR_STATS_TABLE + " WHERE chat_id = ? AND date = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, chatId);
                checkStmt.setDate(2, Date.valueOf(today));
                try (ResultSet resultSet = checkStmt.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getString("winner_user_name");
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

    public static Set<String> getCockSizePlayers(String chatId) {
        Set<String> chatPlayers = new HashSet<>();
        try (Connection getPlayersConn = DataSourceConfig.getDataSource().getConnection()) {
            // Запрос к базе данных для получения списка игроков по chat_id
            String query = "SELECT user_name FROM " + PIDOR_PLAYERS_TABLE + " WHERE chat_id = ?";
            PreparedStatement stmt = getPlayersConn.prepareStatement(query);
            stmt.setString(1, chatId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Добавляем имя игрока в список
                    chatPlayers.add(rs.getString("user_name"));
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка получения списка игроков из БД: ", e);
        }
        return chatPlayers;
    }

    public static int getPlayerCockSize(String username) {
        LocalDate currentDate = LocalDate.now();
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            // Проверяем, есть ли запись для текущей даты
            String checkQuery = "SELECT size FROM " + COCKSIZE_STATS_TABLE + " WHERE user_name = ? AND date = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, username);
                checkStmt.setDate(2, Date.valueOf(currentDate));
                ResultSet resultSet = checkStmt.executeQuery();
                resultSet.next();
                return resultSet.getInt("size");
            }
        } catch (Exception e) {
            logger.error("Ошибка при поиске в БД длинны члена: ", e);
        }
        return -1;
    }

    public static void setPidorWinner(String chatId, String winner, String chatName) {
        LocalDate today = LocalDate.now();
        String insertQuery = "INSERT INTO " + PIDOR_STATS_TABLE + " (chat_id, date, winner_user_name, chat_name) VALUES (?, ?, ?, ?)";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setString(1, chatId);
                insertStmt.setDate(2, Date.valueOf(today));
                insertStmt.setString(3, winner);
                insertStmt.setString(4, chatName);
                insertStmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Ошибка при приверке состоявшейся игры: ", e);
        }
    }

    public static void setCockSizeWinner (String username, Integer size) {
        LocalDate today = LocalDate.now();
        String insertQuery = "INSERT INTO " + COCKSIZE_STATS_TABLE + " (user_name, size, date) VALUES (?, ?, ?)";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setString(1, username);
                insertStmt.setInt(2, size);
                insertStmt.setDate(3, Date.valueOf(today));
                insertStmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Ошибка при записи в БД длинны члена: ", e);
        }
    }

    public static String getCockSizeImage(Integer size) {
        //Кодирование base64: https://base64.guru/converter/encode/image
        String selectQuery = "SELECT img FROM " + COCKSIZE_IMAGES_TABLE + " WHERE cock_size = ?";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(selectQuery)) {
                insertStmt.setInt(1, size);
                ResultSet resultImageSet = insertStmt.executeQuery();
                if (resultImageSet.next())
                    return resultImageSet.getString("img");
            }
        } catch (Exception e) {
            logger.error("Ошибка при записи в БД длинны члена: ", e);
        }
        return null;
    }
}
