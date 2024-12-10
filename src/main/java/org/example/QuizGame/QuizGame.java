package org.example.QuizGame;

import org.example.DataSourceConfig;

import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;


public class QuizGame {
    TelegramBot bot;
    private final Map<Long, QuizService> quizMap = new HashMap<>(); //ключ ID чата, значение экземпляр Quiz
    private final Logger logger = LoggerFactory.getLogger(QuizGame.class);
    private final int quizClueTimer = 5000;
    public Integer currentClueMessageID = null;

    public QuizGame() {
        bot = TelegramBot.getInstance();
        initQuiz();
    }
    private void initQuiz() {
        //TODO: перенести в репо
        List<Long> quizChatIDs = new ArrayList<>();
        String QUIZ_STATS_TABLE = "public.quiz_stats";
        String getScoreQuery = "SELECT DISTINCT chat_id FROM " + QUIZ_STATS_TABLE;
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(getScoreQuery)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        quizChatIDs.add(rs.getLong("chat_id"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при получения счета в БД: ", e);
        }
        quizChatIDs.forEach((chatID) ->
                quizMap.put(chatID, new QuizService(chatID))
        );
    }
    public void startQuizGame(Message message) {
        Long chatID = message.getChatId();
        if (!quizMap.containsKey(chatID))
            quizMap.put(chatID, new QuizService(chatID));
        quizMap.get(chatID).startQuiz();
        sendQuestion(chatID);
    }
    public void getQuizStats(Message message) {
        bot.sendMessage(message.getChatId(), quizMap.get(message.getChatId()).getQuizStats());
    }
    public void stopQuiz(Long chatID) {
        quizMap.get(chatID).isQuizStarted = false;
        bot.sendMessage(chatID, "Викторина завершена");
    }
    private Integer sendClue(Long chatID, String text) {
        return bot.sendMessage(chatID, "Подсказка: " + text);
    }
    public void checkQuizAnswer(Message message) {
        Long chatID = message.getChatId();
        String answer = message.getText();
        Long userID = message.getFrom().getId();
        Integer points = quizMap.get(chatID).checkQuizAnswer(answer);
        if (points != -1) {
            bot.sendReplyMessage(chatID, message.getMessageId(), "Правильный ответ! Вы заработали " + points + " очков!");
            quizMap.get(chatID).setScore(userID, points, chatID);
            quizMap.get(chatID).newQuestion();
            sendQuestion(chatID);
            sendClue(chatID, quizMap.get(chatID).getClue());
        }

    }
    private void sendQuestion(Long chatID) {
    //TODO: поток с отправкой подсказок должен запускаться в этйо функции, сразу после отправки вопроса. И там же проверяем был ли ответ на вопрос что бы завершить поток
        quizMap.get(chatID).newQuestion();
        if (quizMap.get(chatID).currentQuestionID != null) {
            bot.sendMessage(chatID, quizMap.get(chatID).getQuestion());
            sendClue(chatID, quizMap.get(chatID).getClue());
            Thread thread = new Thread(() -> {
                do {
                    try {
                        Thread.sleep(quizClueTimer);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    currentClueMessageID = sendClue(chatID, quizMap.get(chatID).updateClue());
                } while (quizMap.get(chatID).getRemainingNumberOfClue() > 2);

                if (quizMap.get(chatID).isQuizStarted) {
                    bot.sendMessage(chatID, "Правильный ответ: " + quizMap.get(chatID).getQuestion());
                    quizMap.get(chatID).noAnswerCount++;
                }
                if (quizMap.get(chatID).noAnswerCount >= 3)
                    quizMap.get(chatID).stopQuiz();
            });
            thread.start();
        }
        else {
            bot.sendMessage(chatID, "В БД нет вопросов");
            quizMap.get(chatID).stopQuiz();
        }
    }
}
