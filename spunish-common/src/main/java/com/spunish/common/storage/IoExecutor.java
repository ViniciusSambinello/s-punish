package com.spunish.common.storage;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class IoExecutor implements AutoCloseable {

    private final ExecutorService executor;
    private final Duration queryTimeout;

    public IoExecutor(Duration queryTimeout) {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.queryTimeout = queryTimeout;
    }

    public <T> CompletableFuture<T> submit(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                future.complete(task.call());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future.orTimeout(queryTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        executor.close();
    }
}
