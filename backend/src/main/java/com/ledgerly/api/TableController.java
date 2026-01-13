package com.ledgerly.api;

import com.ledgerly.api.dto.CreateTableRequest;
import com.ledgerly.api.dto.DeleteRequest;
import com.ledgerly.api.dto.InsertRequest;
import com.ledgerly.api.dto.JoinRequest;
import com.ledgerly.api.dto.QueryRequest;
import com.ledgerly.api.dto.UpdateRequest;
import com.ledgerly.engine.LedgerEngine;
import com.ledgerly.engine.RowPredicate;
import com.ledgerly.engine.TableSchema;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tables")
public class TableController {

    private final LedgerEngine engine;

    public TableController(LedgerEngine engine) {
        this.engine = engine;
    }

    @GetMapping
    public List<String> listTables() {
        return engine.listTables();
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody CreateTableRequest request) {
        TableSchema schema = DtoMapper.toSchema(
                request.getName(),
                request.getColumns(),
                request.getPrimaryKey(),
                request.getUniqueConstraints() == null ? List.of() : request.getUniqueConstraints());
        engine.createTable(schema);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{table}/rows")
    public ResponseEntity<Void> insert(@PathVariable String table, @RequestBody InsertRequest request) {
        engine.insert(table, request.getValues());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{table}/rows")
    public Map<String, Object> update(@PathVariable String table, @RequestBody UpdateRequest request) {
        RowPredicate predicate = DtoMapper.toPredicate(request.getFilters());
        int count = engine.update(table, predicate, request.getValues());
        return Map.of("updated", count);
    }

    @DeleteMapping("/{table}/rows")
    public Map<String, Object> delete(@PathVariable String table, @RequestBody DeleteRequest request) {
        RowPredicate predicate = DtoMapper.toPredicate(request.getFilters());
        int count = engine.delete(table, predicate);
        return Map.of("deleted", count);
    }

    @PostMapping("/{table}/query")
    public List<Map<String, Object>> query(@PathVariable String table, @RequestBody QueryRequest request) {
        RowPredicate predicate = DtoMapper.toPredicate(request.getFilters());
        List<String> columns = request.getColumns() == null || request.getColumns().isEmpty()
                ? null
                : request.getColumns();
        return engine.select(table, columns, predicate);
    }

    @PostMapping("/join")
    public List<Map<String, Object>> join(@RequestBody JoinRequest request) {
        return engine.join(
                request.getLeftTable(),
                request.getRightTable(),
                request.getLeftColumn(),
                request.getRightColumn(),
                request.getProjection());
    }
}
