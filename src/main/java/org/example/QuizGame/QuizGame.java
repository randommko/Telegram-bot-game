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
import java.util.concurrent.*;

public class QuizGame {
    private final TelegramBot bot;
    private final Map<Long, QuizService> quizMap = new HashMap<>(); //ключ ID чата, значение экземпляр Quiz
    private final Logger logger = LoggerFactory.getLogger(QuizGame.class);
    private final int quizClueTimer = 5000;
    CompletableFuture<Void> currentQuestionThread;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    //private static volatile boolean runningQuestionThread;
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
        if (!quizMap.get(chatID).isQuizStarted) {
            quizMap.get(chatID).startQuiz();
            Thread thread = new Thread(() -> {
                startGameUntilEnd(chatID);
            });
            thread.start();
        } else {
            bot.sendMessage(chatID, "Викторина уже идет!");
        }
    }
    public void getQuizStats(Message message) {
        bot.sendMessage(message.getChatId(), quizMap.get(message.getChatId()).getQuizStats());
    }
    public void stopQuiz(Long chatID) {
        quizMap.get(chatID).isQuizStarted = false;
        executor.shutdownNow(); // Остановка всех потоков
        bot.sendMessage(chatID, "Викторина завершена");
    }
    private Integer sendClue(Long chatID, String text) {
        return currentClueMessageID = bot.sendMessage(chatID, "Подсказка: " + text);
    }
    private void sendQuestion(Long chatID) {
        bot.sendMessage(chatID, quizMap.get(chatID).getQuestion());
    }
    public void checkQuizAnswer(Message message) {
        logger.info("Проверка ответа на вопрос викторины: " + message.getText());
        Long chatID = message.getChatId();
        String answer = message.getText();
        Long userID = message.getFrom().getId();
        Integer points = quizMap.get(chatID).checkQuizAnswer(answer);
        if (points != -1) {
            bot.sendReplyMessage(chatID, message.getMessageId(), "Правильный ответ! Вы заработали " + points + " очков!");
            currentQuestionThread.cancel(true); // Отмена задачи
            quizMap.get(chatID).setScore(userID, points, chatID);
        }
    }
    private void startGameUntilEnd(Long chatID) {
        logger.info("Запускаем бесконечный цикл викторины для чата " + chatID);
        do {
            quizMap.get(chatID).newRandomQuestion();
            quizMap.get(chatID).createClue();
            if (quizMap.get(chatID).currentQuestionID != null) {
                sendQuestion(chatID);
                currentQuestionThread = startQuestionThread(chatID);
                try {
                    currentQuestionThread.join(); // Ожидание завершения потока
                } catch (CancellationException e) {
                    logger.info("Получен верный ответ. Поток с подсказками был прерван: " + e);
                }
            }
            else {
                bot.sendMessage(chatID, "В БД нет вопросов");
                logger.info("В БД нет вопросов - викторина завершена");
                quizMap.get(chatID).stopQuiz();
            }
        } while (quizMap.get(chatID).isQuizStarted);
        logger.info("Бесконечный цикл викторины для чата " + chatID + " завершен");
    }
    private CompletableFuture<Void> startQuestionThread(Long chatID) {
        sendClue(chatID, quizMap.get(chatID).getClue());
        return CompletableFuture.runAsync(() -> {
            boolean questionEndFlag = true;
            while ((quizMap.get(chatID).isQuizStarted) & (questionEndFlag)) {
                try {
                    Thread.sleep(quizClueTimer);
                    if (quizMap.get(chatID).getRemainingNumberOfClue() > 2) {
                        quizMap.get(chatID).updateClue();
                        sendClue(chatID, quizMap.get(chatID).getClue());
                    } else {
                        questionEndFlag = false;
                        bot.sendMessage(chatID, "Правильный ответ: " + quizMap.get(chatID).getAnswer());
                        quizMap.get(chatID).noAnswerCount++;
                    }
                } catch (InterruptedException e) {
                    logger.info("Отправка подсказок была прервана (Викторина завершена?)");
                }
            }
            if (quizMap.get(chatID).noAnswerCount >= 3)
                quizMap.get(chatID).stopQuiz();
        }, executor);

    }
}
