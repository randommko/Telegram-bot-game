package org.example.Chats;

import org.telegram.telegrambots.meta.api.objects.Chat;

public class ChatsService {
    ChatsRepository repo = new ChatsRepository();
    public boolean checkChat(Long chatID) {
        Chat chat = repo.getChatByID(chatID);
        return chat.getId() != null;
    }
    public void addChat(Chat chat) {
        repo.insertChatInDB(chat);
    }
}
