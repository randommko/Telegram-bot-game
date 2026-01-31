package org.example.QuotesGame;


import org.example.DTO.QuoteDTO;
import org.example.DataSourceConfig;
import org.example.QuizGame.QuizRepository;
import org.example.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import static org.example.TablesDB.TELEGRAM_QUOTE_TABLE;

public class QuoteRepository {
    private static final Logger logger = LoggerFactory.getLogger(QuoteRepository.class);
    private final UsersService usersService = new UsersService();

    public boolean canSaveQuote(Long chatId, Long userId) {
        //Функция для защиты от частого сохранения цитат
        // Лимит: 1 цитата в час от пользователя
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String sql = """
            SELECT COUNT(*) 
            FROM telegram_quote 
            WHERE chat_id = ? AND author_user_id = ? 
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
            logger.error("Произошла ошибка при обращении к БД в canSaveQuote(): ", e);
            return false;
        }
    }

    public boolean saveQuote(String quoteText, Long chatId, Long authorId) {
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String sql = "INSERT INTO telegram_quote (chat_id, author_user_id, text) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setLong(2, authorId);
//                stmt.setInt(3, 0); //0 - это AI
                stmt.setString(3, quoteText);
                logger.debug("SQL сохранения цитаты: " + sql);
                stmt.executeUpdate();
            }
            logger.info("Цитата сохранена");
        } catch (Exception e) {
            logger.error("Произошла ошибка при сохранении цитаты в БД: ", e);
            return false;
        }
        return false;
    }

    public QuoteDTO handleRandomQuote(Long chatId) {
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String sql = """
                SELECT author_user_id, text, created_at 
                FROM telegram_quote 
                WHERE chat_id = ? 
                ORDER BY random() 
                LIMIT 1
                """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Long authorId = rs.getLong("author_user_id");
                        String text = rs.getString("text");
                        Timestamp date = rs.getTimestamp("created_at");

                        String authorName = usersService.getUserNameByID(authorId); // твоя логика из telegram_user

                        return new QuoteDTO(text, authorName, date);
                    } else {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при обращении к БД: ", e);
        }
        return null;
    }
}
