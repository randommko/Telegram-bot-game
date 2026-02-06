package org.example.AiChat;

public enum AiChatRole {
    SYSTEM("system"),
    USER("user");
    public final String value;

    AiChatRole(String value) {
        this.value = value;
    }
}
