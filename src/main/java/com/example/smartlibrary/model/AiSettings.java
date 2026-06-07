package com.example.smartlibrary.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_settings")
public class AiSettings {

    @TableId(type = IdType.INPUT) // Id is manually set to 1
    private Long id;
    private String apiKey;
    private String deepseekApiKey;
    private Long activeModelId;

    public AiSettings(String apiKey, String deepseekApiKey, Long activeModelId) {
        this.id = 1L;
        this.apiKey = apiKey;
        this.deepseekApiKey = deepseekApiKey;
        this.activeModelId = activeModelId;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
