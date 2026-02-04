package org.example.AiChat;

// Вспомогательный класс для контекста (замена ChatMessage из GigaChat)
class ChatMessage {
    private final AiChatRole role;
    private final String content;

    public ChatMessage(AiChatRole role, String content) {
        this.role = role;
        this.content = content;
    }

    public AiChatRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}
