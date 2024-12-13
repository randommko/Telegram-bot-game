package org.example.QuizGame;

import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.*;
import java.util.concurrent.*;

import static org.example.Emodji.*;

public class QuizGame {
    private final TelegramBot bot;
    private final Map<Long, QuizService> quizMap = new HashMap<>(); //ключ ID чата, значение экземпляр Quiz
    private final Logger logger = LoggerFactory.getLogger(QuizGame.class);
    private final int quizClueTimer = 15000;
    CompletableFuture<Void> currentQuestionThread;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    public Integer currentClueMessageID = null;
    public Integer currentQuestionMessageID = null;
    private final QuizRepository repo = new QuizRepository();


    public QuizGame() {
        bot = TelegramBot.getInstance();
        List<Long> quizChatIDs = repo.initQuiz();
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
            Thread thread = new Thread(() -> startGameUntilEnd(chatID));
            thread.start();
        } else {
            bot.sendMessage(chatID, "Викторина уже идет!");
        }
    }
    public void getQuizStats(Message message) {
        bot.sendMessage(message.getChatId(), quizMap.get(message.getChatId()).getQuizStats());
    }
    public void stopQuiz(Long chatID) {
        quizMap.get(chatID).stopQuiz();
//        executor.shutdownNow(); // Остановка всех потоков
    }
    private void sendClue(Long chatID) {
//        logger.info("Подсказка обновлена");
        String msg = PAPERCLIP_EMODJI + " Подсказка: " + quizMap.get(chatID).getClue();
        if (currentQuestionMessageID == null)
            bot.sendMessage(chatID, "Вопрос не был задан");
        if (currentClueMessageID == null)
            currentClueMessageID =  bot.sendReplyMessage(chatID, currentQuestionMessageID, msg);
        else
            bot.editMessage(chatID, currentClueMessageID, msg);
    }
    private void sendQuestion(Long chatID) {
        currentQuestionMessageID = bot.sendMessage(chatID, QUESTION_EMODJI + " Вопрос №" + quizMap.get(chatID).currentQuestionID + ": " + quizMap.get(chatID).getQuestion());
    }
    public void checkQuizAnswer(Message message) {
        Long chatID = message.getChatId();
        if (quizMap.get(chatID).isQuizStarted) {
            logger.info("Проверка ответа на вопрос викторины: " + message.getText());
            String answer = message.getText();
            Long userID = message.getFrom().getId();
            Integer points = quizMap.get(chatID).checkQuizAnswer(answer);
            if (points != -1) {
                bot.sendReplyMessage(chatID, message.getMessageId(), "Правильный ответ! Вы заработали " + points + " очков!");
                currentQuestionThread.cancel(true); // Отмена задачи
                quizMap.get(chatID).countAnswer(userID, points, chatID);
            }
        }
        else {
            logger.info("Викторина не запущена для чата: " + chatID);
        }
    }
    private void startGameUntilEnd(Long chatID) {
        logger.info("Запускаем бесконечный цикл викторины для чата " + chatID);
        do {
            currentClueMessageID = null;
            quizMap.get(chatID).newRandomQuestion();
            quizMap.get(chatID).createClue();
            if (quizMap.get(chatID).currentQuestionID != null) {
                sendQuestion(chatID);
                sendClue(chatID);
                startQuestionThread(chatID);
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
    private void startQuestionThread(Long chatID) {
        currentQuestionThread = CompletableFuture.runAsync(() -> {
            boolean questionEndFlag = true;
            while ((quizMap.get(chatID).isQuizStarted) & (questionEndFlag)) {
                try {
//                    for (int i = 0; i < quizClueTimer/1000; i++) {
//                        Thread.sleep(quizClueTimer/1000);
//                        logger.info("");
//                    }
                    Thread.sleep(quizClueTimer);
                    if (quizMap.get(chatID).getRemainingNumberOfClue() > 2) {
                        quizMap.get(chatID).updateClue();
                        sendClue(chatID);
                    } else {
                        questionEndFlag = false;
                        bot.editMessage(chatID, currentClueMessageID,"Правильный ответ: " + quizMap.get(chatID).getAnswer());
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
