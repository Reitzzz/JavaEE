package com.example.smartlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiSettingsRequest(
        @NotBlank(message = "服务商不能为空")
        @Pattern(regexp = "MiMo|DeepSeek", message = "服务商只支持 MiMo 或 DeepSeek")
        String provider,

        @NotBlank(message = "API Key 不能为空")
        @Size(max = 512, message = "API Key 长度不能超过 512 个字符")
        String apiKey
) {
}
