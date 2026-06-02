package com.example.smartlibrary.service;

import com.example.smartlibrary.config.LlmProperties;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.model.Book;
import com.example.smartlibrary.repository.AiModelRepository;
import com.example.smartlibrary.repository.AiSettingsRepository;
import com.example.smartlibrary.repository.BookRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class LlmService {

    private static final String MIMO_TOKEN_PLAN_BASE_URL = "https://token-plan-cn.xiaomimimo.com/v1";

    private final LlmProperties properties;
    private final BookRepository bookRepository;
    private final AiModelRepository aiModelRepository;
    private final AiSettingsRepository aiSettingsRepository;
    private final ObjectMapper objectMapper;

    public LlmService(
            LlmProperties properties,
            BookRepository bookRepository,
            AiModelRepository aiModelRepository,
            AiSettingsRepository aiSettingsRepository,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.bookRepository = bookRepository;
        this.aiModelRepository = aiModelRepository;
        this.aiSettingsRepository = aiSettingsRepository;
        this.objectMapper = objectMapper;
    }

    public String ask(String question) {
        if (question == null || question.isBlank()) {
            return "请输入问题，例如：请根据 JavaEE 大作业推荐两本适合学习的书。";
        }
        List<Book> books = bookRepository.findAll(null);
        if (!hasApiKey()) {
            return fallbackAnswer(question, books);
        }
        try {
            JsonNode response = requestChat(
                    currentModelName(),
                    "你是智能图书管理系统的图书推荐助手。请基于馆藏数据回答，中文输出，简洁实用。",
                    "馆藏数据：" + summarizeBooks(books) + "\n用户问题：" + question.trim());
            JsonNode content = chatContent(response);
            if (content.isTextual() && !content.asText().isBlank()) {
                return content.asText();
            }
            return "大模型已响应，但没有返回有效文本。";
        } catch (Exception exception) {
            return "大模型接口调用失败（" + failureReason(exception) + "），已切换本地推荐："
                    + fallbackAnswer(question, books);
        }
    }

    public void testModel(String modelName) {
        String normalized = normalizeModelName(modelName);
        if (!hasApiKey()) {
            throw new BusinessException("请先在管理员页面配置 API Key 后再测试模型");
        }
        try {
            JsonNode response = requestChat(
                    normalized,
                    "你是模型连通性测试助手。",
                    "请只回复：测试成功");
            JsonNode content = chatContent(response);
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new BusinessException("模型测试失败：接口未返回有效文本");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("模型测试失败，请检查模型名称、API Key 或接口地址");
        }
    }

    private JsonNode requestChat(String modelName, String systemPrompt, String userPrompt) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", modelName);
        ArrayNode messages = body.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", systemPrompt);
        messages.addObject()
                .put("role", "user")
                .put("content", userPrompt);

        return RestClient.builder()
                .baseUrl(currentBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + currentApiKey())
                .build()
                .post()
                .uri(currentChatPath())
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    private JsonNode chatContent(JsonNode response) {
        return response.path("choices").path(0).path("message").path("content");
    }

    private String currentModelName() {
        return aiSettingsRepository.findActiveModelName()
                .or(() -> aiModelRepository.findFirst().map(model -> model.modelName()))
                .orElseGet(properties::getModel);
    }

    private boolean hasApiKey() {
        return currentApiKey() != null && !currentApiKey().isBlank();
    }

    private String currentApiKey() {
        return aiSettingsRepository.find()
                .filter(settings -> settings.hasApiKey())
                .map(settings -> settings.apiKey())
                .orElseGet(properties::getApiKey);
    }

    private String currentBaseUrl() {
        String apiKey = currentApiKey();
        if (apiKey != null && apiKey.trim().startsWith("tp-")) {
            return MIMO_TOKEN_PLAN_BASE_URL;
        }
        return properties.getBaseUrl();
    }

    private String currentChatPath() {
        String apiKey = currentApiKey();
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
        builder.append("当前未配置 API Key，系统使用本地规则推荐。");
        builder.append("你的问题是：").append(question.trim()).append("。");
        builder.append("可优先查看：");
        books.stream()
                .filter(book -> book.availableCopies() > 0)
                .limit(3)
                .forEach(book -> builder
                        .append("《")
                        .append(book.title())
                        .append("》（")
                        .append(book.categoryName())
                        .append("，可借 ")
                        .append(book.availableCopies())
                        .append(" 本）；"));
        return builder.toString();
    }

    private String summarizeBooks(List<Book> books) {
        StringBuilder builder = new StringBuilder();
        books.stream().limit(20).forEach(book -> builder
                .append("书名=")
                .append(book.title())
                .append(", 作者=")
                .append(book.author())
                .append(", 分类=")
                .append(book.categoryName())
                .append(", 可借=")
                .append(book.availableCopies())
                .append("; "));
        return builder.toString();
    }
}
