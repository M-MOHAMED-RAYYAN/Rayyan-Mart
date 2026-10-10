package com.shopping.system.dto;

import java.time.LocalDateTime;

public record ChatResponse(
        String reply,
        String provider,
        boolean fallback,
        LocalDateTime timestamp
) {
}