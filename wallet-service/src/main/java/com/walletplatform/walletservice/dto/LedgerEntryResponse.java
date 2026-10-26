package com.walletplatform.walletservice.dto;

import com.walletplatform.domain.EntryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID transactionId,
        EntryType entryType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
) {

}
