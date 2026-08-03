package com.matheus.procurement.task;

import com.matheus.procurement.agent.Agent;
import com.matheus.procurement.agent.AgentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final AgentRepository agentRepository;
    private final RoutingEngine routingEngine;

    public TaskService(TaskRepository taskRepository, AgentRepository agentRepository, RoutingEngine routingEngine) {
        this.taskRepository = taskRepository;
        this.agentRepository = agentRepository;
        this.routingEngine = routingEngine;
    }

    public Task create(Task task) {
       Optional<Agent> agentOptional = routingEngine.route(task);
       agentOptional.ifPresent(agent -> task.setAgentId(agent.getId()));
        return taskRepository.save(task);
    }

    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    public Optional<Task> findById(UUID id) {
        return taskRepository.findById(id);
    }

    public void delete(UUID id) {
        taskRepository.deleteById(id);
    }
}
