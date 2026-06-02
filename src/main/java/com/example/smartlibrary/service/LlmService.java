package com.example.smartlibrary.service;

import com.example.smartlibrary.config.LlmProperties;
import com.example.smartlibrary.model.Book;
import com.example.smartlibrary.repository.BookRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LlmService {

    private final LlmProperties properties;
    private final BookRepository bookRepository;
    private final ObjectMapper objectMapper;

    public LlmService(LlmProperties properties, BookRepository bookRepository, ObjectMapper objectMapper) {
        this.properties = properties;
        this.bookRepository = bookRepository;
        this.objectMapper = objectMapper;
    }

    public String ask(String question) {
        if (question == null || question.isBlank()) {
            return "请输入问题，例如：请根据 JavaEE 大作业推荐两本适合学习的书。";
        }
        List<Book> books = bookRepository.findAll(null);
        if (!properties.hasApiKey()) {
            return fallbackAnswer(question, books);
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", properties.getModel());
            ArrayNode messages = body.putArray("messages");
            messages.addObject()
                    .put("role", "system")
                    .put("content", "你是智能图书管理系统的图书推荐助手。请基于馆藏数据回答，中文输出，简洁实用。");
            messages.addObject()
                    .put("role", "user")
                    .put("content", "馆藏数据：" + summarizeBooks(books) + "\n用户问题：" + question.trim());

            JsonNode response = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .build()
                    .post()
                    .uri(properties.getChatPath())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode content = response.path("choices").path(0).path("message").path("content");
            if (content.isTextual() && !content.asText().isBlank()) {
                return content.asText();
            }
            return "大模型已响应，但没有返回有效文本。";
        } catch (Exception exception) {
            return "大模型接口调用失败，已切换本地推荐：" + fallbackAnswer(question, books);
        }
    }

    private String fallbackAnswer(String question, List<Book> books) {
        StringBuilder builder = new StringBuilder();
        builder.append("当前未配置 LLM_API_KEY，系统使用本地规则推荐。");
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
