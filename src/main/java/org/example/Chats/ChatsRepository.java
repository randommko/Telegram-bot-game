package org.example.Chats;

import org.example.DataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.example.TablesDB.TG_CHATS_TABLE;
import static org.example.TablesDB.TG_USERS_TABLE;


public class ChatsRepository {
    private final Logger logger = LoggerFactory.getLogger(ChatsRepository.class);

    public void insertChatInDB(Long chatID, String chatTitle) {
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String insertUserQuery = "INSERT INTO " + TG_CHATS_TABLE + " (chat_id, chat_title) VALUES (?, ?)";
            try (PreparedStatement insertUser = connection.prepareStatement(insertUserQuery)) {
                insertUser.setLong(1, chatID);
                insertUser.setString(2, chatTitle);
                insertUser.executeQuery();
            }
        } catch (Exception e) {
            logger.error("Ошибка при сохранении чата в БД: ", e);
        }
    }

    public String getChatTitleByID(Long chatID) {
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String userNameQuery = "SELECT chat_title FROM " + TG_CHATS_TABLE + " WHERE chat_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(userNameQuery)) {
                checkStmt.setLong(1, chatID);
                ResultSet resultSet = checkStmt.executeQuery();
                resultSet.next();
                return resultSet.getString("chat_title");
            }
        } catch (Exception e) {
            logger.error("Ошибка при поиске в БД чата: ", e);
            return null;
        }
    }
}