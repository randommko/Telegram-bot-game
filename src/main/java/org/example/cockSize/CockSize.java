package org.example.cockSize;

import java.util.HashMap;
import java.util.Map;

public class CockSize {
    CockSizeService service = new CockSizeService();
    public Map<Integer, String> getCockSize(Long userID) {
        Map<Integer, String> result = new HashMap<>();

        Integer playerCockSize = service.findTodayCockSize(userID);
        String msgText = service.phraseSelection(playerCockSize, service.getUserNameByID(userID));

        if (playerCockSize != -1) {
            result.put(playerCockSize, msgText);
            return result;
        }

        playerCockSize = service.measureCockSize(userID);
        msgText = service.phraseSelection(playerCockSize, service.getUserNameByID(userID));

        result.put(playerCockSize, msgText);

        return result;

    }
    public String getCockSizeImageName(Integer size) {
        return service.getCockSizeImageName(size);
    }
}
