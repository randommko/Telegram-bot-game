package org.example.CockSize;

import org.example.Users.UsersService;

import java.util.ArrayList;
import java.util.Random;
import com.vdurmont.emoji.EmojiParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CockSizeService {
    private final CockSizeRepository repo = new CockSizeRepository();
    private static final Logger logger = LoggerFactory.getLogger(CockSizeService.class);
    private UsersService usersService = new UsersService();
    public Integer measureCockSize(Long userID) {
        int newRandomSize = getCockSize();
        repo.setCockSizeWinner(userID, newRandomSize);
        return newRandomSize;
    }

    public Integer findTodayCockSize(Long userID) {
        return repo.getPlayerCockSize(userID);
    }

    private static int getCockSize() {
        ArrayList<Integer> cockSizeList = new ArrayList<>();

        cockSizeList.add(0);
        cockSizeList.add(1);
        cockSizeList.add(2);
        cockSizeList.add(3);
        for (int i = 0; i < 2; i++) {
            cockSizeList.add(5);
        }
        for (int i = 0; i < 2; i++) {
            cockSizeList.add(6);
        }
        for (int i = 0; i < 2; i++) {
            cockSizeList.add(7);
        }
        for (int i = 0; i < 2; i++) {
            cockSizeList.add(8);
        }

        for (int i = 0; i < 5; i++) {
            cockSizeList.add(10);
        }

        for (int i = 0; i < 5; i++) {
            cockSizeList.add(11);
        }
        for (int i = 0; i < 5; i++) {
            cockSizeList.add(13);
        }
        for (int i = 0; i < 4; i++) {
            cockSizeList.add(15);
        }
        for (int i = 0; i < 4; i++) {
            cockSizeList.add(16);
        }
        for (int i = 0; i < 3; i++) {
            cockSizeList.add(18);
        }

        for (int i = 0; i < 2; i++) {
            cockSizeList.add(20);
        }
        for (int i = 0; i < 2; i++) {
            cockSizeList.add(24);
        }

        for (int i = 0; i < 1; i++) {
            cockSizeList.add(40);
        }

        for (int i = 0; i < 1; i++) {
            cockSizeList.add(45);
        }
        for (int i = 0; i < 1; i++) {
            cockSizeList.add(49);
        }

        logger.info(cockSizeList.toString());

        // Генерируем случайный элемент
        Random random = new Random();
        int randomIndex = random.nextInt(cockSizeList.size());
        return cockSizeList.get(randomIndex);
    }

    public String phraseSelection(int size, String username) {
        if (size >= 0 && size <= 5) {
            return EmojiParser.parseToUnicode("Cocksize of " + username + " is " + size + "cm :-1:");
        } else if (size >= 6 && size <= 10) {
            return EmojiParser.parseToUnicode("Cocksize of " + username + " is " + size + "cm :handshake:");
        } else if (size >= 11 && size <= 20) {
            return EmojiParser.parseToUnicode("Cocksize of " + username + " is " + size + "cm :tada:");
        } else if (size >= 21 && size <= 30) {
            return EmojiParser.parseToUnicode("Cocksize of " + username + " is " + size + "cm :balloon::balloon:");
        } else if (size >= 31 && size <= 40) {
            return EmojiParser.parseToUnicode("Cocksize of " + username + " is " + size + "cm :palm_tree:");
        } else if (size >= 41 && size <= 50) {
            return EmojiParser.parseToUnicode("TCocksize of " + username + " is " + size + "cm :sparkling_heart::sparkling_heart::sparkling_heart:");
        } else return EmojiParser.parseToUnicode("NO FUCKING WAY! Cocksize of " + username + " is " + size + "cm ");
    }

    public String getCockSizeImageName(Integer size) {
        return repo.getCockSizeImageName(size);
    }

    public String getUserNameByID(Long userID) {
        return usersService.getUserNameByID(userID);
    }
}
