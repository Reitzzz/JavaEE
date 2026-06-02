package com.example.smartlibrary.controller;

import com.example.smartlibrary.dto.AiModelRequest;
import com.example.smartlibrary.dto.AiChatRequest;
import com.example.smartlibrary.dto.AiSettingsRequest;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.model.AiModel;
import com.example.smartlibrary.repository.AiModelRepository;
import com.example.smartlibrary.repository.AiSettingsRepository;
import com.example.smartlibrary.service.LlmService;
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
    private final AiModelRepository aiModelRepository;
    private final AiSettingsRepository aiSettingsRepository;

    public AiController(
            LlmService llmService,
            AiModelRepository aiModelRepository,
            AiSettingsRepository aiSettingsRepository) {
        this.llmService = llmService;
        this.aiModelRepository = aiModelRepository;
        this.aiSettingsRepository = aiSettingsRepository;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody AiChatRequest request) {
        return Map.of("answer", llmService.ask(request.question()));
    }

    @GetMapping("/models")
    public List<AiModel> models() {
        return aiModelRepository.findAll();
    }

    @PostMapping("/models")
    public AiModel addModel(@RequestBody AiModelRequest request) {
        String modelName = request == null ? null : request.modelName();
        llmService.testModel(modelName);
        return aiModelRepository.create(modelName.trim());
    }

    @DeleteMapping("/models/{id}")
    public void deleteModel(@PathVariable Long id) {
        aiModelRepository.delete(id);
    }

    @PostMapping("/models/{id}/activate")
    public Map<String, Object> activateModel(@PathVariable Long id) {
        aiModelRepository.findById(id).orElseThrow(() -> new BusinessException("模型不存在"));
        aiSettingsRepository.saveActiveModel(id);
        return Map.of("success", true, "message", "模型已切换");
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        var settings = aiSettingsRepository.find();
        String apiKey = settings.map(value -> value.apiKey()).orElse("");
        Long activeModelId = settings
                .map(value -> value.activeModelId())
                .orElseGet(() -> aiModelRepository.findFirst().map(model -> model.id()).orElse(null));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("apiKeyConfigured", apiKey != null && !apiKey.isBlank());
        result.put("apiKeyMask", maskApiKey(apiKey));
        result.put("activeModelId", activeModelId);
        return result;
    }

    @PostMapping("/settings")
    public Map<String, Object> saveSettings(@RequestBody AiSettingsRequest request) {
        if (request == null || request.apiKey() == null || request.apiKey().isBlank()) {
            throw new BusinessException("API Key 不能为空");
        }
        aiSettingsRepository.saveApiKey(request.apiKey());
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
