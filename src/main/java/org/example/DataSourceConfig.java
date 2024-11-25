package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceConfig {
    private static DataSource dataSource;


    public DataSourceConfig(String DB_SERVER_URL, String DB_USER, String DB_PASS) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_SERVER_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASS);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10); // Максимальное количество соединений в пуле

        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}