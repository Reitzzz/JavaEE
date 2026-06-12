package com.example.smartlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 60, message = "用户名长度需为 3-60 个字符")
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "用户名只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 60, message = "密码长度需为 6-60 个字符")
        String password,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword,

        @Size(max = 80, message = "显示名称不能超过 80 个字符")
        String displayName,

        @NotBlank(message = "验证码不能为空")
        String captcha
) {
}
