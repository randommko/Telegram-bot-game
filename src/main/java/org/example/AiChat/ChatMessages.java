package org.example.AiChat;

import org.example.Settings.SettingsService;
import org.example.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.example.Settings.Settings.*;

public class ChatMessages {
    //Map - ID пользователя, список сообщений
    private final Map<Long, UserMessages> userMessages = new HashMap<>();
    private final Long chatId;
    private final static Long aiId = 0L;
    private final static String aiSystemRole = "system";
    private final static String aiAssistantRole = "assistant";

    private static final Logger logger = LoggerFactory.getLogger(ChatMessages.class);
    private final SettingsService settings = new SettingsService();

    public ChatMessages(Long chatId) {
        this.chatId = chatId;
        userMessages.put(aiId, new UserMessages(aiSystemRole));
        initAiInChat();
    }
    public void addMessage (Long userId, String role, String content) {
        logger.debug("Добавление сообщения: chatName={}, userName={}, role={}, content={}",
                chatId, getUserName(userId), role, content);

        if (!userMessages.containsKey(userId))
            userMessages.put(userId, new UserMessages(role));

        int userMessagesSize = userMessages.get(userId).saveMessage(content);

        logger.info("В истории AI для чата {} для пользователя {} сохранено {} сообщений",
                chatId,
                userId,
                userMessagesSize);
    }
    public List<UserMessages> getAllMessagesInChat() {
        //TODO: для всех пользователей в userMessages вернуть список сообщений объедененный в List и отсортированный
        return null;
    }
    public void  clearAllHistory() {
        //TODO: удалить все сообщения всех пользователей и сделать заново initAiInChat()
        initAiInChat();
    }
    public int getHistorySize() {
        //TODO: вернуть количество сообщений в userMessages
        return 0;
    }

    private String getSystemPromt() {
        String systemPrompt;

        try {
            if (Objects.equals(chatId, MY_CHAT_ID)) {
                systemPrompt = settings.getSettingValue(AI_CONTEXT_FOR_MY_CHAT);
                logger.info("Использован специальный промпт для Синий чат");
            } else {
                systemPrompt = settings.getSettingValue(AI_CONTEXT);
                logger.info("Использован промпт по умолчанию");
            }
        } catch (Exception e) {
            logger.error("Ошибка применения стартового ПРОМТа: {}", String.valueOf(e));
            return "Ты универсальный помощник";
        }
        return systemPrompt;
    }
    private void initAiInChat() {
        String startPromt = getSystemPromt();
        addMessage(aiId, aiSystemRole, startPromt);
    }
    private String getUserName(Long userId) {
        String userName;
        UsersService usersService = new UsersService();
        if (userId == 0L)
            userName = "AI Bot";
        else
            userName = usersService.getUserNameByID(userId);
        return userName;
    }

}
