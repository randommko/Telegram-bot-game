package org.example.QuotesGame;

import org.telegram.telegrambots.meta.api.objects.Message;

private final OpenAIClient aiClient;
private final Random random = new Random();
public class QuoteHandler {

    this.aiClient = OpenAIOkHttpClient.builder()
            .apiKey(openAiKey)
        .build();


    private void analyzeAndSaveQuoteIfWorth(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        // Лимит: 1 цитата в час от пользователя
        if (!canSaveQuote(chatId, userId)) {
            return;
        }

        String text = message.getText();
        String prompt = """
        Это сообщение из чата друзей: "%s".
        Стоит ли сохранить как мудрую/смешную цитату? 
        Ответь ТОЛЬКО 'ДА' или 'НЕТ'.
        """.formatted(text);

        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-4o-mini")
                    .messages(List.of(ChatMessage.builder()
                                    .role(ChatMessageRole.SYSTEM)
                                    .content("Ты строгий критик цитат. Сохраняй только действительно мудрые или очень смешные.")
                                    .build(),
                            ChatMessage.builder()
                                    .role(ChatMessageRole.USER)
                                    .content(prompt)
                                    .build()))
                    .maxTokens(5)
                    .temperature(0.1)  // мало рандома
                    .build();

            ChatCompletionResult result = aiClient.chat().completions().create(request);
            String aiAnswer = result.getChoices().get(0).getMessage().getContent().trim().toUpperCase();

            if ("ДА".equals(aiAnswer)) {
                saveQuoteToDb(message, "AI");
                sendMessage(chatId, "🤖 ИИ сохранил мудрую цитату: «" + text + "» ✨");
            }

        } catch (Exception e) {
            // Логируем, но не спамим чат
            System.err.println("AI анализ не удался: " + e.getMessage());
        }
    }


    private boolean canSaveQuote(Long chatId, Long userId) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = """
            SELECT COUNT(*) 
            FROM telegram_quote 
            WHERE chat_id = ? AND saver_user_id = ? 
            AND created_at > NOW() - INTERVAL '1 hour'
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setLong(2, userId);
                ResultSet rs = stmt.executeQuery();
                rs.next();
                return rs.getInt(1) < 1;  // 1 в час
            }
        } catch (SQLException e) {
            return false;
        }
    }


    private void handleSaveQuote(Message message) {

        if (message.hasText()) {
            String text = message.getText();
            if (text.length() > 10 && text.length() < 300 && !isBotCommand(text)) {
                analyzeAndSaveQuoteIfWorth(message);
            }

        Message reply = message.getReplyToMessage();
        if (reply == null) {
            sendMessage(message.getChatId(), "Ответь этой командой на сообщение с цитатой 🙃");
            return;
        }

        String quoteText = reply.getText();
        if (quoteText == null || quoteText.trim().isEmpty()) {
            sendMessage(message.getChatId(), "В этом сообщении нет текста, нечего сохранять 🤔");
            return;
        }

        Long chatId = message.getChatId();
        Long authorId = reply.getFrom().getId();
        Long saverId = message.getFrom().getId();

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "INSERT INTO telegram_quote (chat_id, author_user_id, saver_user_id, text) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setLong(2, authorId);
                stmt.setLong(3, saverId);
                stmt.setString(4, quoteText);
                stmt.executeUpdate();
            }
            sendMessage(chatId, "Цитата сохранена как великая мудрость ✨");
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(chatId, "Ошибка сохранения. Попробуй позже 😅");
        }
    }

    private void handleRandomQuote(Long chatId) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = """
                SELECT author_user_id, saver_user_id, text 
                FROM telegram_quote 
                WHERE chat_id = ? 
                ORDER BY random() 
                LIMIT 1
                """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Long authorId = rs.getLong("author_user_id");
                        Long saverId = rs.getLong("saver_user_id");
                        String text = rs.getString("text");

                        String authorName = getUserName(authorId); // твоя логика из telegram_user
                        String saverName = getUserName(saverId);

                        String reply = "«" + text + "»\n— " + authorName + " (сохранил: " + saverName + ")";
                        sendMessage(chatId, reply);
                    } else {
                        sendMessage(chatId, "Пока нет ни одной мудрой цитаты. Сохрани первую с помощью /savequote 😉");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(chatId, "Ошибка поиска цитаты 😅");
        }
    }
}
