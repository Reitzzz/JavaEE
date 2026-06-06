package com.example.smartlibrary.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("borrow_records")
public class BorrowRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    
    @TableField(exist = false)
    private String username;
    
    @TableField(exist = false)
    private String displayName;
    
    private Long bookId;
    
    @TableField(exist = false)
    private String bookTitle;
    
    @TableField(exist = false)
    private String bookAuthor;
    
    private LocalDateTime borrowedAt;
    private LocalDateTime dueAt;
    private LocalDateTime returnedAt;
    private String status;
}
