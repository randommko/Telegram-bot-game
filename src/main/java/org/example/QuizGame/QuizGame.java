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
    //Время между подсказками в мс
    private final int quizClueTimer = 15000;
    private final ChatsService chatsService = new ChatsService();
    private final int remainingNumberOfClue = 1;

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
        startGameUntilEnd(chatID);
//        logger.info("Количество активных потоков с викторинами: " + executorQuizGame.getActiveCount());
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
    private void sendClue(Long chatID) {
        String msg = EmojiParser.parseToUnicode(":bulb: Подсказка: " + quizMap.get(chatID).getClue());
        if (quizMap.get(chatID).currentQuestionMessageID == null)
            bot.sendMessage(chatID, "Вопрос не был задан");
        if (quizMap.get(chatID).currentClueMessageID == null)
            quizMap.get(chatID).currentClueMessageID =  bot.sendReplyMessage(chatID, quizMap.get(chatID).currentQuestionMessageID, msg);
        else
            if (!bot.editMessage(chatID, quizMap.get(chatID).currentClueMessageID, msg))
                bot.sendMessage(chatID, msg);
        logger.debug("Подсказка обновлена: " + quizMap.get(chatID).getClue());
    }
    private void sendQuestion(Long chatID) {
        quizMap.get(chatID).currentQuestionMessageID = bot.sendMessage(chatID,
                EmojiParser.parseToUnicode(":question: Вопрос №" + quizMap.get(chatID).currentQuestionID + ": " + quizMap.get(chatID).getQuestion()));
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
        Integer points = quizMap.get(chatID).checkQuizAnswer(answer);
        if (points != -1) {
            bot.sendReplyMessage(chatID, message.getMessageId(), "Правильный ответ! Вы заработали " + points + " очков!");
            quizMap.get(chatID).endClueUpdateThread("Получен верный ответ на вопрос");
            quizMap.get(chatID).countAnswer(userID, points, chatID);
        }
    }
    private void startGameUntilEnd(Long chatID) {
        logger.info("Запускаем бесконечный цикл викторины для чата " + chatsService.getChatByID(chatID).getType() + " (id = " + chatID + ")");
        do {
            quizMap.get(chatID).currentClueMessageID = null;
            quizMap.get(chatID).newRandomQuestion();

            if (quizMap.get(chatID).currentQuestionID == null) {
                bot.sendMessage(chatID, "В БД нет вопросов");
                logger.warn("В БД нет вопросов - викторина завершена");
                quizMap.get(chatID).stopQuiz();
                return;
            }
            logger.debug("Ответ на вопрос: " + quizMap.get(chatID).getAnswer());
            quizMap.get(chatID).createClue();
            sendQuestion(chatID);
            sendClue(chatID);
            startClueUpdateThread(chatID);
//            try {
//                quizMap.get(chatID).currentQuestionThread.isDone(); // Ожидание завершения потока с подсказками
//            } catch (CancellationException e) {
//                logger.debug("Получен верный ответ. Поток с подсказками был прерван: " + e);
//            }

            // Ожидание завершения потока с подсказками
            try {
                // Ожидаем завершения задачи с таймаутом
                quizMap.get(chatID).currentQuestionThread.get(quizClueTimer + 100, TimeUnit.MILLISECONDS);
                logger.debug("Поток с подсказками завершен.");
            } catch (TimeoutException e) {
                logger.debug("Поток с подсказками еще выполняется.");
            } catch (CancellationException e) {
                logger.debug("Поток с подсказками был прерван: " + e);
            } catch (Exception e) {
                logger.debug("Ошибка при ожидании завершения потока: " + e);
            }

        } while (quizMap.get(chatID).isQuizStarted);
        logger.info("Бесконечный цикл викторины для чата " + chatsService.getChatByID(chatID).getType() + " (id = " + chatID + ") завершен");
    }
    private void startClueUpdateThread(Long chatID) {
//        quizMap.get(chatID).currentQuestionThread = CompletableFuture.runAsync(() -> {
//            boolean questionEndFlag = false; //признак, того что вопрос завершен
//            while ((quizMap.get(chatID).isQuizStarted) & (!questionEndFlag)) {
//                try {
//                    for (int time = 0; time < quizClueTimer; time+=1000) {
//                        //Ждем время до отправки подсказки. Каждую секунду пишем лог
//                        logger.debug("В чате " + chatsService.getChatByID(chatID).getType() + " до следующей подсказки осталось: " + ((quizClueTimer-time)/1000) + " секунд");
//                        Thread.sleep(1000);
//                    }
//                    if (quizMap.get(chatID).getRemainingNumberOfClue() > remainingNumberOfClue) {
//                        //Если подсказки еще остались, то обновляем подсказку и отправляем повторно
//                        quizMap.get(chatID).updateClue();
//                        //Повторная отправка подсказки обновляет сообщение с подсказкой
//                        sendClue(chatID);
//                    } else {
//                        //Если подсказок больше не будет обновляем сообщение: отправляем верный ответ
//                        questionEndFlag = true;
//                        //TODO: добавить эмодзи в правильный ответ
////                        if (!bot.editMessage(chatID, quizMap.get(chatID).currentClueMessageID,"Правильный ответ: " + quizMap.get(chatID).getAnswer()))
//                        bot.editMessage(chatID, quizMap.get(chatID).currentClueMessageID,"Правильный ответ: " + quizMap.get(chatID).getAnswer());
////                            bot.sendMessage(chatID,"Правильный ответ: " + quizMap.get(chatID).getAnswer());
//                        quizMap.get(chatID).noAnswerCount++;
//                    }
//                } catch (InterruptedException e) {
//                    logger.debug("Отправка подсказок была прервана (Викторина завершена?)");
//                }
//            }
//            if (quizMap.get(chatID).noAnswerCount >= 3) {
//                quizMap.get(chatID).stopQuiz();
//                quizMap.get(chatID).endClueUpdateThread("Три вопроса подряд без верного ответа");
//            }
//        }, quizMap.get(chatID).executorClueUpdate);

        if (quizMap.get(chatID).currentQuestionThread != null && !quizMap.get(chatID).currentQuestionThread.isDone()) {
            logger.warn("Предыдущий поток с подсказками не был завершен корректно. Сработало принудительное завершение");
            quizMap.get(chatID).endClueUpdateThread("Запуск нового потока с подсказками");
        }

        quizMap.get(chatID).currentQuestionThread = quizMap.get(chatID).executorClueUpdate.submit(() -> {
            boolean questionEndFlag = false;
            while ((quizMap.get(chatID).isQuizStarted) & (!questionEndFlag)) {
                try {
                    for (int time = 0; time < quizClueTimer; time += 1000) {
                        if (Thread.interrupted()) {
                            throw new InterruptedException(); // Проверяем прерывание
                        }
                        logger.debug("В чате " + chatsService.getChatByID(chatID).getType() + " до следующей подсказки осталось: " + ((quizClueTimer - time) / 1000) + " секунд");
                        Thread.sleep(1000);
                    }
                    if (quizMap.get(chatID).getRemainingNumberOfClue() > remainingNumberOfClue) {
                        quizMap.get(chatID).updateClue();
                        sendClue(chatID);
                    } else {
                        questionEndFlag = true;
                        bot.editMessage(chatID, quizMap.get(chatID).currentClueMessageID, "Правильный ответ: " + quizMap.get(chatID).getAnswer());
                        quizMap.get(chatID).noAnswerCount++;
                    }
                } catch (InterruptedException e) {
                    logger.debug("Отправка подсказок была прервана");
                    break; // Выходим из цикла при прерывании
                }
            }
            if (quizMap.get(chatID).noAnswerCount >= 3) {
                quizMap.get(chatID).stopQuiz();
                quizMap.get(chatID).endClueUpdateThread("Три вопроса подряд без верного ответа");
            }
        }); // Сохраняем Future для последующей отмены

        logger.info("Количество активных потоков с отправкой подсказок: " + quizMap.get(chatID).executorClueUpdate.getActiveCount());
    }

}
