package org.example.AiChat;

import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import org.example.Chats.ChatsService;
import org.example.DataSourceConfig;
import org.example.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.example.Settings.TablesDB.*;

public class ContextRepo {
    private static final Logger logger = LoggerFactory.getLogger(ContextRepo.class);
    private final UsersService usersService = new UsersService();
    private final ChatsService chatsService = new ChatsService();
    public void saveContext(Message message) {
        Long chatId = message.getChatId();
        Long authorId = message.getFrom().getId();
        String text = message.getText();

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String sql = "INSERT INTO " + MESSAGE_HISTORY_TABLE + " (chat_id, user_id, message_text) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setLong(2, authorId);
                stmt.setString(3, text);
                logger.debug("SQL сохранения сообщения: " + sql);
                stmt.executeUpdate();
            }
            logger.debug("Сохранено сообщение: " + chatsService.getChatByID(chatId).getTitle() + "\n" +
                    "от пользователя: " + usersService.getUserNameByID(authorId) + "\n" +
                    "текст: " + text);
        } catch (Exception e) {
            logger.error("Произошла ошибка при сохранении цитаты в БД: ", e);
        }
    }
    public List<ChatMessage> getChatContext(Long chatId, int maxMessages) {
        // Берём последние N сообщений
        List<ChatMessage> history = new ArrayList<>();
        String query = String.format("""
                SELECT user_id, message_text, message_date
                FROM %s
                WHERE chat_id = ?
                ORDER BY message_date DESC
                LIMIT ?;
                """, MESSAGE_HISTORY_TABLE);

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setLong(1, chatId);
            preparedStatement.setInt(2, maxMessages);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Long userId = resultSet.getLong("user_id");
                String text = resultSet.getString("message_text");
                String userName = usersService.getUserNameByID(userId);

                String content = userName + ": " + text;

                history.add(ChatMessage.builder()
                        .role(ChatMessageRole.USER)
                        .content(content)
                        .build());
            }
            logger.debug("Загружен контекст чата {}: {} сообщений", chatId, history.size());
            return history;

        } catch (Exception e) {
            logger.error("Ошибка получения настройки: " + e);
            return null;
        }
    }
}
