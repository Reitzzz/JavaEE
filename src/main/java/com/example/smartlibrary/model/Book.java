package com.example.smartlibrary.model;

public record Book(
        Long id,
        String isbn,
        String title,
        String author,
        String publisher,
        int totalCopies,
        int availableCopies,
        Long categoryId,
        String categoryName,
        String status
) {
}
