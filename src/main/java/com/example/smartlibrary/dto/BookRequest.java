package com.example.smartlibrary.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank(message = "ISBN 不能为空")
        @Size(max = 40, message = "ISBN 不能超过 40 个字符")
        String isbn,

        @NotBlank(message = "书名不能为空")
        @Size(max = 120, message = "书名不能超过 120 个字符")
        String title,

        @NotBlank(message = "作者不能为空")
        @Size(max = 80, message = "作者不能超过 80 个字符")
        String author,

        @NotBlank(message = "出版社不能为空")
        @Size(max = 100, message = "出版社不能超过 100 个字符")
        String publisher,

        @NotNull(message = "馆藏数量不能为空")
        @Min(value = 0, message = "馆藏数量不能小于 0")
        Integer totalCopies,

        @Min(value = 0, message = "可借数量不能小于 0")
        Integer availableCopies,

        @NotNull(message = "分类不能为空")
        Long categoryId,

        @Pattern(regexp = "ON_SHELF|OFF_SHELF", message = "图书状态不合法")
        String status
) {
}
