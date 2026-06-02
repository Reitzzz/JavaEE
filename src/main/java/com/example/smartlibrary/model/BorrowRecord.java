package com.example.smartlibrary.model;

import java.time.LocalDateTime;

public record BorrowRecord(
        Long id,
        Long userId,
        String username,
        String displayName,
        Long bookId,
        String bookTitle,
        LocalDateTime borrowedAt,
        LocalDateTime dueAt,
        LocalDateTime returnedAt,
        String status
) {
}
