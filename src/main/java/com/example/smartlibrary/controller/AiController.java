package com.example.smartlibrary.controller;

import com.example.smartlibrary.dto.AiChatRequest;
import com.example.smartlibrary.service.LlmService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final LlmService llmService;

    public AiController(LlmService llmService) {
        this.llmService = llmService;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody AiChatRequest request) {
        return Map.of("answer", llmService.ask(request.question()));
    }
}
