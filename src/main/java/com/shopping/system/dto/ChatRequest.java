package com.shopping.system.dto;

import javax.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Message is required")
        String message
) {
}