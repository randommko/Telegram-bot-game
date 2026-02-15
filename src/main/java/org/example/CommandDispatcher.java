package org.example;

import org.example.AiChat.AiChat;
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
    private final AiChat aiChat;
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
        SUMMARY("/summary");

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
            Command.SUMMARY, this::handleSummary
    );

    public CommandDispatcher(MessageSender messageSender,
                             CockSizeGame cockSizeGame,
                             PidorGame pidorGame,
                             AiChat aiChat) {
        this.messageSender = messageSender;
        this.cockSizeGame = cockSizeGame;
        this.pidorGame = pidorGame;
        this.aiChat = aiChat;
    }

    public void dispatch(Update update) {
        Message message = update.getMessage();
        String text = message.getText();

        // Сохраняем контекст для не-команд
        if (!text.startsWith("/")) {
//            contextService.saveContext(message);
            logger.debug("Сохранение контекста отключено: {}", text);
            return;
        }

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
        aiChat.askAi(message);
    }

    private void handleSummary(Message message) {
        aiChat.summary(message);
    }

}
