package com.shopping.system.controller;

import com.shopping.system.dto.ChatRequest;
import com.shopping.system.dto.ChatResponse;
import com.shopping.system.service.chat.ChatService;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public shopping-assistant endpoint. Requires no login; the rate limit is
 * enforced per HTTP session.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, HttpSession session) {
        return chatService.chat(session.getId(), request.message());
    }
}