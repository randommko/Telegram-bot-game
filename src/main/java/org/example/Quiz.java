package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import static org.example.TablesDB.*;


public class Quiz {
    public boolean isQuizStarted = false;
    public Integer noAnswerCount = 0;
    public String currentQuestionText;
    public Integer currentQuestionID;
    public String currentAnswer;
    public String clue;

    private final Logger logger = LoggerFactory.getLogger(Quiz.class);
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

    public void updateClue() {
        if (getRemainingNumberOfClue() < 2)
            return;

        char[] clueChar = clue.toCharArray();
        char[] answerChar = currentAnswer.toCharArray();
        int randomNum;
        do {
            randomNum = new Random().nextInt(currentAnswer.length());
        } while (clueChar[randomNum] != '*');

        clueChar[randomNum] = answerChar[randomNum]; // заменяем символ с индексом 1
        clue = new String(clueChar);
    }

    public Integer getRemainingNumberOfClue() {
        int count = 0;
        float num = currentAnswer.length();
        for (int i = 0; i < num; i++) {
            if (clue.toLowerCase().charAt(i) != currentAnswer.toLowerCase().charAt(i)) {
                count++;
            }
        }

        return count;
    }
}
