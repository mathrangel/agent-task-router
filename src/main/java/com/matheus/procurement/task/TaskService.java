package com.matheus.procurement.task;

import com.matheus.procurement.agent.AgentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final AgentRepository agentRepository;


    public TaskService(TaskRepository taskRepository, AgentRepository agentRepository) {
        this.taskRepository = taskRepository;
        this.agentRepository = agentRepository;
    }

    public Task create(Task task) {
        if(agentRepository.findById(task.getAgentId()).isEmpty()){
            throw new IllegalArgumentException("Agent not found: " + task.getAgentId());
        }
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
