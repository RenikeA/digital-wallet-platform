package com.walletplatform.notificationservice.event;

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
        LocalDateTime occurredAt
) {
}
