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
        System.out.println("asd");

        /*
        args[0] - токен бота
        args[1] - IP БД
        args[2] - логин БД
        args[3] - пароль БД
        */

        try {
            DataSourceConfig.initialize(args[1], args[2], args[3]);
        } catch (Exception e) {
            logger.error("Ошибка при подключении к БД: ", e);
        }

        int maxRetries = 3; // Количество попыток
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                attempt++;
                // Создаем объект TelegramBotsApi
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

                // Регистрируем бота
                botsApi.registerBot(new TelegramBot(args[0]));

                logger.info("Бот успешно запущен на попытке №" + attempt);
                break; // Выход из цикла при успешном запуске
            } catch (TelegramApiException e) {
                logger.error("Ошибка при запуске бота на попытке №" + attempt, e);

                if (attempt >= maxRetries) {
                    logger.error("Бот не запустился после " + maxRetries + " попыток.");
                } else {
                    logger.info("Повторная попытка запуска...");
                }
            }
        }
    }
}