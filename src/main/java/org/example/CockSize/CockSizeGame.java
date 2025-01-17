package org.example.CockSize;


import org.example.DTO.AVGCockSizeDTO;
import org.example.TelegramBot;
import org.example.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class CockSizeGame {
    CockSizeService service = new CockSizeService();
    private static final Logger logger = LoggerFactory.getLogger(CockSizeGame.class);
    private final UsersService usersService = new UsersService();
    private final TelegramBot bot;
    private static final String RESOURCES_PATH = "/bin/tg_bot/resources";

    public CockSizeGame() {
        bot = TelegramBot.getInstance();
    }
    public void sendTodayCockSize(Message message) {
        Integer userSize = null;
        String messageText = null;
        Long chatID = message.getChatId();

        Map<Integer, String> sizeMap = getCockSize(message.getFrom().getId());
        for (Map.Entry<Integer, String> entry : sizeMap.entrySet()) {
            userSize = entry.getKey();
            messageText = entry.getValue();
        }

        String imgFileName = getCockSizeImageName(userSize);
        File img = new File(RESOURCES_PATH + "/" + imgFileName);

        if (!bot.sendImgMessage(chatID, messageText, img))
            bot.sendMessage(chatID, messageText);
        bot.sendInlineCockSizeKeyboard(chatID);
    }
    private Map<Integer, String> getCockSize(Long userID) {
        //Ключ - длинна, Значение - сообщение соответсвующее длине
        Map<Integer, String> result = new HashMap<>();

        Integer playerTodayCockSize = service.findTodayCockSize(userID);
        String msgText = service.phraseSelection(playerTodayCockSize, service.getUserNameByID(userID));

        if (playerTodayCockSize != -1) {
            result.put(playerTodayCockSize, msgText);
            return result;
        }

        Integer playerNewCockSize = service.measureCockSize(userID);
        msgText = service.phraseSelection(playerNewCockSize, service.getUserNameByID(userID));

        result.put(playerNewCockSize, msgText);

        return result;
    }
    private String getCockSizeImageName(Integer size) {
        return service.getCockSizeImageName(size);
    }
    public void sendAVGCockSize(Update update) {
        Long userID= update.getCallbackQuery().getFrom().getId();
        Long chatID = update.getCallbackQuery().getMessage().getChatId();
        String userName = usersService.getUserNameByID(userID);
        AVGCockSizeDTO result = service.getAVGCockSize(userID);

        if (result == null) {
            bot.sendMessage(chatID, userName + " ни разу не измерял свой член!");
            return;
        }
        bot.sendMessage(chatID, "Статистика измерений " + userName + "\n" +
                "За период с " + result.firstMeasurementDate + " по " + result.lastMeasurementDate + " было совершено " +
                result.measurementCount + " измерений.\nСредняя длинна составила: " + result.AVGSize);
    }
}
