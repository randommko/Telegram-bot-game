package org.example.AiChat;

import java.util.ArrayList;
import java.util.List;

public class Messages {
    private final List<String> messages = new ArrayList<>();

//    public record messageInChat(String role, String content, Long timestamp) {
//        public messageInChat(String role, String content) {
//            this(role, content, System.currentTimeMillis());
//        }
//    }

    public int saveMessage(String text) {
        messages.add(text);
        return messages.size();
    }
    public int clearMessages () {
        messages.clear();
        return 0;
    }

    public int removeLastMessage() {
        messages.removeLast();
        return messages.size();
    }
}
