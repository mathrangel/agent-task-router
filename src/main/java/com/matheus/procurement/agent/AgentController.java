package com.matheus.procurement.agent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/agents")
public class AgentController {
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Agent>> getAgents() {
        List<Agent> agents = agentService.findAll();
        return ResponseEntity.ok(agents);
    };

    @GetMapping("/{id}")
    public ResponseEntity<Agent> getAgent(@PathVariable UUID id) {
        Optional<Agent> agent = agentService.findById(id);
        return agent.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    };

    @PostMapping("/")
    public ResponseEntity<Agent> createAgent(@RequestBody Agent agent) {
        return ResponseEntity.ok(agentService.create(agent));
    };

    @DeleteMapping("/{id}")
    public ResponseEntity<Agent> deleteAgent(@PathVariable UUID id) {
        Optional<Agent> agent = agentService.findById(id);
        if (agent.isPresent()) {
            agentService.delete(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();

    };
}
