package org.example.QuizGame;

import com.vdurmont.emoji.EmojiParser;
import org.example.Chats.ChatsService;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;

public class QuizService {
    private final QuizRepository repo = new QuizRepository();
    private final ChatsService chatsService = new ChatsService();
    public boolean isQuizStarted = false;
    private final TelegramBot bot;
    private final Long chatID;
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
        this.chatID = chatID;
        bot = TelegramBot.getInstance();
    }
    public void startQuiz() {
        isQuizStarted = true;
        bot.sendMessage(chatID, EmojiParser.parseToUnicode(":tada::tada::tada: Викторина начинается! :tada::tada::tada:"));
        startGameUntilEnd();
    }
    public void stopQuiz() {
        isQuizStarted = false;
        endClueUpdateThread("Викторина завершена");
        bot.sendMessage(chatID, "Викторина завершена");
    }
    public Integer checkAnswer(String answer) {
        if (normalizeAnswer(repo.getQuestionAnswerByID(currentQuestionID)).equals(normalizeAnswer(answer))) {
            noAnswerCount = 0;
            return calculatePoints(answer.toLowerCase(), clueText);
        }
        return -1;
    }
    public void countAnswer(Long userID, Integer points) {
        repo.setScore(userID, points, chatID);
        repo.setUserAnswer(userID, points, chatID, currentQuestionID);
        repo.incrementQuestion(currentQuestionID);
    }
    private void startGameUntilEnd() {
        logger.info("Запускаем бесконечный цикл викторины для чата " + chatsService.getChatByID(chatID).getType());

        do {
            currentClueMessageID = null;
            newRandomQuestion();

            if (currentQuestionID == null) {
                bot.sendMessage(chatID, "В БД нет вопросов");
                logger.warn("В БД нет вопросов - викторина завершена");
                stopQuiz();
                return;
            }

            createClue();
            sendQuestion();
            sendClue();

            logger.debug("Ответ на вопрос: " + chatsService.getChatByID(chatID).getType() + ": " + getAnswer());

//            if (!currentClueThread.isCancelled())
//                currentClueThread.cancel(true);

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
        logger.info("Бесконечный цикл викторины для чата " + chatsService.getChatByID(chatID).getType() + " завершен");
    }
    private void startClueUpdateThread() {
        logger.info("Запущен поток с подсказками для чата " + chatsService.getChatByID(chatID).getType());

        boolean questionEndFlag = false; //признак, того что вопрос завершен
        while ((isQuizStarted) & (!questionEndFlag)) {
            try {
                Thread.sleep(quizClueTimer);
                if (getRemainingNumberOfClue() > 1) {
                    updateClue();
                    sendClue();
                } else {
                    questionEndFlag = true;
                    if (!bot.editMessage(chatID, currentClueMessageID,"Правильный ответ: " + getAnswer()))
                        bot.sendMessage(chatID,"Правильный ответ: " + getAnswer());
                    noAnswerCount++;
                }
            } catch (InterruptedException e) {
                logger.debug("Отправка подсказок была прервана (Викторина завершена?)");
            }
        }
    }
    public void endClueUpdateThread (String reason) {
        //TODO: функция не завершает поток с подсказками
        currentClueThread.cancel(true); // Отмена задачи
        if (currentClueThread == null)
            logger.error("Поток с подсказками не найден! Куда пропал?");
        if (!currentClueThread.isCancelled())
            logger.warn("Поток с подсказками не был отменен!");
        if (!currentClueThread.isDone())
            logger.warn("Поток с подсказками не был завершен!");
        if (currentClueThread.isCancelled())
            logger.info("Поток с подсказками был отменен! ОК! Причина: " + reason);
        if (currentClueThread.isDone())
            logger.info("Поток с подсказками был завершен! ОК! Причина: " + reason);

//        currentClueThread.complete(null);


    }
    public String getQuizStats() {
        Map<String, Integer> stats = repo.getScore(chatID);

        // Преобразуем Map в List<Entry> и сортируем по убыванию значений
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(stats.entrySet());
        sortedList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        // Если нужно, можно вернуть отсортированную карту
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : sortedList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        StringBuilder statsMessage = new StringBuilder("Статистика викторины:\n");
        sortedMap.forEach((userName, score) ->
                statsMessage.append(userName.startsWith("@") ? userName.substring(1) : userName)
                        .append(": ").append(score).append(" очков\n")
        );
        return statsMessage.toString();
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
        if (getRemainingNumberOfClue() < 1)
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
    private Integer getRemainingNumberOfClue() {
        int count = 0;
        String currentAnswer = getAnswer();
        float num = currentAnswer.length();
        for (int i = 0; i < num; i++) {
            if (clueText.toLowerCase().charAt(i) != currentAnswer.toLowerCase().charAt(i)) {
                count++;
            }
        }
        return count;
    }
    private void newRandomQuestion() {
        currentQuestionID = repo.getRandomQuestionID();
    }
    private String getClue() {
        return clueText;
    }
    private String getQuestion() {
        return repo.getQuestionTextByID(currentQuestionID);
    }
    private String getAnswer() {
        return repo.getQuestionAnswerByID(currentQuestionID);
    }
    private Integer calculatePoints (String userAnswer, String clue) {
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
    private void sendQuestion() {
        logger.debug("Вопрос в чате " + chatsService.getChatByID(chatID).getType() + " №" + currentQuestionID + ": " + getQuestion());
        currentQuestionMessageID = bot.sendMessage(chatID,
                EmojiParser.parseToUnicode(":question: Вопрос №" + currentQuestionID + ": " + getQuestion()));
    }
    private void sendClue() {
        String msg = EmojiParser.parseToUnicode(":bulb: Подсказка: " + getClue());
        if (currentQuestionMessageID == null)
            bot.sendMessage(chatID, "Вопрос не был задан");
        if (currentClueMessageID == null)
            currentClueMessageID =  bot.sendReplyMessage(chatID, currentQuestionMessageID, msg);
        else if (!bot.editMessage(chatID, currentClueMessageID, msg))
            bot.sendMessage(chatID, msg);
        logger.debug("Подсказка обновлена: " + getClue());
    }
    private static String normalizeAnswer(String answer) {
        // Приводим к нижнему регистру
        answer = answer.toLowerCase();

        // Заменяем "ё" на "е"
        answer = answer.replace('ё', 'е');

        // Убираем лишние пробелы (начало, конец, несколько пробелов подряд)
        answer = answer.trim().replaceAll("\\s+", " ");

        // Убираем возможные диакритические знаки
        answer = Normalizer.normalize(answer, Normalizer.Form.NFD);
        answer = answer.replaceAll("[\\p{M}]", ""); // Удаляем диакритические символы

        return answer;
    }
}
