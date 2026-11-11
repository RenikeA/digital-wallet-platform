package com.walletplatform.walletservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventPublisher {
    private static final String TOPIC = "wallet-events";

    private final KafkaTemplate<String, WalletEvent> kafkaTemplate;

    public void publish(WalletEvent event) {
        kafkaTemplate.send(TOPIC, event.walletId().toString(), event);
        log.info("Published {} for wallet {}", event.eventType(), event.walletId());
    }
}
