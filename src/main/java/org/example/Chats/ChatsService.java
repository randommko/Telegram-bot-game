package org.example.Chats;

import org.telegram.telegrambots.meta.api.objects.Chat;

public class ChatsService {
    final ChatsRepository repo = new ChatsRepository();
    public boolean checkChat(Long chatID) {
        Chat chat = repo.getChatByID(chatID);
        return chat != null;
    }
    public void addChat(Chat chat) {
        repo.insertChatInDB(chat);
    }

    public void updateChat(Chat chat) {
        repo.updateChatInDB(chat);
    }
}
