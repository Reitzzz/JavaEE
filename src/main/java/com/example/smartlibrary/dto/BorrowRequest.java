package com.example.smartlibrary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BorrowRequest(
        @NotNull(message = "请选择要借阅的图书")
        Long bookId,

        @Min(value = 1, message = "借阅天数必须在 1 到 90 天之间")
        @Max(value = 90, message = "借阅天数必须在 1 到 90 天之间")
        Integer days
) {
}
