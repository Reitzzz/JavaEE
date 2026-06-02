package com.example.smartlibrary.repository;

import com.example.smartlibrary.model.BorrowRecord;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BorrowRecordRepository {

    private static final RowMapper<BorrowRecord> BORROW_ROW_MAPPER = (rs, rowNum) -> new BorrowRecord(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("username"),
            rs.getString("display_name"),
            rs.getLong("book_id"),
            rs.getString("book_title"),
            rs.getTimestamp("borrowed_at").toLocalDateTime(),
            rs.getTimestamp("due_at").toLocalDateTime(),
            toLocalDateTime(rs.getTimestamp("returned_at")),
            rs.getString("status"));

    private final JdbcTemplate jdbcTemplate;

    public BorrowRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BorrowRecord> findAll() {
        return jdbcTemplate.query(baseSelect() + " ORDER BY br.id DESC", BORROW_ROW_MAPPER);
    }

    public List<BorrowRecord> findByUserId(Long userId) {
        return jdbcTemplate.query(baseSelect() + " WHERE br.user_id = ? ORDER BY br.id DESC", BORROW_ROW_MAPPER, userId);
    }

    public Optional<BorrowRecord> findById(Long id) {
        return jdbcTemplate.query(baseSelect() + " WHERE br.id = ?", BORROW_ROW_MAPPER, id).stream().findFirst();
    }

    public boolean hasActiveBorrow(Long userId, Long bookId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM borrow_records
                WHERE user_id = ? AND book_id = ? AND status = 'BORROWED'
                """,
                Integer.class,
                userId,
                bookId);
        return count != null && count > 0;
    }

    public BorrowRecord create(Long userId, Long bookId, int days) {
        LocalDateTime dueAt = LocalDateTime.now().plusDays(days);
        jdbcTemplate.update(
                "INSERT INTO borrow_records (user_id, book_id, due_at, status) VALUES (?, ?, ?, 'BORROWED')",
                userId,
                bookId,
                Timestamp.valueOf(dueAt));
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return findById(id).orElseThrow();
    }

    public void returnBook(Long id) {
        jdbcTemplate.update(
                "UPDATE borrow_records SET returned_at = CURRENT_TIMESTAMP, status = 'RETURNED' WHERE id = ? AND status = 'BORROWED'",
                id);
    }

    private String baseSelect() {
        return """
                SELECT br.id, br.user_id, u.username, u.display_name, br.book_id, b.title AS book_title,
                       br.borrowed_at, br.due_at, br.returned_at, br.status
                FROM borrow_records br
                INNER JOIN users u ON u.id = br.user_id
                INNER JOIN books b ON b.id = br.book_id
                """;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
