package org.example.Settings;

import org.example.DataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.example.Settings.TablesDB.SETTINGS_TABLE;


public class SettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);
    public String getSettingValue(String key) {
        String query = "SELECT value " +
                "FROM " + SETTINGS_TABLE +
                " WHERE setting_key = ? ";

        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, key);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next())
                return resultSet.getString("value");
        } catch (Exception e) {
            logger.error("Ошибка получения настройки: {}", String.valueOf(e));
            return null;
        }
        return null;
    }
}
