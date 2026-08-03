package com.matheus.procurement.task;

import com.matheus.procurement.agent.Agent;
import com.matheus.procurement.agent.AgentRepository;
import com.matheus.procurement.agent.AgentStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class RoutingEngine  {

    private final AgentRepository agentRepository;

    public RoutingEngine(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    public Optional<Agent> route(Task task) {
        List<Agent> availableAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE);

        for (Agent agent : availableAgents) {
            if (Arrays.asList(agent.getCapabilities()).contains(task.getType())) {
                return Optional.of(agent);
            }
        }

        return Optional.empty();
    }
}
