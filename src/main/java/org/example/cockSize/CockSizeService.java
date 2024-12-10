package org.example.cockSize;

import java.util.Random;

public class CockSizeService {
    private final CockSizeRepository repo = new CockSizeRepository();
    public Integer measureCockSize(Long userID) {
        int newRandomSize = getCockSize();
        repo.setCockSizeWinner(userID, newRandomSize);
        return newRandomSize;
    }

    public Integer findTodayCockSize(Long userID) {
        return repo.getPlayerCockSize(userID);
    }

    private static int getCockSize() {
        int randomNum = new Random().nextInt(100);        //Выбираем случайное число для попадания в распределение

        if (randomNum < 2)                                      //У 2% выборки
            return new Random().nextInt(3);              //длина от 0 до 3

        if (randomNum < 7)                                     //У 5% выборки
            return new Random().nextInt(5) + 3;           //длина от 3 до 8

        if (randomNum < 25)                                     //У 18% выборки
            return new Random().nextInt(7) + 8;           //длина от 8 до 15

        if (randomNum < 55)                                     //У 30% выборки
            return new Random().nextInt(5) + 15;          //длина от 15 до 20

        if (randomNum < 65)                                    //У 10% выборки
            return new Random().nextInt(5) + 20;        //длина от 20 до 25

        if (randomNum < 73)                                    //У 8% выборки
            return new Random().nextInt(5) + 25;        //длина от 25 до 30

        if (randomNum < 78)                                    //У 5% выборки
            return new Random().nextInt(3) + 25;        //длина от 25 до 28

        if (randomNum < 83)                                    //У 5% выборки
            return new Random().nextInt(3) + 28;        //длина от 25 до 31

        if (randomNum < 88)                                    //У 5% выборки
            return new Random().nextInt(3) + 31;        //длина от 31 до 34

        if (randomNum < 91)                                    //У 3% выборки
            return new Random().nextInt(3) + 34;        //длина от 34 до 37

        if (randomNum < 94)                                    //У 3% выборки
            return new Random().nextInt(3) + 37;        //длина от 37 до 40

        if (randomNum < 96)                                    //У 2% выборки
            return new Random().nextInt(3) + 40;        //длина от 40 до 43

        if (randomNum < 98)                                    //У 2% выборки
            return new Random().nextInt(5) + 40;        //длина от 40 до 45

        if (randomNum < 100)                                    //У 2% выборки
            return new Random().nextInt(5) + 45;        //длина от 45 до 50
        return -1;
    }

    public String phraseSelection(int size, String username) {
        if (size >= 0 && size <= 5) {
            return "The legendary cocksize of @" + username + " is " + size + "cm\uD83D\uDC4E";
        } else if (size >= 6 && size <= 10) {
            return "The mighty cocksize of @" + username + " is " + size + "cm\uD83D\uDE22";
        } else if (size >= 11 && size <= 20) {
            return "The epic cocksize of @" + username + " is " + size + "cm\uD83D\uDE0D";
        } else if (size >= 21 && size <= 30) {
            return "The majestic cocksize of @" + username + " is " + size + "cm\uD83D\uDE0E";
        } else if (size >= 31 && size <= 40) {
            return "The legendary cocksize of @" + username + " is " + size + "cm\uD83E\uDD21";
        } else if (size >= 41 && size <= 50) {
            return "The mythical cocksize of @" + username + " is " + size + "cm\uD83D\uDD25";
        } else return "NO FUCKING WAY! Cocksize @" + username + " is " + size + "cm\uD83D\uDC80";
    }

    public String getCockSizeImageName(Integer size) {
        return repo.getCockSizeImageName(size);
    }

    public String getUserNameByID(Long userID) {
        return repo.getUserNameByID(userID);
    }
}