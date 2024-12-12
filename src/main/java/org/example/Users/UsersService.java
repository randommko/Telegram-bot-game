package org.example.Users;

import java.util.Map;

public class UsersService {
    private final UsersRepository repo = new UsersRepository();


    public void addUser(String userName, Long userID) {
        repo.insertUserInDB(userName, userID);
    }

    public boolean checkUser(Long userID) {
        return !repo.getUserNameByID(userID).isEmpty();
    }

    public String getUserNameByID(Long userID) {
        return repo.getUserNameByID(userID);
    }
}
