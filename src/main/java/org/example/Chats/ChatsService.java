package org.example.Chats;

public class ChatsService {
    ChatsRepository repo = new ChatsRepository();
    public boolean checkChat(Long chatID) {
        return repo.getChatTitleByID(chatID) != null;
    }
    public void addChat(Long chatID, String chatTitle) {
        repo.insertChatInDB(chatID, chatTitle);
    }
}
