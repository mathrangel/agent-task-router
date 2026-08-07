package com.matheus.procurement.execution;

import com.matheus.procurement.task.Task;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ExecutionEngine {

    @Async
    public CompletableFuture<String> execute(Task task) {
        String name = Thread.currentThread().getName();

        return CompletableFuture.completedFuture(name);
    }
}
