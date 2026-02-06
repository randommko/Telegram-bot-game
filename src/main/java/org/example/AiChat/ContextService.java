package org.example.AiChat;

import org.telegram.telegrambots.meta.api.objects.Message;



public class ContextService {
    private final ContextRepo repo = new ContextRepo();
    public void saveContext(Message message) {
        if (needToSaveMessage(message))
            return;
        repo.saveContext(message);
    }
    private Boolean needToSaveMessage(Message message) {
        String text = message.getText();
        Long userId = message.getFrom().getId();
        //не сохраняем сообщения от ботов (ПРОД и ТЕСТ)
        //userId == 7332966399L || userId == 7712595730L
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.trim().startsWith("/");
    }
}
