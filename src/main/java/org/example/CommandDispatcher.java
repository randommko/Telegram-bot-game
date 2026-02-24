package org.example;


import org.example.AiChat.AiService;
import org.example.CockSize.CockSizeGame;

import org.example.PidorGame.PidorGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CommandDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(CommandDispatcher.class);

    private final MessageSender messageSender;
    private final CockSizeGame cockSizeGame;
    private final PidorGame pidorGame;
    private final AiService aiService;
    private final ConversationHistoryService conversationHistoryService;
    private final KeyboardBuilder keyboardBuilder = new KeyboardBuilder();

    // Enum для команд (расширяемо)
    public enum Command {
        BOT_INFO("/bot_info", "/help"),
        COCKSIZE("/cocksize"),
        PIDOR_REG("/pidor_reg"),
        PIDOR_STATS("/pidor_stats"),
        PIDOR_START("/pidor_start"),
        HOROSCOPE("/horoscope_today"),
        AI("/ai", "/ии", "/аи", "/юра", "/юрий", "/юрец"),
        SUMMARY("/summary"),
        AI_CLEAR_HISTORY("/clear");

        private final List<String> aliases;

        Command(String... aliases) {
            this.aliases = List.of(aliases);
        }

        public static Command fromString(String cmd) {
            String cleanCmd = cmd.toLowerCase().trim();
            for (Command command : values()) {
                if (command.aliases.stream().anyMatch(alias ->
                        cleanCmd.startsWith(alias.toLowerCase()) ||
                                cleanCmd.equals(alias.replace("/", "").toLowerCase()))) {
                    return command;
                }
            }
            return null;
        }
    }

    // EnumMap для диспетчеризации
    private final Map<Command, Consumer<Message>> handlers = Map.of(
            Command.BOT_INFO, this::handleBotInfo,
            Command.COCKSIZE, this::handleCockSize,
            Command.PIDOR_REG, this::handlePidorReg,
            Command.PIDOR_STATS, this::handlePidorStats,
            Command.PIDOR_START, this::handlePidorStart,
            Command.HOROSCOPE, this::handleHoroscope,
            Command.AI, this::handleAi,
            Command.AI_CLEAR_HISTORY, this::handleAiChatHistory
    );

    public CommandDispatcher(MessageSender messageSender,
                             CockSizeGame cockSizeGame,
                             PidorGame pidorGame,
                             AiService aiService,
                             ConversationHistoryService conversationHistoryService) {
        this.messageSender = messageSender;
        this.cockSizeGame = cockSizeGame;
        this.pidorGame = pidorGame;
        this.aiService = aiService;
        this.conversationHistoryService = conversationHistoryService;
    }

    public void dispatch(Update update) {
        Message message = update.getMessage();
        String text = message.getText();

        saveMessage(message);

        if (!text.startsWith("/"))
            return;

        String[] parts = text.split(" ", 2);
        String commandStr = parts[0];

        Command command = Command.fromString(commandStr);
        if (command == null) {
            logger.debug("Неизвестная команда: {}", commandStr);
            return;
        }

        logger.debug("Выполняем команду: {}", command);

        if (handlers.containsKey(command)) {
            handlers.get(command).accept(message);
        }
    }

    private void saveMessage(Message message) {
        String text = message.getText();
        try {
            String userName;
            String textToSave = text.split(" ", 2)[1];

            if (message.getFrom().getUserName() == null)
                userName = message.getFrom().getFirstName();
            else
                userName = message.getFrom().getUserName();

            String messageToSave = "Сообщение от: " + userName + " : " + textToSave;
            String role = "user";

            aiService.saveMessage(message.getChatId(),
                    message.getFrom().getId(),
                    role,
                    messageToSave);
        }
        catch (Exception e) {
            logger.error("Ошибка сохранения сообщения в историю: {}", String.valueOf(e));
        }
    }

    // Хендлеры команд (каждый - 1 ответственность)
    private void handleBotInfo(Message message) {
        String info = """
            Бот для развлечений! Команды:
            /cocksize - Измерить достоинство
            /pidor_start - Выбрать пидора дня
            /horoscope_today - Гороскоп
            /pidor_reg - Вступить в ряды пидоров
            /pidor_stats - Статистика пидоров
            /ai - Спроси GigaChat
            /summary - Пересказ последних 100 сообщений
            """;
        messageSender.sendMessage(message.getChatId(), info);
    }
    private void handleCockSize(Message message) {
        cockSizeGame.sendTodayCockSize(message);
    }
    private void handlePidorReg(Message message) {
        pidorGame.registerPlayer(message.getChatId(), message.getFrom().getId());
    }
    private void handlePidorStats(Message message) {
        pidorGame.sendPidorStats(message.getChatId());
    }
    private void handlePidorStart(Message message) {
        pidorGame.startPidorGame(message.getChatId(), message.getFrom().getId());
    }
    private void handleHoroscope(Message message) {
        keyboardBuilder.sendHoroscopeKeyboard(messageSender, message.getChatId());
    }
    private void handleAi(Message message) {
        aiService.askAi(message);
    }
    private void handleAiChatHistory(Message message) {
        Long chatId = message.getChatId();
        try {
            String clearMsg = "Запуск принудительной отчистки истории AI, будет удалено воспоминаний: " + conversationHistoryService.getHistorySize(chatId);
            messageSender.sendMessage(chatId, clearMsg);
            Thread.sleep(1000);

            clearMsg = "Было приятно помнить вас \uD83D\uDE22 \uD83D\uDE22 \uD83D\uDE22";
            messageSender.sendMessage(chatId, clearMsg);

            Thread.sleep(1000);

            conversationHistoryService.clearAllHistory(chatId);

            String successClearMsg = "Принудительная отчистка истории AI в чате выполнена. Воспоминаний в памяти: 0";
            messageSender.sendMessage(chatId, successClearMsg);

            //TODO: сохранять в память количество сбрасований

            logger.info("Выполнена принудительная отчистка истории AI в чате: {}", message.getChat().getTitle());
        }
        catch (Exception e) {
            logger.error("Ошибка отчистки памяти AI: {}", e.toString());
        }

    }

}
