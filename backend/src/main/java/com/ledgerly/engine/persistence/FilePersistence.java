package com.ledgerly.engine.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FilePersistence implements Persistence {
    private final Path walPath;
    private final ObjectMapper mapper;

    public FilePersistence(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create data dir: " + dataDir, e);
        }
        this.walPath = dataDir.resolve("ledgerly-wal.jsonl");
        this.mapper = new ObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public List<PersistenceEvent> loadEvents() {
        if (!Files.exists(walPath)) {
            return List.of();
        }
        List<PersistenceEvent> events = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(walPath, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                events.add(mapper.readValue(line, PersistenceEvent.class));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load WAL from " + walPath, e);
        }
        return events;
    }

    @Override
    public synchronized void appendEvent(PersistenceEvent event) {
        try (BufferedWriter writer = Files.newBufferedWriter(walPath,
                StandardCharsets.UTF_8,
                Files.exists(walPath) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE)) {
            writer.write(mapper.writeValueAsString(event));
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to append WAL event", e);
        }
    }
}
