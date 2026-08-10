package com.matheus.procurement.task;

import com.matheus.procurement.agent.Agent;
import com.matheus.procurement.agent.AgentRepository;
import com.matheus.procurement.execution.ExecutionEngine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final AgentRepository agentRepository;
    private final RoutingEngine routingEngine;
    private final ExecutionEngine executionEngine;

    public TaskService(TaskRepository taskRepository,
                       AgentRepository agentRepository,
                       RoutingEngine routingEngine,
                       ExecutionEngine executionEngine) {
        this.taskRepository = taskRepository;
        this.agentRepository = agentRepository;
        this.routingEngine = routingEngine;
        this.executionEngine = executionEngine;
    }

    public Task create(Task task) {
       Optional<Agent> agentOptional = routingEngine.route(task);
       agentOptional.ifPresent(agent -> task.setAgentId(agent.getId()));
        Task savedTask = taskRepository.save(task);
        if(savedTask.getAgentId() != null) {
            executionEngine.execute(savedTask);
        }

        return savedTask;
    }

    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    public Optional<Task> findById(UUID id) {
        return taskRepository.findById(id);
    }

    public List<Task> findByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public void delete(UUID id) {
        taskRepository.deleteById(id);
    }
}
