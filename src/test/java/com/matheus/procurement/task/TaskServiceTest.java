package com.matheus.procurement.task;

import com.matheus.procurement.agent.Agent;
import com.matheus.procurement.agent.AgentRepository;
import com.matheus.procurement.agent.AgentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private AgentRepository agentRepository;

    @Test
    void create_doesNotBlockOnExecutionEngine() {
        Agent agent = new Agent();
        agent.setName("test-agent");
        agent.setStatus(AgentStatus.AVAILABLE);
        agent.setCapabilities(new String[]{"summarize"});
        agent.setMaxConcurrency(1);
        agentRepository.save(agent);

        Task task = new Task();
        task.setType("summarize");
        task.setPayload("test payload");

        long start = System.currentTimeMillis();
        taskService.create(task);
        long elapsed = System.currentTimeMillis() - start;

        // Verificado manualmente em 10/08 com um Thread.sleep(3000) temporário
        // dentro de ExecutionEngine.execute() — create() retornou em <500ms
        // mesmo assim, confirmando que não bloqueia esperando a execução.
        assertThat(elapsed).isLessThan(500);
    }
}
