package com.example.smartlibrary.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserWithStatsDTO {
    private Long id;
    private String username;
    private String displayName;
    private String status;
    private int totalBorrows;
    private int unreturnedBorrows;
    private LocalDateTime lastBorrowTime;
}
