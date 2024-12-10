package org.example.Chats;

public class ChatsService {
    ChatsRepository repo = new ChatsRepository();
    public boolean checkChat(Long chatID) {
        if (repo.getChatTitleByID(chatID) == null)
            return true;
        return !repo.getChatTitleByID(chatID).isEmpty();
    }
    public void addChat(Long chatID, String chatTitle) {
        repo.insertChatInDB(chatID, chatTitle);
    }
}
