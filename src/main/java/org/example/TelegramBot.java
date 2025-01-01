package org.example;

import com.vdurmont.emoji.EmojiParser;
import org.example.Chats.ChatsService;
import org.example.CockSize.CockSizeGame;
import org.example.Horoscope.HoroscopeService;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.*;


public class TelegramBot extends TelegramLongPollingBot {
    private final String botToken;
    private static TelegramBot instance = null;
    private final UsersService usersService = new UsersService();
    private final ChatsService chatsService = new ChatsService();
    private final CockSizeGame cockSizeGame;
    private final PidorGame pidorGame;
    private final QuizGame quizGame;
    private final HoroscopeService horoscopeService;
    private static Map<Long, LocalDate> usersUpdateTime = new HashMap<>();
    private static Map<Long, LocalDate> chatsUpdateTime = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    public TelegramBot(String botToken) {
        this.botToken = botToken;
        instance = this;
        cockSizeGame = new CockSizeGame();
        pidorGame = new PidorGame();
        quizGame = new QuizGame();
        horoscopeService = new HoroscopeService();
        //TODO: добавить отправку различных сообщений по CRON
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
        if (update.hasCallbackQuery()) {
            executeCallback(update);
            return;
        }

        Message message = update.getMessage();

        if (message == null) {
            logger.debug("Пустое сообщение (например, событие, связанное с вызовами, действиями пользователей, или callback-запросами)");
            return;
        }
        checkUser(message);
        checkChat(message);
        if (message.getText() == null) {
            logger.debug("Сообщение не содержит текста");
            return;
        }

        logger.debug("Получено сообщение из чата " + message.getChat().getId().toString() +": "+ message.getText());
        executeMessage(update);
    }

    private void checkUser(Message message) {
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
    }
    private void checkChat(Message message) {
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
    }
    public static TelegramBot getInstance() {
        return instance;
    }
    private void botInfo(Message message) {
        Long chatID = message.getChatId();
        sendMessage(chatID, """
                 Бот для развлечений! Команды:
                 /cocksize - Измерить достоинство
                 /horoscope_today - Гороскоп
                 /quiz_start - Запустить викторину
                 /quiz_stop - Остановить викторину
                 /quiz_stats - Статистика викторина
                 /pidor_start - Выбрать победителя
                 /pidor_reg - Вступить в игру
                 /pidor_stats - Статистика""");
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
    private void executeMessage(Update update) {
        Message message = update.getMessage();
        if (update.hasMessage()) {
            String[] parts = message.getText().split(" ", 2); // Разделяем строку по первому пробелу

            /*
            parts[0] - команда
            parts[1] - параметр (сейчас не используется)
             */

            String command = parts[0];

            switch (command) {
                case "/bot_info", "/bot_info@ChatGamePidor_Bot", "/help", "/help@ChatGamePidor_Bot" -> botInfo(message);
                case "/cocksize", "/cocksize@ChatGamePidor_Bot" -> cockSizeGame.cockSizeStart(message);
                case "/pidor_reg", "/pidor_reg@ChatGamePidor_Bot" -> pidorGame.registerPlayer(message.getChatId(), message.getFrom().getId());
                case "/pidor_stats", "/pidor_stats@ChatGamePidor_Bot" -> pidorGame.sendPidorStats(message.getChatId());
                case "/pidor_start", "/pidor_start@ChatGamePidor_Bot" -> pidorGame.startPidorGame(message.getChatId());
                case "/quiz_start", "/quiz_start@ChatGamePidor_Bot" -> quizGame.startQuizGame(message);
                case "/quiz_stop", "/quiz_stop@ChatGamePidor_Bot" -> quizGame.stopQuiz(message.getChatId());
                case "/quiz_stats", "/quiz_stats@ChatGamePidor_Bot" -> quizGame.getQuizStats(message);
                case "/horoscope_today", "/horoscope_today@ChatGamePidor_Bot" -> sendInlineKeyboard(message.getChatId());
                default -> quizGame.checkQuizAnswer(message);
            }
        }
    }
    private void sendInlineKeyboard(Long chatID) {
        // Создаем кнопки
        InlineKeyboardButton ariesButton = new InlineKeyboardButton();
        ariesButton.setText(EmojiParser.parseToUnicode(":aries: Овен"));
        ariesButton.setCallbackData("aries_button_pressed");

        InlineKeyboardButton taurusButton = new InlineKeyboardButton();
        taurusButton.setText(EmojiParser.parseToUnicode(":taurus: Телец"));
        taurusButton.setCallbackData("taurus_button_pressed");

        InlineKeyboardButton geminiButton = new InlineKeyboardButton();
        geminiButton.setText(EmojiParser.parseToUnicode(":gemini: Близнецы"));
        geminiButton.setCallbackData("gemini_button_pressed");

        InlineKeyboardButton cancerButton = new InlineKeyboardButton();
        cancerButton.setText(EmojiParser.parseToUnicode(":cancer: Рак"));
        cancerButton.setCallbackData("cancer_button_pressed");

        InlineKeyboardButton leoButton = new InlineKeyboardButton();
        leoButton.setText(EmojiParser.parseToUnicode(":leo: Лев"));
        leoButton.setCallbackData("leo_button_pressed");

        InlineKeyboardButton virgoButton = new InlineKeyboardButton();
        virgoButton.setText(EmojiParser.parseToUnicode(":virgo: Дева"));
        virgoButton.setCallbackData("virgo_button_pressed");

        InlineKeyboardButton libraButton = new InlineKeyboardButton();
        libraButton.setText(EmojiParser.parseToUnicode(":libra: Весы"));
        libraButton.setCallbackData("libra_button_pressed");

        InlineKeyboardButton scorpioButton = new InlineKeyboardButton();
        scorpioButton.setText(EmojiParser.parseToUnicode(":scorpius: Скорпион"));
        scorpioButton.setCallbackData("scorpio_button_pressed");

        InlineKeyboardButton sagittariusButton = new InlineKeyboardButton();
        sagittariusButton.setText(EmojiParser.parseToUnicode(":sagittarius: Стрелец"));
        sagittariusButton.setCallbackData("sagittarius_button_pressed");

        InlineKeyboardButton capricornButton = new InlineKeyboardButton();
        capricornButton.setText(EmojiParser.parseToUnicode(":capricorn: Козерог"));
        capricornButton.setCallbackData("capricorn_button_pressed");

        InlineKeyboardButton aquariusButton = new InlineKeyboardButton();
        aquariusButton.setText(EmojiParser.parseToUnicode(":aquarius: Водолей"));
        aquariusButton.setCallbackData("aquarius_button_pressed");

        InlineKeyboardButton piscesButton = new InlineKeyboardButton();
        piscesButton.setText(EmojiParser.parseToUnicode(":pisces: Рыбы"));
        piscesButton.setCallbackData("pisces_button_pressed");

        // Создаем ряды кнопок
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(ariesButton);
        row1.add(taurusButton);
        row1.add(geminiButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(cancerButton);
        row2.add(leoButton);
        row2.add(virgoButton);


        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(libraButton);
        row3.add(scorpioButton);
        row3.add(sagittariusButton);


        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(capricornButton);
        row4.add(aquariusButton);
        row4.add(piscesButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        // Устанавливаем кнопки в сообщение
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatID.toString());
        message.setText("Выберите знак зодиака:");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void executeCallback(Update update) {
        String callbackData = update.getCallbackQuery().getData();

        // Обрабатываем нажатие inline кнопки
        switch (callbackData) {
            case "aries_button_pressed" -> horoscopeService.sendHoroscope(update, "aries", "today");
            case "taurus_button_pressed" -> horoscopeService.sendHoroscope(update, "taurus", "today");
            case "gemini_button_pressed" -> horoscopeService.sendHoroscope(update, "gemini", "today");
            case "cancer_button_pressed" -> horoscopeService.sendHoroscope(update, "cancer", "today");
            case "leo_button_pressed" -> horoscopeService.sendHoroscope(update, "leo", "today");
            case "virgo_button_pressed" -> horoscopeService.sendHoroscope(update, "virgo", "today");
            case "libra_button_pressed" -> horoscopeService.sendHoroscope(update, "libra", "today");
            case "scorpio_button_pressed" -> horoscopeService.sendHoroscope(update, "scorpio", "today");
            case "sagittarius_button_pressed" -> horoscopeService.sendHoroscope(update, "sagittarius", "today");
            case "capricorn_button_pressed" -> horoscopeService.sendHoroscope(update, "capricorn", "today");
            case "aquarius_button_pressed" -> horoscopeService.sendHoroscope(update, "aquarius", "today");
            case "pisces_button_pressed" -> horoscopeService.sendHoroscope(update, "pisces", "today");
            default -> logger.debug("Ошибка работы inline кнопок");
        }
    }

}
