package org.example.quizGame;

import org.example.DataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.regex.Pattern;

import static org.example.TablesDB.TG_CHATS_TABLE;

public class QuizService {
    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);
    public static Map<Long, String> initChats() {
        String checkChatQuery = "SELECT chat_id, chat_title FROM " + TG_CHATS_TABLE;
        Map<Long, String> result = new HashMap<>();
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement checkChat = connection.prepareStatement(checkChatQuery)) {
                ResultSet resultChatSet = checkChat.executeQuery();
                while (resultChatSet.next()) {
                    result.put(resultChatSet.getLong("chat_id"), resultChatSet.getString("chat_title"));
                }
                return result;
            }
        }
        catch (Exception e) {
            logger.error("Ошибка получения списка чатов из БД: ", e);
        }
        return null;
    }

    private Integer sendClue(Long chatID) {
        return sendMessage(chatID, "Подсказка: " + quizMap.get(chatID).clue).getMessageId();
//        if (quizMap.get(chatID).clueMessageID == null){
//            return sendMessage(chatID, "Подсказка: " + quizMap.get(chatID).clue).getMessageId();
//        }
//        else {
//            EditMessageText editMessageText = new EditMessageText();
//            editMessageText.setChatId(chatID);
//            editMessageText.setMessageId(quizMap.get(chatID).clueMessageID);
//            editMessageText.setText("**Подсказка**: " + quizMap.get(chatID).clue);
//            editMessageText.setParseMode("Markdown");
//            try {
//                execute(editMessageText);
//            } catch (Exception e) {
//                logger.error("Ошибка при изменения сообщения: ", e);
//            }
//            return quizMap.get(chatID).clueMessageID;
//        }
    }

    private void sendQuestion(Long chatID) {
        //TODO: поток с отправкой подсказок должен запускаться в этйо функции, сразу после отправки вопроса. И там же проверяем был ли ответ на вопрос что бы завершить поток
        quizMap.get(chatID).newQuestion();
        if (!quizMap.get(chatID).currentQuestionText.isEmpty()) {
            sendMessage(chatID, quizMap.get(chatID).currentQuestionText);
            quizMap.get(chatID).clueMessageID = sendClue(chatID);
//            Thread thread = new Thread(() -> {
            do {
                try {
                    Thread.sleep(quizClueTimer);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                quizMap.get(chatID).updateClue();
//                    if (quizMap.get(chatID).isQuizStarted)
                quizMap.get(chatID).clueMessageID = sendClue(chatID);
            } while ((quizMap.get(chatID).getRemainingNumberOfClue() > 2) & (quizMap.get(chatID).isQuizStarted));

            if (quizMap.get(chatID).isQuizStarted) {
                sendMessage(chatID, "Правильный ответ: " + quizMap.get(chatID).currentAnswer);
                quizMap.get(chatID).noAnswerCount++;
            }
            if (quizMap.get(chatID).noAnswerCount >= 3)
                stopQuiz(chatID);
//            });
//            thread.start();
        }
        else {
            sendMessage(chatID, "В БД нет вопросов");
            quizMap.get(chatID).isQuizStarted = false;
        }
    }

    private void obsceneAnswer(Message message) {
        //TODO: наполнение переменных должны происходить один раз при запуске приложения
        String answer = message.getText();
        //TODO: не верно работает сравнение с regexp
        Set<String> obscenePatterns = new HashSet<>();
        obscenePatterns.add("^хуй.*");
        obscenePatterns.add(".*хуй.*");
        obscenePatterns.add("^хуе.*");
        obscenePatterns.add("^хуё.*");
        obscenePatterns.add("^хуи.*");
        obscenePatterns.add("^Пидор.*");
        obscenePatterns.add("^пидор.*");
        obscenePatterns.add(".*ну и пошёл ты нахуй.*");
        obscenePatterns.add(".*да идиты в жопу.*");
        obscenePatterns.add(".*пиздец.*");

        List<String> botAnswerList = new ArrayList<>();
        botAnswerList.add("Пошел на хер");

        String botAnswer = "Сам такой";
        Random random = new Random();

        for (String item : obscenePatterns) {
            if (Pattern.matches(item, answer.toLowerCase())) {
                int randomIndex = random.nextInt(botAnswerList.size());
                botAnswer = botAnswerList.get(randomIndex);
            }
        }

        int randomNum = random.nextInt(10);
        if (randomNum == 5)
            sendReplyMessage(message, botAnswer);

    }

    private void checkQuizAnswer(Message message) {
        Long chatID = message.getChatId();
        String answer = message.getText();
        Long userID = message.getFrom().getId();

        if (!quizMap.get(chatID).isQuizStarted)
            return;
        obsceneAnswer(message);
        //TODO: буквы "е" и "ё" считать одинаковыми
        if (quizMap.get(chatID).currentAnswer.equalsIgnoreCase(answer)) {
            quizMap.get(chatID).noAnswerCount = 0;
            Integer points = quizMap.get(chatID).calculatePoints(answer.toLowerCase());
            quizMap.get(chatID).setScore(userID, points, chatID);
            //TODO: добавить ник пользователя в сообщение
            sendReplyMessage(message, "Правильный ответ! Вы заработали " + points.toString() + " очков!");
//            quizMap.get(chatID).newQuestion();
//            sendQuestion(chatID);
//            sendClue(chatID);
        }
    }

    private void getQuizStats(Message message) {
        Long chatID = message.getChatId();
        Map<String, Integer> stats;
        stats = quizMap.get(chatID).getScore(chatID);

        StringBuilder statsMessage = new StringBuilder("Статистика викторины:\n");
        stats.forEach((userName, score) ->
                statsMessage.append(userName).append(": ").append(score).append(" очков\n")
        );
        sendMessage(chatID, statsMessage.toString());
    }
}
