package com.example.smartlibrary.model;

public record UserAccount(Long id, String username, String password, String displayName, boolean enabled) {
}
