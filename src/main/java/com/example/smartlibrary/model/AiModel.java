package com.example.smartlibrary.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_models")
public class AiModel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String modelName;
    private String provider;
    private LocalDateTime createdAt;
}
