package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.Style;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class Quiz {
    private static final String QUIZ_QUESTION_TABLE = "quiz_questions";
    private static final Logger logger = LoggerFactory.getLogger(Quiz.class);
    public static Map<Integer, String> getRandomQuestion() {
        //TODO: дополнительно возращать ответ, зачем?
        Map<Integer, String> question = new HashMap<>();
        String sql = "SELECT id, question FROM " + QUIZ_QUESTION_TABLE + " ORDER BY RANDOM() LIMIT 1";

        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        question.put(rs.getInt("text"), String.valueOf(rs.getInt("id")));
                        IncrementQuestion(rs.getInt("id"));
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
        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
}
