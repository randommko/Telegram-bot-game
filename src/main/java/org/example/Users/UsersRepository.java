package org.example.Users;

import org.example.Chats.ChatsRepository;
import org.example.DataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.example.TablesDB.TG_USERS_TABLE;

public class UsersRepository {
    private final Logger logger = LoggerFactory.getLogger(UsersRepository.class);

    public void insertUserInDB(String userName, Long userID) {
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

    public String getUserNameByID(Long userID) {
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String userNameQuery = "SELECT user_name FROM " + TG_USERS_TABLE + " WHERE user_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(userNameQuery)) {
                checkStmt.setLong(1, userID);
                ResultSet resultSet = checkStmt.executeQuery();
                resultSet.next();
                return resultSet.getString("user_name");
            }
        } catch (Exception e) {
            logger.error("Ошибка при поиске в БД длинны члена: ", e);
            return null;
        }
    }
}
