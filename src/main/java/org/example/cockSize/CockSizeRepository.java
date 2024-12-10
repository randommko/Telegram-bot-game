package org.example.cockSize;

import org.example.DataSourceConfig;
import org.example.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import static org.example.TablesDB.*;

public class CockSizeRepository {
    private final Logger logger = LoggerFactory.getLogger(CockSizeRepository.class);
    public void setCockSizeWinner (Long userID, Integer size) {
        LocalDate today = LocalDate.now();
        String insertQuery = "INSERT INTO " + COCKSIZE_STATS_TABLE + " (user_id, size, date) VALUES (?, ?, ?)";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setLong(1, userID);
                insertStmt.setInt(2, size);
                insertStmt.setDate(3, Date.valueOf(today));
                insertStmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Ошибка при записи в БД длинны члена: ", e);
        }
    }

    public String getCockSizeImageName(Integer size) {
        String selectQuery = "SELECT img FROM " + COCKSIZE_IMAGES_TABLE + " WHERE cock_size = ?";
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            try (PreparedStatement insertStmt = connection.prepareStatement(selectQuery)) {
                insertStmt.setInt(1, size);
                ResultSet resultImageSet = insertStmt.executeQuery();
                if (resultImageSet.next())
                    return resultImageSet.getString("img");
            }
        } catch (Exception e) {
            logger.error("Ошибка при записи в БД длинны члена: ", e);
        }
        return null;
    }

    public int getPlayerCockSize(Long userID) {
        LocalDate currentDate = LocalDate.now();
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            // Проверяем, есть ли запись для текущей даты
            String checkQuery = "SELECT size FROM " + COCKSIZE_STATS_TABLE + " WHERE user_id = ? AND date = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setLong(1, userID);
                checkStmt.setDate(2, Date.valueOf(currentDate));
                ResultSet resultSet = checkStmt.executeQuery();
                resultSet.next();
                return resultSet.getInt("size");
            }
        } catch (Exception e) {
            logger.error("Ошибка при поиске в БД длинны члена: ", e);
            return -1;
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
