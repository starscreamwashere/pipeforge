package com.pipeforge.worker.runtime;

import org.springframework.stereotype.Component;

import java.util.UUID;

/** Stable identity for this worker instance (used as the worker name + lock owner). */
@Component
public class WorkerIdentity {

    private final String id = "worker-" + UUID.randomUUID();

    public String get() {
        return id;
    }
}
