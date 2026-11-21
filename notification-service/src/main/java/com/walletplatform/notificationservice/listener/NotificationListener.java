package com.walletplatform.notificationservice.listener;

import com.walletplatform.notificationservice.event.WalletEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationListener {

    @KafkaListener(topics = "wallet-events")
    public void handle(WalletEvent event) {
        String message = switch (event.eventType()) {
            case "WALLET_CREATED"    -> "Your " + event.currency() + " wallet is ready.";
            case "MONEY_DEPOSITED"   -> "You deposited " + event.currency() + " " + event.amount() + ".";
            case "MONEY_WITHDRAWN"   -> "You withdrew " + event.currency() + " " + event.amount() + ".";
            case "TRANSFER_SENT"     -> "You sent " + event.currency() + " " + event.amount() + ".";
            case "TRANSFER_RECEIVED" -> "You received " + event.currency() + " " + event.amount() + ".";
            default                  -> "Unknown event: " + event.eventType();
        };

        log.info("EMAIL to user {}: {}", event.userId(), message);
        log.info("SMS   to user {}: {}", event.userId(), message);
    }
}