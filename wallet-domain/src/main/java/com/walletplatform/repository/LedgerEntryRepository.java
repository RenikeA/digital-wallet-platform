package com.walletplatform.repository;
import com.walletplatform.domain.LedgerEntry;
import com.walletplatform.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID>, JpaSpecificationExecutor<LedgerEntry> {
    List<LedgerEntry>findByTransaction(Transaction transaction);

}