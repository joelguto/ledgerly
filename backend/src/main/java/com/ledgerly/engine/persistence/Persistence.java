package com.ledgerly.engine.persistence;

import java.util.List;

public interface Persistence {
    List<PersistenceEvent> loadEvents();

    void appendEvent(PersistenceEvent event);
}
