package com.example.smartlibrary.model;

public record AiSettings(String apiKey, Long activeModelId) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
