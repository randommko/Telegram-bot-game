package org.example.QuizGame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class QuizService {
    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);
    private final QuizRepository repo = new QuizRepository();
    public boolean isQuizStarted = false;
    private Long chatID;
    public Integer noAnswerCount = 0;
    public Integer currentQuestionID;
    private String clueText;


    public QuizService(Long chatID) {
        this.chatID = chatID;
    }
    public Integer checkQuizAnswer(String answer) {
        //TODO: буквы "е" и "ё" считать одинаковыми
        if (repo.getQuestionAnswerByID(currentQuestionID).equalsIgnoreCase(answer)) {
            noAnswerCount = 0;
            return calculatePoints(answer.toLowerCase(), clueText);
        }
        return -1;
    }
    public void setScore(Long userID, Integer points, Long chatID) {
        repo.setScore(userID, points, chatID);
    }
    public void startQuiz() {
        isQuizStarted = true;
        currentQuestionID = getRandomQuestionID();
        createClue();
    }
    public void stopQuiz() {
        isQuizStarted = false;
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
    public String updateClue() {
        String newClue;
        String currentAnswer = repo.getQuestionAnswerByID(currentQuestionID);
        if (getRemainingNumberOfClue() < 2) {
            newClue = currentAnswer;
            return newClue;
        }

        char[] clueChar = clueText.toCharArray();
        char[] answerChar = currentAnswer.toCharArray();
        int randomNum;
        do {
            randomNum = new Random().nextInt(currentAnswer.length());
        } while (clueChar[randomNum] != '*');

        clueChar[randomNum] = answerChar[randomNum]; // заменяем символ с индексом 1
        clueText = new String(clueChar);
        return clueText;
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
    private Integer getRandomQuestionID() {
        return repo.getRandomQuestionID();
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
        Map<String, Integer> stats;
        stats = repo.getScore(chatID);

        StringBuilder statsMessage = new StringBuilder("Статистика викторины:\n");
        stats.forEach((userName, score) ->
                statsMessage.append(userName).append(": ").append(score).append(" очков\n")
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
    public void newQuestion() {
        //TODO:
    }
}
