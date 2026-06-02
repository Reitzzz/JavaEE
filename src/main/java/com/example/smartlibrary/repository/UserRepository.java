package com.example.smartlibrary.repository;

import com.example.smartlibrary.model.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserAccount> findByUsername(String username) {
        List<UserAccount> users = jdbcTemplate.query(
                "SELECT id, username, password, display_name, enabled FROM users WHERE username = ?",
                (rs, rowNum) -> new UserAccount(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("display_name"),
                        rs.getBoolean("enabled")),
                username);
        return users.stream().findFirst();
    }

    public List<String> findRoleNames(Long userId) {
        return jdbcTemplate.query(
                """
                SELECT r.name
                FROM roles r
                INNER JOIN user_roles ur ON ur.role_id = r.id
                WHERE ur.user_id = ?
                """,
                (rs, rowNum) -> rs.getString("name"),
                userId);
    }
}
