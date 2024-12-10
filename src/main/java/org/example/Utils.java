package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.*;

import static org.example.TablesDB.*;

public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static void insertChatInDB(Message message) {
        Long chatID = message.getChatId();
        String chatTitle = message.getChat().getTitle();
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

    public static void insertUserInDB(Message message) {
        Long userID = message.getFrom().getId();
        String userName = message.getFrom().getUserName();

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String insertUserQuery = "INSERT INTO " + TG_USERS_TABLE + " (user_id, user_name) VALUES (?, ?)";
            try (PreparedStatement insertUser = connection.prepareStatement(insertUserQuery)) {
                insertUser.setLong(1, userID);
                insertUser.setString(2, "@" + userName);
                insertUser.executeQuery();
            }
        } catch (Exception e) {
            logger.error("Ошибка при сохранении пользователя в БД: ", e);
        }
    }








}
