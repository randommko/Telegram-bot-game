package org.example.Users;

import org.telegram.telegrambots.meta.api.objects.User;


public class UsersService {
    private final UsersRepository repo = new UsersRepository();

    public void addUser(User user) {
        repo.insertUserInDB(user);
    }

    public void updateUser(User user) {
        repo.updateUserInDB(user);
    }

    public boolean checkUser(User user) {
        return !(repo.getUserByID(user.getId()) == null);
    }

    public String getUserNameByID(Long userID) {
        return repo.getUserByID(userID).getUserName();
    }
}
