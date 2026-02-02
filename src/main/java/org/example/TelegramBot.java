package org.example;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.Scope;
import org.example.AiChat.AiChat;
import org.example.AiChat.ContextService;
import org.example.Chats.ChatsService;
import org.example.CockSize.CockSizeGame;
import org.example.Horoscope.HoroscopeService;
import org.example.PidorGame.PidorGame;
import org.example.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    private final String botToken;
    private static TelegramBot instance;

    // Зависимости
    private final MessageSender messageSender;
    private final UserChatManager userChatManager;
    private final CommandDispatcher commandDispatcher;
    private final CallbackDispatcher callbackDispatcher;
    private final ContextService contextService;
    private final ThreadPoolExecutor executor;


    public TelegramBot(String botToken, String aiToken) {
        this.botToken = botToken;
        instance = this;

        // Инициализация сервисов
        UsersService usersService = new UsersService();
        ChatsService chatsService = new ChatsService();
        CockSizeGame cockSizeGame = new CockSizeGame();
        PidorGame pidorGame = new PidorGame();
        HoroscopeService horoscopeService = new HoroscopeService();
        AiChat aiChat = new AiChat();
        GigaChatClient aiClient = initAiClient(aiToken); //TODO: Вынесите в отдельный метод

        // Фабрики диспетчеров
        this.messageSender = new MessageSender(this);
        this.userChatManager = new UserChatManager(usersService, chatsService);
        this.contextService = new ContextService();
        this.commandDispatcher = new CommandDispatcher(messageSender, cockSizeGame,
                pidorGame, horoscopeService, aiChat, contextService);
        this.callbackDispatcher = new CallbackDispatcher(
                messageSender, horoscopeService, cockSizeGame);
        this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    private GigaChatClient initAiClient(String aiToken) {
        return GigaChatClient.builder()
                .verifySslCerts(false)
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .authKey(aiToken)
                                .scope(Scope.GIGACHAT_API_PERS)
                                .build())
                        .build())
                .build();
    }

    @Override
    public String getBotUsername() {
        return "Викторина бот";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        CompletableFuture.runAsync(() -> processUpdate(update), executor);
        logger.info("Активных потоков: {}", executor.getActiveCount());
    }

    private void processUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            var callback = update.getCallbackQuery();
            userChatManager.checkUser(callback.getFrom());
            userChatManager.checkChat(callback.getMessage().getChat());
            callbackDispatcher.dispatch(callback);
            return;
        }

        Message message = update.getMessage();
        if (message == null || message.getText() == null) {
            logger.debug("Пустое или не-текстовое сообщение");
            return;
        }

        logger.debug("Сообщение из {}: {}", message.getChat().getId(), message.getText());

        userChatManager.checkUser(message.getFrom());
        userChatManager.checkChat(message.getChat());

        commandDispatcher.dispatch(update);
    }

    public static TelegramBot getInstance() {
        return instance;
    }

    // Геттеры для доступа из других классов (уберите singleton позже)
    public MessageSender getMessageSender() { return messageSender; }
    public UserChatManager getUserChatManager() { return userChatManager; }
}
