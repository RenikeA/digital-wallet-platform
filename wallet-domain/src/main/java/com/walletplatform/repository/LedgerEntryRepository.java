package com.walletplatform.repository;

import com.walletplatform.domain.LedgerEntry;
import com.walletplatform.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByTransaction(Transaction transaction);
}