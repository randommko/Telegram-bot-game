package org.example.AiChat;

import java.util.ArrayList;
import java.util.List;

public class UserMessages {
    private final List<messageInChat> messages;
    private final String userRole;

    public record messageInChat(String content, Long timestamp, String role) {
        public messageInChat(String content, String role) {
            this(content, System.currentTimeMillis(), role);
        }
    }

    public UserMessages(String role, String text) {
        messages = new ArrayList<>();
        this.userRole = role;
        saveMessage(text);
    }

    public void saveMessage(String text) {
        messages.add(new messageInChat(text, userRole));
    }

    public List<messageInChat> getMessages() {
        return messages;
    }

    public int getSize() {
        return messages.size();
    }

    public String getUserRole() {
        return userRole;
    }

    public void clearMessages() {
        messages.clear();
    }

    public void removeLastMessage() {
        if (!messages.isEmpty()) {
            messages.remove(messages.size() - 1);
        }
    }
}