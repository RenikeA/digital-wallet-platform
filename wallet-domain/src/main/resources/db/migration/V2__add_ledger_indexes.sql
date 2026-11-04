CREATE INDEX idx_ledger_entries_wallet_created
 ON ledger_entries(wallet_id, created_at);

 CREATE INDEX idx_ledger_entries_transaction
     ON ledger_entries (transaction_id);