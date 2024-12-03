package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;


public class Quiz {
    public boolean isQuizStarted = false;
    public String currentQuestionText;
    public Integer currentQuestionID;
    public String currentAnswer;
    public String clue;
    private final String QUIZ_QUESTION_TABLE = "public_test.quiz_questions";
    private final String QUIZ_ANSWERS_TABLE = "public_test.quiz_answers";
    private final String QUIZ_STATS_TABLE = "public_test.quiz_stats";
    private final Logger logger = LoggerFactory.getLogger(Quiz.class);
    public void getRandomQuestion() {
        //TODO: дополнительно возращать ответ, зачем?
        //Map<Integer, String> question = new HashMap<>();
        String sql = "SELECT id, question, answer FROM " + QUIZ_QUESTION_TABLE + " ORDER BY RANDOM() LIMIT 1";

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
            logger.error("Произошла ошибка при поулчении вопроса из БД: ", e);

        }
    }
    private void IncrementQuestion () {
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
    public Integer calculatePoints (String userAnswer) {
        //clue - текущая подсказка
        //userAnswer - ответ пользователя
        int count = 0;
        for (int i = 0; i < clue.length(); i++) {
            if (clue.toLowerCase().charAt(i) != userAnswer.charAt(i)) {
                count++;
            }
        }
        return count;
    }
    private void setUserAnswer(String user_name, Integer points, Long chatID, String chat_name) {
        String insertQuery = "INSERT INTO " + QUIZ_ANSWERS_TABLE + " (user_name, question_id, get_points, chat_id, chat_name) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setString(1, user_name);
                insertStmt.setInt(2, currentQuestionID);
                insertStmt.setInt(3, points);
                insertStmt.setLong(4, chatID);
                insertStmt.setString(5, chat_name);
                insertStmt.execute();
            }
        } catch (Exception e) {
            logger.error("Ошибка записи верного ответа: ", e);
        }
    }
    public void setScore (String user_name, Integer points, Long chatID, String chat_name) {
        setUserAnswer(user_name, points, chatID, chat_name);
        IncrementQuestion();

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

    public void newQuestion() {
        getRandomQuestion();
        StringBuilder result = new StringBuilder();
        // Проходим по каждому символу строки
        for (char ch : currentAnswer.toCharArray()) {
            if (Character.isDigit(ch)) { // Проверяем, является ли символ цифрой
                result.append("*"); // Добавляем '*' count раз
            } else if (Character.isLetter(ch)) {
                result.append("*"); // Добавляем '*' count раз
            }
            else {
                result.append(ch); // Сохраняем символ (например, пробел)
            }
        }
        clue = result.toString();
    }
}
