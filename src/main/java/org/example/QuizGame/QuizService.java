package org.example.QuizGame;

import com.vdurmont.emoji.EmojiParser;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class QuizService {
    private final QuizRepository repo = new QuizRepository();
    public boolean isQuizStarted = false;
    private final TelegramBot bot;
    private final Long chatID;
    public CompletableFuture<Void> currentQuestionThread;
//    public ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    public Integer noAnswerCount = 0;
    public Integer currentQuestionID = null;
    private String clueText;
    private final Logger logger = LoggerFactory.getLogger(QuizService.class);

    public QuizService(Long chatID) {
        this.chatID = chatID;
        bot = TelegramBot.getInstance();
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
    public Integer checkQuizAnswer(String answer) {
        if (normalizeAnswer(repo.getQuestionAnswerByID(currentQuestionID)).equals(normalizeAnswer(answer))) {
            noAnswerCount = 0;
            return calculatePoints(answer.toLowerCase(), clueText);
        }
        return -1;
    }
    public void countAnswer(Long userID, Integer points, Long chatID) {
        repo.setScore(userID, points, chatID);
        repo.setUserAnswer(userID, points, chatID, currentQuestionID);
        repo.incrementQuestion(currentQuestionID);
    }
    public void startQuiz() {
        isQuizStarted = true;
        bot.sendMessage(chatID, EmojiParser.parseToUnicode(":tada::tada::tada: Викторина начинается! :tada::tada::tada:"));
    }
    public void stopQuiz() {
        isQuizStarted = false;
        bot.sendMessage(chatID, "Викторина завершена");
    }
    public void createClue() {
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
    public void updateClue() {
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
    public Integer getRemainingNumberOfClue() {
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
    public void newRandomQuestion() {
        currentQuestionID = repo.getRandomQuestionID();
    }
    public String getClue() {
        return clueText;
    }
    public String getQuestion() {
        return repo.getQuestionTextByID(currentQuestionID);
    }
    public String getAnswer() {
        return repo.getQuestionAnswerByID(currentQuestionID);
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
    public Integer calculatePoints (String userAnswer, String clue) {
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
    public void endClueUpdateThread (String reason) {
        currentQuestionThread.cancel(true); // Отмена задачи
        logger.debug("Поток с обновлением подсказок завершен. Причина: " + reason);
    }


}
