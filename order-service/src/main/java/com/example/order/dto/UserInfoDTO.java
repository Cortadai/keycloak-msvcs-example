package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de User Info (copiado de User Service)
 *
 * Necesitamos esta clase para deserializar la respuesta del User Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {
    private String username;
    private String email;
    private String name;
    private String givenName;
    private String familyName;
    private List<String> roles;
    private Boolean emailVerified;
}
