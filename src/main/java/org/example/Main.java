package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Ошибка: Токен бота не передан.");
            System.exit(1);
        }

        String botToken = args[0];
        String DB_SERVER_URL = args[1];
        String DB_USER = args[2];
        String DB_PASS = args[3];
        final DataSourceConfig dataSourceConfig = new DataSourceConfig(DB_SERVER_URL, DB_USER, DB_PASS);

        try {
            // Создаем объект TelegramBotsApi
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Регистрируем бота
            botsApi.registerBot(new GameBot(botToken, dataSourceConfig));

            System.out.println("Бот успешно запущен!");
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при запуске бота: " + e.getMessage());
            e.printStackTrace();
        }
    }
}