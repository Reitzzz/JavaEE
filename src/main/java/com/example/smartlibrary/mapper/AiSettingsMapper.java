package com.example.smartlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartlibrary.model.AiSettings;
import com.example.smartlibrary.model.AiModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiSettingsMapper extends BaseMapper<AiSettings> {

    @Select("SELECT m.* " +
            "FROM ai_settings s " +
            "INNER JOIN ai_models m ON m.id = s.active_model_id " +
            "WHERE s.id = 1")
    AiModel findActiveModel();
}
