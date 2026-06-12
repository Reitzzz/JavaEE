package com.example.smartlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiModelRequest(
        @NotBlank(message = "服务商不能为空")
        @Pattern(regexp = "MiMo|DeepSeek", message = "服务商只支持 MiMo 或 DeepSeek")
        String provider,

        @NotBlank(message = "模型名称不能为空")
        @Size(max = 80, message = "模型名称不能超过 80 个字符")
        String modelName
) {
}
