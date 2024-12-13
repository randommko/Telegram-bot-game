package org.example.Chats;

import org.telegram.telegrambots.meta.api.objects.Chat;

public class ChatsService {
    ChatsRepository repo = new ChatsRepository();
    public boolean checkChat(Long chatID) {
        return repo.getChatTitleByID(chatID) != null;
    }
    public void addChat(Chat chat) {
        repo.insertChatInDB(chat);
    }
}
