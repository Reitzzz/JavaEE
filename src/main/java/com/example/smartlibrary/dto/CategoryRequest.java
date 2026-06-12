package com.example.smartlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 80, message = "分类名称不能超过 80 个字符")
        String name,

        @NotBlank(message = "分类描述不能为空")
        @Size(max = 200, message = "分类描述不能超过 200 个字符")
        String description
) {
}
