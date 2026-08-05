package com.matheus.procurement.execution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExecutionRepository extends JpaRepository<Execution, UUID> {

}
