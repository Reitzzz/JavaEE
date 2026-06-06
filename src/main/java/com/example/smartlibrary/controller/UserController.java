package com.example.smartlibrary.controller;

import com.example.smartlibrary.dto.UserWithStatsDTO;
import com.example.smartlibrary.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserWithStatsDTO> list() {
        return userService.findAllWithStats();
    }

    @PostMapping("/{id}/ban")
    public void ban(@PathVariable Long id) {
        userService.banUser(id);
    }

    @PostMapping("/{id}/unban")
    public void unban(@PathVariable Long id) {
        userService.unbanUser(id);
    }
}
