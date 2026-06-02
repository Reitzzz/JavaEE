package com.example.smartlibrary.dto;

public record BorrowRequest(Long bookId, Integer days) {
}
