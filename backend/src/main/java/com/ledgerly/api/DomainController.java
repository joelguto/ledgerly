package com.ledgerly.api;

import com.ledgerly.api.dto.MerchantCreateRequest;
import com.ledgerly.api.dto.OutcomeRequest;
import com.ledgerly.api.dto.TransactionCreateRequest;
import com.ledgerly.domain.DomainService;
import com.ledgerly.domain.TransactionState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ledger")
public class DomainController {

    private final DomainService domain;

    public DomainController(DomainService domain) {
        this.domain = domain;
    }

    @PostMapping("/merchants")
    public ResponseEntity<Void> createMerchant(@RequestBody MerchantCreateRequest req) {
        domain.createMerchant(req.getId(), req.getName(), req.getStatus());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/merchants/{id}")
    public ResponseEntity<Map<String, Object>> getMerchant(@PathVariable String id) {
        Map<String, Object> m = domain.getMerchant(id);
        return m == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(m);
    }

    @PostMapping("/transactions")
    public ResponseEntity<Void> createTransaction(@RequestBody TransactionCreateRequest req) {
        domain.createTransaction(
                req.getId(),
                req.getMerchantId(),
                req.getAmount(),
                req.getCurrency(),
                parseInstant(req.getExpiresAt()),
                req.getMetadata());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<Map<String, Object>> getTransaction(@PathVariable String id) {
        Map<String, Object> tx = domain.getTransaction(id);
        return tx == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(tx);
    }

    @GetMapping("/transactions")
    public List<Map<String, Object>> listTransactions(@RequestParam(name = "merchant_id", required = false) String merchantId,
                                                      @RequestParam(name = "state", required = false) String state) {
        TransactionState s = state == null ? null : TransactionState.valueOf(state.toUpperCase());
        return domain.listTransactions(merchantId, s);
    }

    @PostMapping("/transactions/{id}/outcome")
    public ResponseEntity<Void> assertOutcome(@PathVariable String id, @RequestBody OutcomeRequest req) {
        TransactionState outcome = TransactionState.valueOf(req.getStatus().toUpperCase());
        domain.assertOutcome(id, outcome, req.getExternalReference(), parseInstant(req.getReportedAt()), req.getMetadata());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transactions/expire")
    public Map<String, Object> expire() {
        int updated = domain.expirePending();
        return Map.of("expired", updated);
    }

    private Instant parseInstant(String maybe) {
        if (maybe == null || maybe.isBlank()) return null;
        return Instant.parse(maybe);
    }
}
