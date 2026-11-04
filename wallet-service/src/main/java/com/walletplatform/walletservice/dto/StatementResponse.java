package com.walletplatform.walletservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;;
import java.util.List;
import java.util.UUID;

public record StatementResponse(
        UUID walletId,
        String currency,
        LocalDate from,
        LocalDate to,
        BigDecimal openingBalance,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        BigDecimal closingBalance,
        List<LedgerEntryResponse>entries
        ) {}
