package com.matheus.procurement.agent;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AgentService {
    private final AgentRepository agentRepository;

    public AgentService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    public Agent create(Agent agent) {
        return agentRepository.save(agent);

    }

    public List<Agent> findAll() {
        return agentRepository.findAll();
    }

    public Optional<Agent> findById(UUID id) {
        return agentRepository.findById(id);
    }

    public void delete(UUID id) {
        agentRepository.deleteById(id);
    }
}
