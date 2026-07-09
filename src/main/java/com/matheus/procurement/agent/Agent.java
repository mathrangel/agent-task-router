package com.matheus.procurement.agent;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "agents")
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "endpoint_url")
    private String endpointUrl;

    @Column
    private String[] capabilities;

    @Enumerated(EnumType.STRING)
    @Column
    private AgentStatus status;

    @Column(name = "max_concurrency")
    private Integer maxConcurrency;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
