package org.example.QuotesGame;


import org.example.DataSourceConfig;
import org.example.QuizGame.QuizRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class QuoteRepository {
    private static final Logger logger = LoggerFactory.getLogger(QuizRepository.class);

    public boolean canSaveQuote(Long chatId, Long userId) {
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String sql = """
            SELECT COUNT(*) 
            FROM telegram_quote 
            WHERE chat_id = ? AND saver_user_id = ? 
            AND created_at > NOW() - INTERVAL '1 hour'
            """;
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setLong(2, userId);
                ResultSet rs = stmt.executeQuery();
                rs.next();
                return rs.getInt(1) < 1;  // 1 в час
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при получении вопроса из БД: ", e);
            return false;
        }
    }
}
