package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class Quiz {
    private static final String QUIZ_QUESTION_TABLE = "public_test.quiz_questions";
    private static final String QUIZ_ANSWERS_TABLE = "public_test.quiz_answers";
    private static final String QUIZ_STATS_TABLE = "public_test.quiz_stats";
    private static final Logger logger = LoggerFactory.getLogger(Quiz.class);
    public static Map<Integer, String> getRandomQuestion() {
        //TODO: дополнительно возращать ответ, зачем?
        Map<Integer, String> question = new HashMap<>();
        String sql = "SELECT id, question FROM " + QUIZ_QUESTION_TABLE + " ORDER BY RANDOM() LIMIT 1";

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        question.put(rs.getInt("text"), String.valueOf(rs.getInt("id")));

                        return question;
                    } else {
                        // Если нет вопросов в БД
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при поулчении вопроса из БД: ", e);
            return null;
        }
    }
    public static boolean CheckQuestionAnswer(Integer questionID, String userAnswer) {
        String sql = "SELECT answer FROM " + QUIZ_QUESTION_TABLE + " WHERE id = ?";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, questionID);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    String correctAnswer = rs.getString("answer");
                    return Objects.equals(correctAnswer, userAnswer);
                }
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при поулчении ответа из БД: ", e);
            return false;
        }
    }
    private static void IncrementQuestion (Integer id) {
        String sqlIncrementQuestion = "UPDATE " + QUIZ_QUESTION_TABLE + " SET used_times = used_times + 1 WHERE id = ?";

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(sqlIncrementQuestion)) {
                insertStmt.setString(1, String.valueOf(id));
                insertStmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Ошибка при увеличении количества раз использоваения вопроса: ", e);
        }
    }
    private static Integer calculatePoints (String clue, String userAnswer) {
        //clue - текущая подсказка
        //userAnswer - ответ пользователя
        int count = 0;
        for (int i = 0; i < clue.length(); i++) {
            if (clue.charAt(i) != userAnswer.charAt(i)) {
                count++;
            }
        }
        return count;
    }
    private static void setUserAnswer(String user_name, Integer question_id, Integer points, String chat_id, String chat_name) {
        String insertQuery = "INSERT INTO " + QUIZ_ANSWERS_TABLE + " (user_name, question_id, get_points, chat_id, chat_name) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setString(1, user_name);
                insertStmt.setInt(2, question_id);
                insertStmt.setInt(3, points);
                insertStmt.setString(4, chat_id);
                insertStmt.setString(5, chat_name);
                insertStmt.execute();
            }
        } catch (Exception e) {
            logger.error("Ошибка записи верного ответа: ", e);
        }
    }
    public static void setScore (String user_name, Integer question_id, Integer points, String chat_id, String chat_name) {
        setUserAnswer(user_name, question_id, points, chat_id, chat_name);
        IncrementQuestion(question_id);

        String getScoreQuery = "SELECT score FROM " + QUIZ_STATS_TABLE + " WHERE user_name = ? AND chat_name = ?";

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(getScoreQuery)) {
                stmt.setString(1, user_name);
                stmt.setString(2, chat_name);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String sqlIncrementQuestion = "UPDATE " + QUIZ_STATS_TABLE + " SET score = score + " + points + " WHERE user_name = ? AND chat_name = ?";
                        try (PreparedStatement insertStmt = connection.prepareStatement(sqlIncrementQuestion)) {
                            insertStmt.setString(1, user_name);
                            insertStmt.setString(2, chat_name);
                            insertStmt.executeUpdate();
                        }
                    }
                    else {
                        String sqlInsertUserQuery = "INSERT INTO " + QUIZ_STATS_TABLE + "(user_name, score, chat_name) VALUES (?,?,?)";
                        try (PreparedStatement insertStmt = connection.prepareStatement(sqlInsertUserQuery)) {
                            insertStmt.setString(1, user_name);
                            insertStmt.setInt(2, points);
                            insertStmt.setString(3, chat_name);
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
