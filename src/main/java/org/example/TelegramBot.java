package org.example;

import org.example.Chats.ChatsService;
import org.example.CockSize.CockSizeGame;
import org.example.PidorGame.PidorGame;
import org.example.QuizGame.QuizGame;
import org.example.Users.UsersService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class TelegramBot extends TelegramLongPollingBot {
    private final String botToken;
    private static TelegramBot instance = null;
    private final UsersService usersService = new UsersService();
    private final ChatsService chatsService = new ChatsService();
    private final CockSizeGame cockSizeGame;
    private final PidorGame pidorGame;
    private final QuizGame quizGame;
    private static Map<Long, LocalDate> usersUpdateTime = new HashMap<>();
    private static Map<Long, LocalDate> chatsUpdateTime = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    public TelegramBot(String botToken) {
        this.botToken = botToken;
        instance = this;
        cockSizeGame = new CockSizeGame();
        pidorGame = new PidorGame();
        quizGame = new QuizGame();
    }
    @Override
    public String getBotUsername() {
        return "Викторина бот"; // Замените на имя вашего бота
    }
    @Override
    public String getBotToken() {
        return botToken;
    }
    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();

        if (message == null) {
            logger.debug("Пустое сообщение (например, событие, связанное с вызовами, действиями пользователей, или callback-запросами)");
            return;
        }

        if (!usersService.checkUser(message.getFrom())) {
            usersService.addUser(message.getFrom());
            usersUpdateTime.put(message.getFrom().getId(), LocalDate.now());
        }
        else {
            if (!Objects.equals(usersUpdateTime.get(message.getFrom().getId()), LocalDate.now())) {
                usersService.updateUser(message.getFrom());
                usersUpdateTime.put(message.getFrom().getId(), LocalDate.now());
            }
        }

        if (!chatsService.checkChat(message.getChatId())) {
            chatsService.addChat(message.getChat());
            chatsUpdateTime.put(message.getFrom().getId(), LocalDate.now());
        }
        else {
            if (!Objects.equals(chatsUpdateTime.get(message.getFrom().getId()), LocalDate.now())) {
                chatsService.updateChat(message.getChat());
                chatsUpdateTime.put(message.getFrom().getId(), LocalDate.now());
            }
        }


//        if (!chatsService.checkChat(message.getChatId()))
//            chatsService.addChat(message.getChat());


        logger.debug("Получено сообщение из чата " + message.getChat().getId().toString() +": "+ message.getText());
        if (update.hasMessage()) {
            String command = message.getText();
            switch (command) {
                //TODO: добавить гороскоп
                case "/bot_info", "/bot_info@ChatGamePidor_Bot", "/help", "/help@ChatGamePidor_Bot" -> botInfo(message);
                case "/cocksize", "/cocksize@ChatGamePidor_Bot" -> cockSizeGame.cockSizeStart(message);
                case "/pidor_reg", "/pidor_reg@ChatGamePidor_Bot" -> pidorGame.registerPlayer(message.getChatId(), message.getFrom().getId());
                case "/pidor_stats", "/pidor_stats@ChatGamePidor_Bot" -> pidorGame.sendPidorStats(message.getChatId());
                case "/pidor_start", "/pidor_start@ChatGamePidor_Bot" -> pidorGame.startPidorGame(message.getChatId());
                case "/quiz_start", "/quiz_start@ChatGamePidor_Bot" -> quizGame.startQuizGame(message);
                case "/quiz_stop", "/quiz_stop@ChatGamePidor_Bot" -> quizGame.stopQuiz(message.getChatId());
                case "/quiz_stats", "/quiz_stats@ChatGamePidor_Bot" -> quizGame.getQuizStats(message);
                default -> quizGame.checkQuizAnswer(message);
            }
        }
    }
    public static TelegramBot getInstance() {
        return instance;
    }
    private void botInfo(Message message) {
        Long chatID = message.getChatId();
        sendMessage(chatID, """
                 Этот бот создан для определения пидора дня в чате! Команды:
                 /cocksize - Измерить причиндалы
                 /quiz_start - Запустить викторину
                 /quiz_stop - Остановить викторину
                 /quiz_stats - Статистика викторина
                 /pidor_start - Найти пидора дня
                 /pidor_reg - Добавиться в игру поиска пидоров
                 /pidor_stats - Статистика пидоров""");
    }
    public Integer sendMessage(Long chatID, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatID);
        message.setText(text);
        try {
            return execute(message).getMessageId();
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
        }
        return null;
    }
    public Integer sendReplyMessage(Long chatId, Integer replyMessageID, String messageText) {
        SendMessage response = new SendMessage();
        response.setChatId(chatId);
        response.setText(messageText);
        response.setReplyToMessageId(replyMessageID); // Привязываем к конкретному сообщению

        try {
            return execute(response).getMessageId();
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
            return null;
        }
    }
    public boolean sendImgMessage (Long chatId, String text, File imageFile) {

        if (imageFile.exists()) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(new InputFile(imageFile));
            sendPhoto.setCaption(text);

            try {
                execute(sendPhoto);
                return true;
            } catch (TelegramApiException e) {
                logger.error("Ошибка при отправке сообщения: ", e);
            }
        } else {
            logger.error("Image file not found: " + imageFile.getPath());
            return false;
        }
        return false;
    }
    public boolean editMessage(Long chatId, Integer messageID, String newMessageText) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageID);
        message.setText(newMessageText);
        try {
            execute(message);
            return true;
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
            return false;
        }
    }
    public Boolean checkAccessPrivileges(Message message) {
        Long chatID = message.getChatId();
        String chatType = message.getChat().isGroupChat() ? "Group" : message.getChat().isSuperGroupChat() ? "Supergroup" : message.getChat().isChannelChat() ? "Channel" : "Private";

        switch (chatType) {
            case "Private":
                logger.debug("Message was sent in a private chat.");
                return true;
            case "Group", "Supergroup":
                logger.debug("Message was sent in a group chat.");
                try {
                    GetChatMember getChatMember = new GetChatMember();
                    getChatMember.setChatId(chatID);
                    getChatMember.setUserId(this.execute(new GetMe()).getId());

                    ChatMember chatMember = execute(getChatMember);
                    return Objects.equals(chatMember.getStatus(), "administrator");

                } catch (Exception e) {
                    logger.debug("Ошибка проверки прав доступа у бота: " + e);
                    return false;
                }
            case "Channel":
                logger.debug("Message was sent in a channel.");
                return false;
            default:
                logger.debug("Unknown chat type.");
        }
        return false;
    }


}
