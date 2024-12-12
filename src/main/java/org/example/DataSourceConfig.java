package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceConfig {
    private static DataSource dataSource;

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static void initialize(String dbServerUrl, String dbUser, String dbPass) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbServerUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPass);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(100); // Максимальное количество соединений в пуле
        dataSource = new HikariDataSource(config);
    }


}