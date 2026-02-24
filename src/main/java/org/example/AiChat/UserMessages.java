package org.example.AiChat;


public class UserMessages {
    private final Messages messages;
    private final String role;

    public UserMessages(String role) {
        this.role = role;
        messages = new Messages();
    }

    public int saveMessage(String text) {
        return messages.saveMessage(text);
    }

    public Messages getMessages() {
        return messages;
    }

    public String getRole() {
        return role;
    }

    public int clearMessages () {
        return messages.clearMessages();
    }

    public int removeLastMessage() {
        return messages.removeLastMessage();
    }

}
