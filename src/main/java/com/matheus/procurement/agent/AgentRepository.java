package com.matheus.procurement.agent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

public interface AgentRepository extends JpaRepository<Agent, UUID> {

}
