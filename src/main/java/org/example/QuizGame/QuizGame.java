package org.example.QuizGame;

import com.vdurmont.emoji.EmojiParser;
import org.example.Chats.ChatsService;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.*;
import java.util.concurrent.*;

public class QuizGame {
    private final TelegramBot bot;
    private final Map<Long, QuizService> quizMap = new HashMap<>(); //ключ ID чата, значение экземпляр QuizService
    private final Logger logger = LoggerFactory.getLogger(QuizGame.class);
    //TODO: сделать разные параметры в зависимости от среды: ПРОД и ДЕВ
    private final int quizClueTimer = 2000;
    private final ChatsService chatsService = new ChatsService();
    public Integer currentClueMessageID = null;
    public Integer currentQuestionMessageID = null;
    private final QuizRepository repo = new QuizRepository();
    public final ThreadPoolExecutor executorQuizGame = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public QuizGame() {
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

        // Проверяем, завершены ли предыдущие потоки
        if (quizMap.get(chatID).currentQuestionThread != null && !quizMap.get(chatID).currentQuestionThread.isDone()) {
            quizMap.get(chatID).endClueUpdateThread("Новая викторина запущена до завершения предыдущей");
        }

        quizMap.get(chatID).startQuiz();

        CompletableFuture.runAsync(() -> startGameUntilEnd(chatID), executorQuizGame);
        logger.info("Количество активных потоков с викторинами: " + executorQuizGame.getActiveCount());
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
        quizMap.get(chatID).endClueUpdateThread("Викторина была завершена по команде");
    }
    private void sendClue(Long chatID) {
        logger.debug("Подсказка обновлена");
        String msg = EmojiParser.parseToUnicode(":bulb: Подсказка: " + quizMap.get(chatID).getClue());
        if (currentQuestionMessageID == null)
            bot.sendMessage(chatID, "Вопрос не был задан");
        if (currentClueMessageID == null)
            currentClueMessageID =  bot.sendReplyMessage(chatID, currentQuestionMessageID, msg);
        else
            if (!bot.editMessage(chatID, currentClueMessageID, msg))
                bot.sendMessage(chatID, msg);
    }
    private void sendQuestion(Long chatID) {
        currentQuestionMessageID = bot.sendMessage(chatID,
                EmojiParser.parseToUnicode(":question: Вопрос №" + quizMap.get(chatID).currentQuestionID + ": " + quizMap.get(chatID).getQuestion()));
    }
    public void checkQuizAnswer(Message message) {
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
        Integer points = quizMap.get(chatID).checkQuizAnswer(answer);
        if (points != -1) {
            bot.sendReplyMessage(chatID, message.getMessageId(), "Правильный ответ! Вы заработали " + points + " очков!");
            quizMap.get(chatID).endClueUpdateThread("Получен верный ответ на вопрос");
            quizMap.get(chatID).countAnswer(userID, points, chatID);
        }
    }
    private void startGameUntilEnd(Long chatID) {
        logger.info("Запускаем бесконечный цикл викторины для чата " + chatsService.getChatByID(chatID).getType());
        do {
            currentClueMessageID = null;
            quizMap.get(chatID).newRandomQuestion();

            if (quizMap.get(chatID).currentQuestionID == null) {
                bot.sendMessage(chatID, "В БД нет вопросов");
                logger.warn("В БД нет вопросов - викторина завершена");
                quizMap.get(chatID).stopQuiz();
                executorQuizGame.shutdownNow();
                return;
            }

            quizMap.get(chatID).createClue();
            sendQuestion(chatID);
            sendClue(chatID);

            logger.debug("Ответ на вопрос в чате " + chatsService.getChatByID(chatID).getType() + ": " + quizMap.get(chatID).getAnswer());

            quizMap.get(chatID).currentQuestionThread = CompletableFuture.runAsync(() -> {
                startClueUpdateThread(chatID);
            }, quizMap.get(chatID).executorClueUpdate);

            logger.info("Количество активных потоков с отправкой подсказок: " + quizMap.get(chatID).executorClueUpdate.getActiveCount());

            try {
                quizMap.get(chatID).currentQuestionThread.join(); // Ожидание завершения потока с подсказками
            } catch (CancellationException e) {
                logger.debug("Получен верный ответ. Поток с подсказками был прерван: " + e);
            }

            if (quizMap.get(chatID).noAnswerCount >= 3) {
                quizMap.get(chatID).stopQuiz();
                quizMap.get(chatID).endClueUpdateThread("Три вопроса подряд без верного ответа");
            }

        } while (quizMap.get(chatID).isQuizStarted);
        logger.info("Бесконечный цикл викторины для чата " + chatsService.getChatByID(chatID).getType() + " завершен");
        logger.info("Количество активных потоков с викторинами: " + executorQuizGame.getActiveCount());
    }
    private void startClueUpdateThread(Long chatID) {
        logger.info("Запущен поток с подсказками для чата " + chatsService.getChatByID(chatID).getType());

        boolean questionEndFlag = false; //признак, того что вопрос завершен
        while ((quizMap.get(chatID).isQuizStarted) & (!questionEndFlag)) {
            try {
                Thread.sleep(quizClueTimer);
                int remainingNumberOfClue = 1;
                if (quizMap.get(chatID).getRemainingNumberOfClue() > remainingNumberOfClue) {
                    quizMap.get(chatID).updateClue();
                    sendClue(chatID);
                } else {
                    questionEndFlag = true;
                    if (!bot.editMessage(chatID, currentClueMessageID,"Правильный ответ: " + quizMap.get(chatID).getAnswer()))
                        bot.sendMessage(chatID,"Правильный ответ: " + quizMap.get(chatID).getAnswer());
                    quizMap.get(chatID).noAnswerCount++;
                }
            } catch (InterruptedException e) {
                logger.debug("Отправка подсказок была прервана (Викторина завершена?)");
            }
        }
    }

}
