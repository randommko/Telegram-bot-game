package org.example.AiChat;

import org.example.Settings.SettingsService;
import org.example.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.Settings.Settings.*;

public class ConversationHistoryService {
    private static final Logger logger = LoggerFactory.getLogger(ConversationHistoryService.class);
    // Ключ: chatId, значение: Map, где ключ - userId, значение - история пользователя
    private final Map<Long, Map<Long, List<messageInChat>>> allChatsAllUsersMessages = new ConcurrentHashMap<>();
    private final SettingsService settings = new SettingsService();

    public record messageInChat(String role, String content, Long timestamp) {
        public messageInChat(String role, String content) {
            this(role, content, System.currentTimeMillis());
        }
    }
    public void addMessage (Message message, String role, String content) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        try {
            logger.debug("Добавление сообщения: chatName={}, userName={}, role={}, content={}",
                    chatId, getUserName(userId), role, content);

            // Проверяем и инициализируем для chatId если нужно
            if (!allChatsAllUsersMessages.containsKey(chatId)) {
                allChatsAllUsersMessages.put(chatId, new HashMap<>());
            }

            // Проверяем и инициализируем для userId если нужно
            if (!allChatsAllUsersMessages.get(chatId).containsKey(userId)) {
                logger.info("Не найдена история сообщений в чате {} для пользователя {}", chatId, userId);
                allChatsAllUsersMessages.get(chatId).put(userId, new ArrayList<>());
            }

            // Теперь безопасно добавляем сообщение
            if (userId.equals(0L))
                allChatsAllUsersMessages.get(chatId).get(userId).add(new messageInChat("system", getSystemPromt(chatId)));
            else
                allChatsAllUsersMessages.get(chatId).get(userId).add(new messageInChat(role, content));

            logger.info("В истории AI для чата {} для пользователя {} сохранено {} сообщений",
                    chatId,
                    userId,
                    allChatsAllUsersMessages.get(chatId).get(userId).size());
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
    public Map<Long, List<messageInChat>> getAllMessagesInChat(Long chatId) {
        return allChatsAllUsersMessages.getOrDefault(chatId, new ConcurrentHashMap<>());
    }
    public void clearAllHistory(Long chatId) {
        allChatsAllUsersMessages.remove(chatId);
    }
    public Integer getHistorySize(Long chatId) {
        int totalMessagesInChat = 0;
        Map<Long, List<messageInChat>> usersMessages = allChatsAllUsersMessages.get(chatId);

        // Проверяем, существует ли такой чат
        if (usersMessages != null) {
            // Проходим по каждому пользователю в чате
            for (List<messageInChat> messages : usersMessages.values()) {
                totalMessagesInChat += messages.size();
            }
        }

        return totalMessagesInChat;
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
