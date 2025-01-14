package org.example.QuizGame;

import com.vdurmont.emoji.EmojiParser;
import org.example.Chats.ChatsService;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static org.example.QuizGame.QuizUtil.*;

public class QuizService {
    private final QuizRepository repo = new QuizRepository();
    private final ChatsService chatsService = new ChatsService();
    public boolean isQuizStarted = false;
    private final TelegramBot bot;
    private final Long currentChatID;
    public CompletableFuture<Void> currentClueThread;
    public final ThreadPoolExecutor executorClueUpdate = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    public Integer noAnswerCount = 0;
    public Integer currentQuestionID = null;
    public Integer currentClueMessageID = null;
    public Integer currentQuestionMessageID = null;
    //Время между подсказками в мс
    private final int quizClueTimer = 15000;
    private String clueText;
    private final Logger logger = LoggerFactory.getLogger(QuizService.class);

    public QuizService(Long chatID) {
        this.currentChatID = chatID;
        bot = TelegramBot.getInstance();
    }
    public void startQuiz() {
        isQuizStarted = true;
        bot.sendMessage(currentChatID, EmojiParser.parseToUnicode(":tada::tada::tada: Викторина начинается! :tada::tada::tada:"));
        startGameUntilEnd();
    }
    public void stopQuiz() {
        isQuizStarted = false;
        endClueUpdateThread("Викторина завершена");
        bot.sendMessage(currentChatID, "Викторина завершена");
    }
    public Integer checkAnswer(String answer) {
        if (normalizeAnswer(repo.getQuestionAnswerByID(currentQuestionID)).equals(normalizeAnswer(answer))) {
            noAnswerCount = 0;
            return calculatePoints(answer.toLowerCase(), clueText);
        }
        return -1;
    }
    public void countAnswer(Long userID, Integer points) {
        repo.setScore(userID, points, currentChatID);
        repo.setUserAnswer(userID, points, currentChatID, currentQuestionID);
        repo.incrementQuestion(currentQuestionID);
    }
    private void startGameUntilEnd() {
        logger.info("Запускаем бесконечный цикл викторины для чата " + chatsService.getChatByID(currentChatID).getType());

        do {
            currentClueMessageID = null;
            newRandomQuestion();

            if (currentQuestionID == null) {
                bot.sendMessage(currentChatID, "В БД нет вопросов");
                logger.warn("В БД нет вопросов - викторина завершена");
                stopQuiz();
                return;
            }

            createClue();
            sendQuestion();
            sendClue();

            logger.debug("Ответ на вопрос: " + chatsService.getChatByID(currentChatID).getType() + ": " + getAnswer());
            currentClueThread = CompletableFuture.runAsync(this::startClueUpdateThread, executorClueUpdate);
            logger.info("Количество активных потоков с отправкой подсказок: " + executorClueUpdate.getActiveCount());

            try {
//                currentClueThread.join(); // Ожидание завершения потока с подсказками
                currentClueThread.get(); // Ожидание завершения потока с подсказками
            } catch (CancellationException e) {
                logger.debug("Поток с подсказками был прерван: " + e);
            } catch (Exception e) {
                logger.debug("Ошибка при ожидании завершения потока: " + e);
            }

            if (noAnswerCount >= 3) {
                stopQuiz();
                endClueUpdateThread("Три вопроса подряд без верного ответа");
            }
        } while (isQuizStarted);
        logger.info("Бесконечный цикл викторины для чата " + chatsService.getChatByID(currentChatID).getType() + " завершен");
    }
    private void startClueUpdateThread() {
        logger.info("Запущен поток с подсказками для чата " + chatsService.getChatByID(currentChatID).getType());

        boolean questionEndFlag = false; //признак, того что вопрос завершен
        while (isQuizStarted && !questionEndFlag && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(quizClueTimer);
                if (getRemainingNumberOfClue(getAnswer(), clueText) > 1) {
                    updateClue();
                    sendClue();
                } else {
                    questionEndFlag = true;
                    if (!bot.editMessage(currentChatID, currentClueMessageID, "Правильный ответ: " + getAnswer()))
                        bot.sendMessage(currentChatID, "Правильный ответ: " + getAnswer());
                    noAnswerCount++;
                }
            } catch (InterruptedException e) {
                logger.debug("Отправка подсказок была прервана (Викторина завершена?)");
                Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
                break; // Выходим из цикла
            }
        }

        // Логирование завершения потока
        if (Thread.currentThread().isInterrupted()) {
            logger.debug("Поток с подсказками был прерван и завершен.");
        } else if (!isQuizStarted) {
            logger.debug("Поток с подсказками завершен, так как викторина остановлена.");
        } else if (questionEndFlag) {
            logger.debug("Поток с подсказками завершен, так как вопрос завершен.");
        }
    }
    public void endClueUpdateThread (String reason) {
        //TODO: функция не завершает поток с подсказками
        //currentClueThread.cancel(true); // Отмена задачи
        if (currentClueThread != null && !currentClueThread.isDone()) {
            currentClueThread.cancel(true); // Прерываем поток
            logger.info("Поток с подсказками прерван: " + reason);
        } else {
            logger.warn("Функция endClueUpdateThread() не смогла прервать поток с подсказками: " + reason);
        }
    }
    public String getQuizStats() {
        Map<String, Integer> stats = repo.getScore(currentChatID);

        // Преобразуем Map в List<Entry> и сортируем по убыванию значений
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(stats.entrySet());
        sortedList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        StringBuilder statsMessage = getStringFromSortedList(sortedList);
        return statsMessage.toString();
    }
    private static StringBuilder getStringFromSortedList(List<Map.Entry<String, Integer>> sortedList) {
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : sortedList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        StringBuilder statsMessage = new StringBuilder("Статистика викторины:\n");
        sortedMap.forEach((userName, score) ->
                statsMessage.append(userName.startsWith("@") ? userName.substring(1) : userName)
                        .append(": ").append(score).append(" очков\n")
        );
        return statsMessage;
    }
    private void createClue() {
        StringBuilder result = new StringBuilder();
        // Проходим по каждому символу строки
        for (char ch : getAnswer().toCharArray()) {
            if (Character.isDigit(ch)) { // Проверяем, является ли символ цифрой
                result.append("*"); // Добавляем '*' count раз
            } else if (Character.isLetter(ch)) {
                result.append("*"); // Добавляем '*' count раз
            }
            else {
                result.append(ch); // Сохраняем символ (например, пробел)
            }
        }
        clueText = result.toString();
    }
    private void updateClue() {
        String currentAnswer = repo.getQuestionAnswerByID(currentQuestionID);
        if (getRemainingNumberOfClue(getAnswer(), clueText) < 1)
            return;
        char[] clueChar = clueText.toCharArray();
        char[] answerChar = currentAnswer.toCharArray();
        int randomNum;
        do {
            randomNum = new Random().nextInt(currentAnswer.length());
        } while (clueChar[randomNum] != '*');

        clueChar[randomNum] = answerChar[randomNum]; // заменяем символ с индексом 1
        clueText = new String(clueChar);
    }
    private void sendQuestion() {
        logger.debug("Вопрос в чате " + chatsService.getChatByID(currentChatID).getType() + " №" + currentQuestionID + ": " + getQuestion());
        currentQuestionMessageID = bot.sendMessage(currentChatID,
                EmojiParser.parseToUnicode(":question: Вопрос №" + currentQuestionID + ": " + getQuestion()));
    }
    private void sendClue() {
        String msg = EmojiParser.parseToUnicode(":bulb: Подсказка: " + clueText);
        if (currentQuestionMessageID == null)
            bot.sendMessage(currentChatID, "Вопрос не был задан");
        if (currentClueMessageID == null)
            currentClueMessageID =  bot.sendReplyMessage(currentChatID, currentQuestionMessageID, msg);
        else if (!bot.editMessage(currentChatID, currentClueMessageID, msg))
            bot.sendMessage(currentChatID, msg);
        logger.debug("Подсказка обновлена: " + clueText);
    }
    private void newRandomQuestion() {
        currentQuestionID = repo.getRandomQuestionID();
    }
    private String getQuestion() {
        return repo.getQuestionTextByID(currentQuestionID);
    }
    private String getAnswer() {
        return repo.getQuestionAnswerByID(currentQuestionID);
    }

}
