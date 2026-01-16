package com.ledgerly.domain;

import com.ledgerly.engine.Condition;
import com.ledgerly.engine.ConditionOperator;
import com.ledgerly.engine.LedgerEngine;
import com.ledgerly.engine.RowPredicate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DomainService {

    private final LedgerEngine engine;

    public DomainService(LedgerEngine engine) {
        this.engine = engine;
    }

    public void createMerchant(String id, String name, String status) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
        ensureAbsent("merchants", "id", id, "Merchant already exists");
        engine.insert("merchants", Map.of(
                "id", id,
                "name", name,
                "status", status,
                "created_at", Instant.now().toString()
        ));
    }

    public Map<String, Object> getMerchant(String id) {
        return singleOrNull(selectOne("merchants", id, "id"));
    }

    public void createTransaction(String id,
                                  String merchantId,
                                  long amount,
                                  String currency,
                                  Instant expiresAt,
                                  String metadata) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(merchantId, "merchantId");
        Objects.requireNonNull(currency, "currency");
        ensureExists("merchants", "id", merchantId, "Merchant not found");
        ensureAbsent("transactions", "id", id, "Transaction already exists");
        Instant now = Instant.now();
        Instant expiry = expiresAt != null ? expiresAt : now.plusSeconds(3600);
        Map<String, Object> values = new HashMap<>();
        values.put("id", id);
        values.put("merchant_id", merchantId);
        values.put("amount", amount);
        values.put("currency", currency);
        values.put("state", TransactionState.PENDING.name());
        values.put("created_at", now.toString());
        values.put("expires_at", expiry.toString());
        values.put("metadata", metadata);
        engine.insert("transactions", values);
    }

    public Map<String, Object> getTransaction(String id) {
        return singleOrNull(selectOne("transactions", id, "id"));
    }

    public List<Map<String, Object>> listTransactions(String merchantId, TransactionState state) {
        List<Condition> conditions = new ArrayList<>();
        if (merchantId != null) {
            conditions.add(new Condition("merchant_id", ConditionOperator.EQ, merchantId));
        }
        if (state != null) {
            conditions.add(new Condition("state", ConditionOperator.EQ, state.name()));
        }
        RowPredicate predicate = conditions.isEmpty() ? null : new RowPredicate(conditions);
        return engine.select("transactions", null, predicate);
    }

    public void assertOutcome(String txId,
                              TransactionState outcomeState,
                              String externalRef,
                              Instant reportedAt,
                              String metadata) {
        Map<String, Object> tx = getTransaction(txId);
        if (tx == null) {
            throw new IllegalArgumentException("Transaction not found");
        }
        String currentState = (String) tx.get("state");
        if (!TransactionState.PENDING.name().equals(currentState)) {
            throw new IllegalArgumentException("Transaction not pending");
        }
        Instant when = reportedAt != null ? reportedAt : Instant.now();

        // Upsert outcome row
        RowPredicate pred = new RowPredicate(List.of(new Condition("tx_id", ConditionOperator.EQ, txId)));
        int updated = engine.update("outcomes", pred, Map.of(
                "status", outcomeState.name(),
                "external_reference", externalRef,
                "reported_at", when.toString(),
                "metadata", metadata
        ));
        if (updated == 0) {
            engine.insert("outcomes", Map.of(
                    "tx_id", txId,
                    "status", outcomeState.name(),
                    "external_reference", externalRef,
                    "reported_at", when.toString(),
                    "metadata", metadata
            ));
        }

        engine.update("transactions", pred, Map.of("state", outcomeState.name()));
    }

    public int expirePending() {
        Instant now = Instant.now();
        List<Condition> conditions = List.of(
                new Condition("state", ConditionOperator.EQ, TransactionState.PENDING.name()),
                new Condition("expires_at", ConditionOperator.LT, now.toEpochMilli())
        );
        RowPredicate predicate = new RowPredicate(conditions);
        return engine.update("transactions", predicate, Map.of("state", TransactionState.EXPIRED.name()));
    }

    private void ensureExists(String table, String col, Object value, String message) {
        if (selectOne(table, value, col).isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void ensureAbsent(String table, String col, Object value, String message) {
        if (!selectOne(table, value, col).isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private Map<String, Object> singleOrNull(List<Map<String, Object>> rows) {
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, Object>> selectOne(String table, Object value, String col) {
        RowPredicate predicate = new RowPredicate(List.of(new Condition(col, ConditionOperator.EQ, value)));
        return engine.select(table, null, predicate);
    }
}
