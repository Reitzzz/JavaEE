package com.example.smartlibrary.controller;

import com.example.smartlibrary.dto.BorrowRequest;
import com.example.smartlibrary.model.BorrowRecord;
import com.example.smartlibrary.service.BorrowService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @GetMapping
    public List<BorrowRecord> list() {
        return borrowService.findAll();
    }

    @GetMapping("/mine")
    public List<BorrowRecord> mine(Authentication authentication) {
        return borrowService.findMine(authentication);
    }

    @PostMapping
    public BorrowRecord borrow(Authentication authentication, @RequestBody BorrowRequest request) {
        return borrowService.borrow(authentication, request);
    }

    @PostMapping("/{id}/return")
    public void returnBook(Authentication authentication, @PathVariable Long id) {
        borrowService.returnBook(authentication, id);
    }
}
