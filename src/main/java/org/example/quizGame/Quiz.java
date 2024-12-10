package org.example.quizGame;

import org.example.DataSourceConfig;
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
    public Integer clueMessageID = null;
    public String currentQuestionText;
    public Integer currentQuestionID;
    public String currentAnswer;
    public String clue;

    private final Logger logger = LoggerFactory.getLogger(Quiz.class);


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
