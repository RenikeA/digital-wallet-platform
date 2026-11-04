package com.walletplatform.walletservice.specifications;

import com.walletplatform.domain.EntryType;
import com.walletplatform.domain.LedgerEntry;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerEntrySpecifications {

    public static Specification<LedgerEntry> forWallet(UUID walletId) {
        return (root, query, cb)
                -> cb.equal(root.get("wallet").get("id"), walletId);
    }

    public static Specification<LedgerEntry> hasType(EntryType type) {
        return (root, query, cb)
                -> cb.equal(root.get("entryType"), type);
    }

    public static Specification<LedgerEntry> createdFrom(LocalDateTime start) {
        return (root, query, cb)
                -> cb.greaterThanOrEqualTo(root.get("createdAt"), start);
    }

    public static Specification<LedgerEntry> createdBefore(LocalDateTime end) {
        return (root, query, cb)
                -> cb.lessThan(root.get("createdAt"), end);
    }
}