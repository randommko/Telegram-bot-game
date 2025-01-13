package org.example.CockSize;


import org.example.TelegramBot;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class CockSizeGame {
    CockSizeService service = new CockSizeService();
    private final TelegramBot bot;
    private static final String RESOURCES_PATH = "/bin/tg_bot/resources";

    public CockSizeGame() {
        bot = TelegramBot.getInstance();
    }
    public void cockSizeStart(Message message) {
        Integer userSize = null;
        String messageText = null;
        Map<Integer, String> sizeMap = getCockSize(message.getFrom().getId());
        for (Map.Entry<Integer, String> entry : sizeMap.entrySet()) {
            userSize = entry.getKey();
            messageText = entry.getValue();
        }

        String imgFileName = getCockSizeImageName(userSize);
        File img = new File(RESOURCES_PATH + "/" + imgFileName);

        if (!bot.sendImgMessage(message.getChatId(), messageText, img))
            bot.sendMessage(message.getChatId(), messageText);
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



}
