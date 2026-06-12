package com.example.smartlibrary.controller;

import com.example.smartlibrary.dto.AiModelRequest;
import com.example.smartlibrary.dto.AiChatRequest;
import com.example.smartlibrary.dto.AiSettingsRequest;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.model.AiModel;
import com.example.smartlibrary.model.AiSettings;
import com.example.smartlibrary.mapper.AiModelMapper;
import com.example.smartlibrary.mapper.AiSettingsMapper;
import com.example.smartlibrary.service.LlmService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final LlmService llmService;
    private final AiModelMapper aiModelMapper;
    private final AiSettingsMapper aiSettingsMapper;

    public AiController(
            LlmService llmService,
            AiModelMapper aiModelMapper,
            AiSettingsMapper aiSettingsMapper) {
        this.llmService = llmService;
        this.aiModelMapper = aiModelMapper;
        this.aiSettingsMapper = aiSettingsMapper;
    }

    @PostMapping(value = "/chat", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.http.ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> chat(@Valid @RequestBody AiChatRequest request) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setCacheControl(org.springframework.http.CacheControl.noCache());
        headers.set("X-Accel-Buffering", "no"); // 禁用 Nginx 等反向代理的缓存
        headers.set("Connection", "keep-alive");
        return org.springframework.http.ResponseEntity.ok().headers(headers).body(llmService.askStream(request.question()));
    }

    @GetMapping("/models")
    public List<AiModel> models() {
        QueryWrapper<AiModel> query = new QueryWrapper<>();
        query.orderByDesc("id");
        return aiModelMapper.selectList(query);
    }

    @PostMapping("/models")
    public AiModel addModel(@Valid @RequestBody AiModelRequest request) {
        String modelName = request == null ? null : request.modelName();
        String provider = request == null || request.provider() == null || request.provider().isBlank() ? "MiMo" : request.provider();
        llmService.testModel(provider, modelName);
        AiModel model = new AiModel();
        model.setModelName(modelName.trim());
        model.setProvider(provider.trim());
        model.setCreatedAt(LocalDateTime.now());
        aiModelMapper.insert(model);
        return aiModelMapper.selectById(model.getId());
    }

    @DeleteMapping("/models/{id}")
    public void deleteModel(@PathVariable Long id) {
        int rows = aiModelMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("模型不存在");
        }
    }

    @PostMapping("/models/{id}/activate")
    public Map<String, Object> activateModel(@PathVariable Long id) {
        AiModel model = aiModelMapper.selectById(id);
        if (model == null) {
            throw new BusinessException("模型不存在");
        }
        AiSettings settings = aiSettingsMapper.selectById(1L);
        if (settings == null) {
            settings = new AiSettings("", "", id);
            aiSettingsMapper.insert(settings);
        } else {
            settings.setActiveModelId(id);
            aiSettingsMapper.updateById(settings);
        }
        return Map.of("success", true, "message", "模型已切换");
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        AiSettings settings = aiSettingsMapper.selectById(1L);
        String apiKey = settings != null ? settings.getApiKey() : "";
        String deepseekApiKey = settings != null ? settings.getDeepseekApiKey() : "";
        Long activeModelId = settings != null ? settings.getActiveModelId() : null;
        if (activeModelId == null) {
            QueryWrapper<AiModel> query = new QueryWrapper<>();
            query.orderByDesc("id").last("LIMIT 1");
            AiModel firstModel = aiModelMapper.selectOne(query);
            if (firstModel != null) {
                activeModelId = firstModel.getId();
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("apiKeyConfigured", apiKey != null && !apiKey.isBlank());
        result.put("apiKeyMask", maskApiKey(apiKey));
        result.put("deepseekApiKeyConfigured", deepseekApiKey != null && !deepseekApiKey.isBlank());
        result.put("deepseekApiKeyMask", maskApiKey(deepseekApiKey));
        result.put("activeModelId", activeModelId);
        return result;
    }

    @PostMapping("/settings")
    public Map<String, Object> saveSettings(@Valid @RequestBody AiSettingsRequest request) {
        if (request == null || request.apiKey() == null || request.apiKey().isBlank()) {
            throw new BusinessException("API Key 不能为空");
        }
        String provider = request.provider() == null || request.provider().isBlank() ? "MiMo" : request.provider();
        AiSettings settings = aiSettingsMapper.selectById(1L);
        if (settings == null) {
            settings = new AiSettings();
            settings.setId(1L);
            if ("DeepSeek".equalsIgnoreCase(provider)) {
                settings.setDeepseekApiKey(request.apiKey().trim());
                settings.setApiKey("");
            } else {
                settings.setApiKey(request.apiKey().trim());
                settings.setDeepseekApiKey("");
            }
            aiSettingsMapper.insert(settings);
        } else {
            if ("DeepSeek".equalsIgnoreCase(provider)) {
                settings.setDeepseekApiKey(request.apiKey().trim());
            } else {
                settings.setApiKey(request.apiKey().trim());
            }
            aiSettingsMapper.updateById(settings);
        }
        return Map.of("success", true, "message", "API Key 已保存");
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 10) {
            return "********";
        }
        return trimmed.substring(0, 5) + "****" + trimmed.substring(trimmed.length() - 5);
    }
}
