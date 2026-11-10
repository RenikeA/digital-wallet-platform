package com.walletplatform.walletservice.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletEvent(
        UUID eventId,
        String eventType,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime occurredAt
) {
}
