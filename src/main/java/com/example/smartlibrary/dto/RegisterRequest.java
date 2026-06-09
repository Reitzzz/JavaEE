package com.example.smartlibrary.dto;

public record RegisterRequest(
        String username,
        String password,
        String confirmPassword,
        String displayName,
        String captcha
) {
}
