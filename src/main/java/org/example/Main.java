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
            logger.error("Ошибка: Токен бота не передан.");
            System.exit(1);
        }

        try {
            DataSourceConfig.initialize(args[1], args[2], args[3]);
        } catch (Exception e) {
            logger.error("Ошибка при подключении к БД: ", e);
        }

        try {
            // Создаем объект TelegramBotsApi
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Регистрируем бота
            botsApi.registerBot(new TelegramBot(args[0]));

            logger.info("Бот успешно запущен!");
        } catch (TelegramApiException e) {
            logger.error("Ошибка при запуске бота: ", e);
        }
    }
}