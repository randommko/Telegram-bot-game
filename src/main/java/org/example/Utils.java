package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static org.example.TablesDB.*;

public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

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
    public static Long getTodayWinner(Long chatID) {
        // Проверяем, была ли уже игра сегодня
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

    public static Set<Long> getPidorGamePlayers(Long chatID) {
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

    public static int getPlayerCockSize(Long userID) {
        LocalDate currentDate = LocalDate.now();
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            // Проверяем, есть ли запись для текущей даты
            String checkQuery = "SELECT size FROM " + COCKSIZE_STATS_TABLE + " WHERE user_id = ? AND date = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setLong(1, userID);
                checkStmt.setDate(2, Date.valueOf(currentDate));
                ResultSet resultSet = checkStmt.executeQuery();
                resultSet.next();
                return resultSet.getInt("size");
            }
        } catch (Exception e) {
            logger.error("Ошибка при поиске в БД длинны члена: ", e);
            return -1;
        }
    }

    public static Map<Long, String> initUsers() {
        String checkUserQuery = "SELECT user_id, user_name FROM " + TG_USERS_TABLE;
        Map<Long, String> result = new HashMap<>();
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement checkUser = connection.prepareStatement(checkUserQuery)) {
                ResultSet resultUserSet = checkUser.executeQuery();
                while (resultUserSet.next()) {
                    result.put(resultUserSet.getLong("user_id"), resultUserSet.getString("user_name"));
                }
                return result;
            }
        }
        catch (Exception e) {
            logger.error("Ошибка получения списка пользователей из БД: ", e);
        }
        return null;
    }

    public static Map<Long, String> initChats() {
        String checkChatQuery = "SELECT chat_id, chat_title FROM " + TG_CHATS_TABLE;
        Map<Long, String> result = new HashMap<>();
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement checkChat = connection.prepareStatement(checkChatQuery)) {
                ResultSet resultChatSet = checkChat.executeQuery();
                while (resultChatSet.next()) {
                    result.put(resultChatSet.getLong("chat_id"), resultChatSet.getString("chat_title"));
                }
                return result;
            }
        }
        catch (Exception e) {
            logger.error("Ошибка получения списка чатов из БД: ", e);
        }
        return null;
    }

    public static void insertChatInDB(Message message) {
        Long chatID = message.getChatId();
        String chatTitle = message.getChat().getTitle();
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String insertUserQuery = "INSERT INTO " + TG_CHATS_TABLE + " (chat_id, chat_title) VALUES (?, ?)";
            try (PreparedStatement insertUser = connection.prepareStatement(insertUserQuery)) {
                insertUser.setLong(1, chatID);
                insertUser.setString(2, chatTitle);
                insertUser.executeQuery();
            }
        } catch (Exception e) {
            logger.error("Ошибка при сохранении чата в БД: ", e);
        }
    }

    public static void insertUserInDB(Message message) {
        Long userID = message.getFrom().getId();
        String userName = message.getFrom().getUserName();

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String insertUserQuery = "INSERT INTO " + TG_USERS_TABLE + " (user_id, user_name) VALUES (?, ?)";
            try (PreparedStatement insertUser = connection.prepareStatement(insertUserQuery)) {
                insertUser.setLong(1, userID);
                insertUser.setString(2, "@" + userName);
                insertUser.executeQuery();
            }
        } catch (Exception e) {
            logger.error("Ошибка при сохранении пользователя в БД: ", e);
        }
    }

    public static String getUserNameByID(Long userID) {
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String checkQuery = "SELECT user_name FROM " + TG_USERS_TABLE + " WHERE user_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setLong(1, userID);
                ResultSet resultSet = checkStmt.executeQuery();
                resultSet.next();
                return resultSet.getString("user_name");
            }
        } catch (Exception e) {
            logger.error("Ошибка при поиске в БД длинны члена: ", e);
        }
        return String.valueOf(userID);
    }

    public static void setPidorWinner(Long chatID, Long userID) {
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

    public static void setCockSizeWinner (Long userID, Integer size) {
        LocalDate today = LocalDate.now();
        String insertQuery = "INSERT INTO " + COCKSIZE_STATS_TABLE + " (user_id, size, date) VALUES (?, ?, ?)";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setLong(1, userID);
                insertStmt.setInt(2, size);
                insertStmt.setDate(3, Date.valueOf(today));
                insertStmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Ошибка при записи в БД длинны члена: ", e);
        }
    }

    public static String getCockSizeImage(Integer size) {
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
