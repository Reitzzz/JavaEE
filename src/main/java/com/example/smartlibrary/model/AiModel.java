package com.example.smartlibrary.model;

import java.time.LocalDateTime;

public record AiModel(
        Long id,
        String modelName,
        String provider,
        LocalDateTime createdAt) {
}
