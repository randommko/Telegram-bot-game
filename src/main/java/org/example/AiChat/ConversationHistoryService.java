package org.example.AiChat;

import org.example.Settings.SettingsService;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.Settings.Settings.*;

public class ConversationHistoryService {
    private static ConversationHistoryService instance;
    private static final Logger logger = LoggerFactory.getLogger(ConversationHistoryService.class);
    // Ключ: chatId, значение: Map, где ключ - userId, значение - история пользователя
    private final Map<Long, Map<Long, List<Message>>> userHistory = new ConcurrentHashMap<>();
    private final SettingsService settings = new SettingsService();

    public record Message(String role, String content, Long timestamp) {
        public Message(String role, String content) {
            this(role, content, System.currentTimeMillis());
        }
    }

    public void addMessage(Long chatId, Long userId, String role, String content) {
        logger.debug("Добавление сообщения: chatId={}, userId={}, role={}, content={}",
                chatId, userId, role, content);

        // Получаем или создаем историю для чата и пользователя
        Map<Long, List<Message>> chatHistory = userHistory.computeIfAbsent(chatId, k -> {
            logger.info("Создана новая история для чата: {}", chatId);
            return new ConcurrentHashMap<>();
        });

        List<Message> userMessages = chatHistory.computeIfAbsent(userId, k -> {
            logger.info("Создана новая история для пользователя {} в чате {}", userId, chatId);
            List<Message> messages = new ArrayList<>();
            String systemPrompt;
            if (Objects.equals(chatId, MY_CHAT_ID)) {
                systemPrompt = settings.getSettingValue(AI_CONTEXT_FOR_MY_CHAT);
                logger.info("Использован специальный промпт для MY_CHAT");
            } else {
                systemPrompt = settings.getSettingValue(AI_CONTEXT);
                logger.info("Использован стандартный промпт");
            }
            messages.add(new Message("system", systemPrompt));
            return messages;
        });

        // Добавляем новое сообщение пользователя
        userMessages.add(new Message(role, content));

        // Проверяем, что сообщение добавилось
        logger.debug("Теперь в истории пользователя {} сообщений", userMessages.size());

        // Для дополнительной проверки выведем последнее сообщение
        Message lastMessage = userMessages.get(userMessages.size() - 1);
        logger.debug("Последнее сообщение: role={}, content={}", lastMessage.role(), lastMessage.content());
    }

    public List<Message> getHistory(Long chatId, Long userId) {
        return userHistory.getOrDefault(chatId, new ConcurrentHashMap<>())
                .getOrDefault(userId, new ArrayList<>());
    }

    public Map<Long, List<Message>> getAllUsersInChat(Long chatId) {
        return userHistory.getOrDefault(chatId, new ConcurrentHashMap<>());
    }

    public void clearHistory(Long chatId, Long userId) {
        Map<Long, List<Message>> chatUsers = userHistory.get(chatId);
        if (chatUsers != null) {
            chatUsers.remove(userId);
            // Если в чате больше нет пользователей с историей, удаляем и чат
            if (chatUsers.isEmpty()) {
                userHistory.remove(chatId);
            }
        }
    }

    public void clearAllHistory(Long chatId) {
        userHistory.remove(chatId);
    }
    public static ConversationHistoryService getInstance() {
        return instance;
    }

}
