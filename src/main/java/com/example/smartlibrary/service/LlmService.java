package com.example.smartlibrary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.smartlibrary.config.LlmProperties;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.constant.LlmProvider;
import com.example.smartlibrary.mapper.AiModelMapper;
import com.example.smartlibrary.mapper.AiSettingsMapper;
import com.example.smartlibrary.model.AiModel;
import com.example.smartlibrary.model.AiSettings;
import com.example.smartlibrary.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class LlmService {

    private static final String MIMO_TOKEN_PLAN_BASE_URL = "https://token-plan-cn.xiaomimimo.com/v1";

    private final LlmProperties properties;
    private final BookService bookService;
    private final AiModelMapper aiModelMapper;
    private final AiSettingsMapper aiSettingsMapper;
    private final ObjectMapper objectMapper;

    public LlmService(
            LlmProperties properties,
            BookService bookService,
            AiModelMapper aiModelMapper,
            AiSettingsMapper aiSettingsMapper,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.bookService = bookService;
        this.aiModelMapper = aiModelMapper;
        this.aiSettingsMapper = aiSettingsMapper;
        this.objectMapper = objectMapper;
    }

    public SseEmitter askStream(String question) {
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout
        if (question == null || question.isBlank()) {
            sendAndComplete(emitter, "请输入问题，例如：请根据 JavaEE 大作业推荐两本适合学习的书。");
            return emitter;
        }
        List<Book> books = bookService.findAll(null);
        AiModel currentModel = currentAiModel();
        if (!hasApiKey(currentModel.getProvider())) {
            sendAndComplete(emitter, fallbackAnswer(question, books));
            return emitter;
        }

        Thread.startVirtualThread(() -> {
            try {
                requestChatStream(
                        currentModel,
                        "你是智能图书管理系统的图书推荐助手。请基于馆藏数据回答，中文输出，简洁实用。",
                        "馆藏数据：" + summarizeBooks(books) + "\n用户问题：" + question.trim(),
                        emitter);
            } catch (Exception exception) {
                try {
                    emitter.send("大模型接口调用失败（" + failureReason(exception) + "），已切换本地推荐：\n\n" + fallbackAnswer(question, books));
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        });

        return emitter;
    }

    private void requestChatStream(AiModel model, String systemPrompt, String userPrompt, SseEmitter emitter) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model.getModelName());
        body.put("stream", true);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(currentBaseUrl(model.getProvider()) + currentChatPath(model.getProvider())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + currentApiKey(model.getProvider()))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            java.net.http.HttpResponse<java.io.InputStream> response = client.send(
                    request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new BusinessException("HTTP " + response.statusCode() + ": " + errorBody);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                        String json = line.substring(6);
                        try {
                            JsonNode node = objectMapper.readTree(json);
                            JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                            if (delta.isTextual() && !delta.asText().isEmpty()) {
                                emitter.send(delta.asText());
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private void sendAndComplete(SseEmitter emitter, String text) {
        try {
            emitter.send(text);
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

    public void testModel(String provider, String modelName) {
        String normalized = normalizeModelName(modelName);
        if (!hasApiKey(provider)) {
            throw new BusinessException("请先在管理员页面配置该服务商的 API Key 后再测试模型");
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", normalized);
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", "你是模型连通性测试助手。");
            messages.addObject().put("role", "user").put("content", "请只回复：测试成功");

            JsonNode response = RestClient.builder()
                    .baseUrl(currentBaseUrl(provider))
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + currentApiKey(provider))
                    .build()
                    .post()
                    .uri(currentChatPath(provider))
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode content = response.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new BusinessException("模型测试失败：接口未返回有效文本");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("模型测试失败，请检查模型名称、API Key 或接口地址");
        }
    }

    private AiModel currentAiModel() {
        AiModel activeModel = aiSettingsMapper.findActiveModel();
        if (activeModel != null && activeModel.getModelName() != null && !activeModel.getModelName().isBlank()) {
            return activeModel;
        }
        QueryWrapper<AiModel> query = new QueryWrapper<>();
        query.orderByDesc("id").last("LIMIT 1");
        AiModel firstModel = aiModelMapper.selectOne(query);
        if (firstModel != null) {
            return firstModel;
        }
        AiModel defaultModel = new AiModel();
        defaultModel.setModelName(properties.getModel());
        defaultModel.setProvider(LlmProvider.MIMO);
        return defaultModel;
    }

    private boolean hasApiKey(String provider) {
        String key = currentApiKey(provider);
        return key != null && !key.isBlank();
    }

    private String currentApiKey(String provider) {
        AiSettings settings = aiSettingsMapper.selectById(1L);
        if (settings != null) {
            if (LlmProvider.DEEPSEEK.equalsIgnoreCase(provider)) {
                if (settings.getDeepseekApiKey() != null && !settings.getDeepseekApiKey().isBlank()) {
                    return settings.getDeepseekApiKey();
                }
            } else {
                if (settings.hasApiKey()) {
                    return settings.getApiKey();
                }
            }
        }
        if (LlmProvider.DEEPSEEK.equalsIgnoreCase(provider)) {
            return properties.getDeepseekApiKey();
        }
        return properties.getApiKey();
    }

    private String currentBaseUrl(String provider) {
        if (LlmProvider.DEEPSEEK.equalsIgnoreCase(provider)) {
            return properties.getDeepseekBaseUrl();
        }
        String apiKey = currentApiKey(provider);
        if (apiKey != null && apiKey.trim().startsWith("tp-")) {
            return MIMO_TOKEN_PLAN_BASE_URL;
        }
        return properties.getBaseUrl();
    }

    private String currentChatPath(String provider) {
        if (LlmProvider.DEEPSEEK.equalsIgnoreCase(provider)) {
            return properties.getDeepseekChatPath();
        }
        String apiKey = currentApiKey(provider);
        if (apiKey != null && apiKey.trim().startsWith("tp-")) {
            return "/chat/completions";
        }
        return properties.getChatPath();
    }

    private String failureReason(Exception exception) {
        if (exception instanceof RestClientResponseException responseException) {
            String body = responseException.getResponseBodyAsString();
            if (body == null || body.isBlank()) {
                return "HTTP " + responseException.getStatusCode().value();
            }
            return "HTTP " + responseException.getStatusCode().value() + "：" + compact(body);
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : compact(message);
    }

    private String compact(String value) {
        String compacted = value.replaceAll("\\s+", " ").trim();
        return compacted.length() > 140 ? compacted.substring(0, 140) + "..." : compacted;
    }

    private String normalizeModelName(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            throw new BusinessException("模型名称不能为空");
        }
        return modelName.trim();
    }

    private String fallbackAnswer(String question, List<Book> books) {
        StringBuilder builder = new StringBuilder();
        builder.append("当前未配置 API Key，系统使用本地规则推荐。\n\n");
        builder.append("你的问题是：").append(question.trim()).append("。\n");
        builder.append("可优先查看：\n");
        books.stream()
                .filter(book -> book.getAvailableCopies() > 0)
                .limit(3)
                .forEach(book -> builder
                        .append("- 《")
                        .append(book.getTitle())
                        .append("》（")
                        .append(book.getCategoryName())
                        .append("，可借 ")
                        .append(book.getAvailableCopies())
                        .append(" 本）\n"));
        return builder.toString();
    }

    private String summarizeBooks(List<Book> books) {
        StringBuilder builder = new StringBuilder();
        books.stream().limit(20).forEach(book -> builder
                .append("书名=")
                .append(book.getTitle())
                .append(", 作者=")
                .append(book.getAuthor())
                .append(", 分类=")
                .append(book.getCategoryName())
                .append(", 可借=")
                .append(book.getAvailableCopies())
                .append("; "));
        return builder.toString();
    }
}


