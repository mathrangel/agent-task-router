package com.matheus.procurement.execution;

import com.matheus.procurement.task.Task;
import com.matheus.procurement.task.TaskRepository;
import com.matheus.procurement.task.TaskStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ExecutionEngine {

    private final TaskRepository taskRepository;

    public ExecutionEngine(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Async
    public CompletableFuture<String> execute(Task task) {
        task.setStatus(TaskStatus.RUNNING);
        taskRepository.save(task);

        String name = Thread.currentThread().getName();

        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);

        return CompletableFuture.completedFuture(name);
    }
}
