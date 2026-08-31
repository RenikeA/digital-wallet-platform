package com.walletplatform.walletservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateWalletRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Currency is required")
    private String currency;
}