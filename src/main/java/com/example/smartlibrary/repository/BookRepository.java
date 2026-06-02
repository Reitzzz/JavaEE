package com.example.smartlibrary.repository;

import com.example.smartlibrary.dto.BookRequest;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.model.Book;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BookRepository {

    private static final RowMapper<Book> BOOK_ROW_MAPPER = (rs, rowNum) -> new Book(
            rs.getLong("id"),
            rs.getString("isbn"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("publisher"),
            rs.getInt("total_copies"),
            rs.getInt("available_copies"),
            rs.getLong("category_id"),
            rs.getString("category_name"),
            rs.getString("status"));

    private final JdbcTemplate jdbcTemplate;

    public BookRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Book> findAll(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return jdbcTemplate.query(baseSelect() + " ORDER BY b.id DESC", BOOK_ROW_MAPPER);
        }
        String like = "%" + keyword.trim().toLowerCase() + "%";
        return jdbcTemplate.query(
                baseSelect() + """
                 WHERE LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ? OR LOWER(b.isbn) LIKE ?
                 ORDER BY b.id DESC
                """,
                BOOK_ROW_MAPPER,
                like,
                like,
                like);
    }

    public Optional<Book> findById(Long id) {
        List<Book> books = jdbcTemplate.query(baseSelect() + " WHERE b.id = ?", BOOK_ROW_MAPPER, id);
        return books.stream().findFirst();
    }

    public Book create(BookRequest request) {
        validate(request);
        int availableCopies = request.availableCopies() == null ? request.totalCopies() : request.availableCopies();
        jdbcTemplate.update(
                """
                INSERT INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                request.isbn().trim(),
                request.title().trim(),
                request.author().trim(),
                request.publisher().trim(),
                request.totalCopies(),
                availableCopies,
                request.categoryId(),
                normalizeStatus(request.status()));
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return findById(id).orElseThrow(() -> new BusinessException("图书创建失败"));
    }

    public Book update(Long id, BookRequest request) {
        validate(request);
        int availableCopies = request.availableCopies() == null ? request.totalCopies() : request.availableCopies();
        if (availableCopies > request.totalCopies()) {
            throw new BusinessException("可借数量不能大于馆藏总数");
        }
        int rows = jdbcTemplate.update(
                """
                UPDATE books
                SET isbn = ?, title = ?, author = ?, publisher = ?, total_copies = ?,
                    available_copies = ?, category_id = ?, status = ?
                WHERE id = ?
                """,
                request.isbn().trim(),
                request.title().trim(),
                request.author().trim(),
                request.publisher().trim(),
                request.totalCopies(),
                availableCopies,
                request.categoryId(),
                normalizeStatus(request.status()),
                id);
        if (rows == 0) {
            throw new BusinessException("图书不存在");
        }
        return findById(id).orElseThrow(() -> new BusinessException("图书不存在"));
    }

    public void delete(Long id) {
        int rows = jdbcTemplate.update("DELETE FROM books WHERE id = ?", id);
        if (rows == 0) {
            throw new BusinessException("图书不存在");
        }
    }

    public void decreaseAvailable(Long id) {
        int rows = jdbcTemplate.update(
                "UPDATE books SET available_copies = available_copies - 1 WHERE id = ? AND available_copies > 0",
                id);
        if (rows == 0) {
            throw new BusinessException("图书库存不足");
        }
    }

    public void increaseAvailable(Long id) {
        jdbcTemplate.update(
                "UPDATE books SET available_copies = available_copies + 1 WHERE id = ? AND available_copies < total_copies",
                id);
    }

    private String baseSelect() {
        return """
                SELECT b.id, b.isbn, b.title, b.author, b.publisher, b.total_copies, b.available_copies,
                       b.category_id, c.name AS category_name, b.status
                FROM books b
                INNER JOIN categories c ON c.id = b.category_id
                """;
    }

    private void validate(BookRequest request) {
        if (request == null
                || isBlank(request.isbn())
                || isBlank(request.title())
                || isBlank(request.author())
                || isBlank(request.publisher())
                || request.categoryId() == null
                || request.totalCopies() == null
                || request.totalCopies() < 0
                || (request.availableCopies() != null && request.availableCopies() < 0)) {
            throw new BusinessException("图书 ISBN、名称、作者、出版社、分类和馆藏数量不能为空");
        }
        if (request.availableCopies() != null && request.availableCopies() > request.totalCopies()) {
            throw new BusinessException("可借数量不能大于馆藏总数");
        }
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "ON_SHELF" : status.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
