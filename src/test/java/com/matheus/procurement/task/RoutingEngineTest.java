package com.matheus.procurement.task;

import com.matheus.procurement.agent.Agent;
import com.matheus.procurement.agent.AgentRepository;
import com.matheus.procurement.agent.AgentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoutingEngineTest {

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private RoutingEngine routingEngine;

    @Test
    void shouldRouteToAgentWithMatchingCapability() {
        Agent agent = new Agent();
        agent.setStatus(AgentStatus.AVAILABLE);
        agent.setCapabilities(new String[]{"pdf_extract"});

        Task task = new Task();
        task.setType("pdf_extract");

        when(agentRepository.findByStatus(AgentStatus.AVAILABLE)).thenReturn(List.of(agent));

        Optional<Agent> result = routingEngine.route(task);

        assertThat(result).contains(agent);
    }

    @Test
    void shouldRouteToAgentWithNoMatchingCapability() {
        Agent agent = new Agent();
        agent.setStatus(AgentStatus.AVAILABLE);
        agent.setCapabilities(new String[]{"email_send"});

        Task task = new Task();
        task.setType("pdf_extract");

        when(agentRepository.findByStatus(AgentStatus.AVAILABLE)).thenReturn(List.of(agent));

        Optional<Agent> result = routingEngine.route(task);

        assertThat(result).isEmpty();
    }
}
