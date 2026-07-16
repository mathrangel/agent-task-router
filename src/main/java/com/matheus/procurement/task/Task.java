package com.matheus.procurement.task;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false,name = "agent_id")
    private UUID agentId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column
    private TaskStatus status;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
