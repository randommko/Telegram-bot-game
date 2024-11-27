package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Ошибка: Токен бота не передан.");
            System.exit(1);
        }

        DataSourceConfig.initialize(args[1], args[2], args[3]);

        try {
            // Создаем объект TelegramBotsApi
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Регистрируем бота
            botsApi.registerBot(new GameBot(args[0]));

            System.out.println("Бот успешно запущен!");
            System.out.println("запущено локальное приложение!");
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при запуске бота: " + e.getMessage());
            logger.error("Ошибка при запуске бота: ", e);
        }
    }
}