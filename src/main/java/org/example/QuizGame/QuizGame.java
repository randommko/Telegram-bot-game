package org.example.QuizGame;

import org.example.Chats.ChatsService;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.*;

public class QuizGame {
    private final TelegramBot bot;
    private final Map<Long, QuizService> quizMap = new HashMap<>(); //ключ ID чата, значение экземпляр QuizService
    private final Logger logger = LoggerFactory.getLogger(QuizGame.class);
    private final ChatsService chatsService = new ChatsService();
    private final QuizRepository repo = new QuizRepository();
    public QuizGame(){
        bot = TelegramBot.getInstance();
        List<Long> quizChatIDs = repo.initQuiz();
        quizChatIDs.forEach((chatID) ->
                quizMap.put(chatID, new QuizService(chatID))
        );
    }
    public void startQuizGame(Message message) {
        Long chatID = message.getChatId();
        String warningBotMsgNoRules = "Что бы бот мог читать ответы сделайте его администратором";
        if (!bot.checkAccessPrivileges(message)) {
            bot.sendMessage(chatID, warningBotMsgNoRules);
            return;
        }

        if (!quizMap.containsKey(chatID))
            quizMap.put(chatID, new QuizService(chatID));

        if (quizMap.get(chatID).isQuizStarted) {
            bot.sendMessage(chatID, "Викторина уже идет!");
            return;
        }

        quizMap.get(chatID).startQuiz();
    }
    public void getQuizStats(Message message) {
        bot.sendMessage(message.getChatId(), quizMap.get(message.getChatId()).getQuizStats());
    }
    public void stopQuiz(Long chatID) {
        if (!quizMap.get(chatID).isQuizStarted) {
            bot.sendMessage(chatID, "Никто не начинал викторину. Начните викторину командой /quiz_start");
            return;
        }

        quizMap.get(chatID).stopQuiz();
//        quizMap.get(chatID).endClueUpdateThread("Викторина была завершена по команде");
    }
    public void checkQuizAnswer(Message message) {
        //TODO: с каждым верным ответом запускается новый поток с подсказками. А старый не завершается
        Long chatID = message.getChatId();
        if (!quizMap.containsKey(chatID)) {
            logger.debug("Проверка ответа не произведена. Викторина ни разу не запускалась в чате: " + chatsService.getChatByID(chatID).getTitle());
            return;
        }

        if (!quizMap.get(chatID).isQuizStarted) {
            logger.debug("Проверка ответа не произведена. Викторина не запущена для чата: " + chatsService.getChatByID(chatID).getTitle());
            return;
        }

        logger.debug("Проверка ответа на вопрос викторины: " + message.getText());
        String answer = message.getText();
        Long userID = message.getFrom().getId();
        Integer points = quizMap.get(chatID).checkAnswer(answer);

        if (points != -1) {
            bot.sendReplyMessage(chatID, message.getMessageId(), "Правильный ответ! Вы заработали " + points + " очков!");
            quizMap.get(chatID).endClueUpdateThread("Получен верный ответ на вопрос");
            quizMap.get(chatID).countAnswer(userID, points);
        }
    }

}
