package com.matheus.procurement.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    // getters e setters (ou @Data do Lombok)
}
