package org.example.quizGame;

import org.example.DataSourceConfig;
import org.example.pidorGame.PidorGameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.example.TablesDB.*;

public class QuizRepository {
    private static final Logger logger = LoggerFactory.getLogger(QuizRepository.class);
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

    public void getRandomQuestion() {
        String sql = "SELECT id, question, answer FROM (SELECT id, question, answer FROM " + QUIZ_QUESTION_TABLE + " ORDER BY used_times ASC LIMIT 10) AS top_questions ORDER BY RANDOM() LIMIT 1;";

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                try (ResultSet queryResult = stmt.executeQuery()) {
                    if (queryResult.next()) {
                        currentQuestionText = queryResult.getString("question");
                        currentQuestionID = queryResult.getInt("id");
                        currentAnswer = queryResult.getString("answer");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при получении вопроса из БД: ", e);

        }
    }

    private void incrementQuestion() {
        String sqlIncrementQuestion = "UPDATE " + QUIZ_QUESTION_TABLE + " SET used_times = used_times + 1 WHERE id = ?";

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(sqlIncrementQuestion)) {
                insertStmt.setInt(1, currentQuestionID);
                insertStmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Ошибка при увеличении количества раз использоваения вопроса: ", e);
        }
    }

    private void setUserAnswer(Long userID, Integer points, Long chatID) {
        String insertQuery = "INSERT INTO " + QUIZ_ANSWERS_TABLE + " (user_id, question_id, get_points, chat_id) VALUES (?, ?, ?, ?)";

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setLong(1, userID);
                insertStmt.setInt(2, currentQuestionID);
                insertStmt.setInt(3, points);
                insertStmt.setLong(4, chatID);
                insertStmt.execute();
            }
        } catch (Exception e) {
            logger.error("Ошибка записи верного ответа: ", e);
        }
    }

    public Map<String, Integer> getScore(Long chatID) {
        Map<String, Integer> stats = new HashMap<>();
        String getScoreQuery = "SELECT tut.user_name, qst.score FROM " + QUIZ_STATS_TABLE + " AS qst JOIN " + TG_USERS_TABLE + " AS tut ON qst.user_id = tut.user_id WHERE qst.chat_id = ?";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(getScoreQuery)) {
                stmt.setLong(1, chatID);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        stats.put(rs.getString("user_name"), rs.getInt("score")) ;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при получения счета в БД: ", e);
        }

        return stats;
    }

    public void setScore (Long userID, Integer points, Long chatID) {
        setUserAnswer(userID, points, chatID);
        incrementQuestion();

        String getScoreQuery = "SELECT score FROM " + QUIZ_STATS_TABLE + " WHERE user_id = ? AND chat_id = ?";

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(getScoreQuery)) {
                stmt.setLong(1, userID);
                stmt.setLong(2, chatID);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String sqlIncrementQuestion = "UPDATE " + QUIZ_STATS_TABLE + " SET score = score + " + points + " WHERE user_id = ? AND chat_id = ?";
                        try (PreparedStatement insertStmt = connection.prepareStatement(sqlIncrementQuestion)) {
                            insertStmt.setLong(1, userID);
                            insertStmt.setLong(2, chatID);
                            insertStmt.executeUpdate();
                        }
                    }
                    else {
                        String sqlInsertUserQuery = "INSERT INTO " + QUIZ_STATS_TABLE + "(user_id, score, chat_id) VALUES (?,?,?)";
                        try (PreparedStatement insertStmt = connection.prepareStatement(sqlInsertUserQuery)) {
                            insertStmt.setLong(1, userID);
                            insertStmt.setInt(2, points);
                            insertStmt.setLong(3, chatID);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при записи счета в БД: ", e);
        }
    }
}
