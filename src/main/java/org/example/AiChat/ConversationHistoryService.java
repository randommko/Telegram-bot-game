package org.example.AiChat;

import org.example.Chats.ChatsService;
import org.example.Settings.SettingsService;
import org.example.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.Settings.Settings.*;

public class ConversationHistoryService {
    private static final Logger logger = LoggerFactory.getLogger(ConversationHistoryService.class);
    // Ключ: chatId, значение: Map, где ключ - userId, значение - история пользователя
    private final Map<Long, Map<Long, List<Message>>> allChatsAllUsersMessages = new ConcurrentHashMap<>();
    private final SettingsService settings = new SettingsService();
    private static final Long systemPromtUserId = 0L;

    public record Message(String role, String content, Long timestamp) {
        public Message(String role, String content) {
            this(role, content, System.currentTimeMillis());
        }
    }

    public void addMessage(Long chatId, Long userId, String role, String content) {
        try {
            logger.debug("Добавление сообщения: chatName={}, userName={}, role={}, content={}",
                    getChatTitle(chatId, userId), getUserName(userId), role, content);
            List<Message> userMessages = getAllMessagesInChat(chatId);
            userMessages.add(new Message(role, content));
            logger.info("В истории для AI чата {} сохранено {} сообщений", getChatTitle(chatId, userId), userMessages.size());
        }
        catch (Exception e) {
            logger.error("Ошибка сохранения сообщения в историю AI: {}", e.toString());
        }
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
    private String getChatTitle(Long chatId, Long userId) {
        ChatsService chatsService = new ChatsService();
        String chatTitle = chatsService.getChatTitle(chatId);
        if (chatTitle == null)
            chatTitle = "Личный чат с: " + getUserName(userId);
        return chatTitle;
    }
    public Map<Long, List<Message>> getAllUsersInChat(Long chatId) {
        return allChatsAllUsersMessages.getOrDefault(chatId, new ConcurrentHashMap<>());
    }
    public void clearAllHistory(Long chatId) {
        allChatsAllUsersMessages.remove(chatId);
    }
    public Integer getHistorySize(Long chatId) {
        return  allChatsAllUsersMessages.get(chatId).size();
    }





    public List<Message> getAllMessagesInChat(Long chatId) {
        // Получаем или создаем историю для чата
        Map<Long, List<Message>> usersMessages = allChatsAllUsersMessages.get(chatId);

        // Если истории нет, инициализируем чат
        if (usersMessages == null || usersMessages.isEmpty()) {
            usersMessages = initChat(chatId);
        }

        // Собираем все сообщения от всех пользователей
        List<Message> allMessages = new ArrayList<>();

        for (List<Message> userMessages : usersMessages.values()) {
            if (userMessages != null && !userMessages.isEmpty()) {
                allMessages.addAll(userMessages);
            }
        }

        // Сортируем по времени (от старых к новым)
        allMessages.sort(Comparator.comparing(Message::timestamp));

        return allMessages;
    }





    private Map<Long, List<Message>> initChat(Long chatId) {
        // Инициализируем историю чата, если её нет
        return allChatsAllUsersMessages.computeIfAbsent(chatId, k -> {
            ChatsService chatsService = new ChatsService();
            logger.info("Создана новая история для чата: {}", chatsService.getChatTitle(chatId));

            // Создаем новую мапу для пользователей и сразу инициализируем системное сообщение
            Map<Long, List<Message>> usersMessages = new ConcurrentHashMap<>();

            // Добавляем системное сообщение для пользователя 0L
            List<Message> systemMessages = new ArrayList<>();
            systemMessages.add(new Message("system", getSystemPromt(chatId)));
            usersMessages.put(0L, systemMessages);

            logger.info("Создана новая история для пользователя AI в чате {}",
                    chatsService.getChatTitle(chatId));

            return usersMessages;
        });
    }

    private String getSystemPromt (Long chatId) {
        String systemPrompt;

        if (Objects.equals(chatId, MY_CHAT_ID)) {
            systemPrompt = settings.getSettingValue(AI_CONTEXT_FOR_MY_CHAT);
            logger.info("Использован специальный промпт для Синий чат");
        } else {
            systemPrompt = settings.getSettingValue(AI_CONTEXT);
            logger.info("Использован промпт по умолчанию");
        }

        return systemPrompt;
    }

}
