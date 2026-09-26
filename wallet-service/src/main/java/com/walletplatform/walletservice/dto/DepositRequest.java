package com.walletplatform.walletservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {
    @NotNull(message = "wallet ID cannot be null")
    private UUID walletId;

    @NotNull(message = "Amount cannot be null")
    @Positive(message ="Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key cannot be blank")
    private String idempotencyKey;


}
