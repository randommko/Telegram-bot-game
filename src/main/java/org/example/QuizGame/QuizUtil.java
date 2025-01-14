package org.example.QuizGame;

import java.text.Normalizer;

public class QuizUtil {
    public static String normalizeAnswer(String answer) {
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
    public static Integer calculatePoints (String userAnswer, String clue) {
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
    public static Integer getRemainingNumberOfClue(String currentAnswer, String clueText) {
        int count = 0;
        //String currentAnswer = getAnswer();
        float num = currentAnswer.length();
        for (int i = 0; i < num; i++) {
            if (clueText.toLowerCase().charAt(i) != currentAnswer.toLowerCase().charAt(i)) {
                count++;
            }
        }
        return count;
    }
}
