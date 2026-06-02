package com.example.smartlibrary.dto;

public record BookRequest(
        String isbn,
        String title,
        String author,
        String publisher,
        Integer totalCopies,
        Integer availableCopies,
        Long categoryId,
        String status
) {
}
