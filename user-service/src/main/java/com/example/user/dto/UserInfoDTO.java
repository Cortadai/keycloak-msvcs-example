package com.example.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO con información del usuario extraída del JWT.
 *
 * Esta clase representa la información que viene en el JWT de Keycloak.
 *
 * CLAIMS DEL JWT:
 * ===============
 *
 * Un JWT de Keycloak contiene (entre otros):
 * - sub: Subject (ID del usuario)
 * - preferred_username: Username
 * - email: Email
 * - email_verified: Si el email fue verificado
 * - name: Nombre completo
 * - given_name: Nombre
 * - family_name: Apellido
 * - realm_access.roles: Roles del usuario
 *
 * EJEMPLO DE JWT DECODED:
 * =======================
 *
 * {
 *   "sub": "12345678-1234-1234-1234-123456789abc",
 *   "preferred_username": "user",
 *   "email": "user@example.com",
 *   "email_verified": true,
 *   "name": "John Doe",
 *   "given_name": "John",
 *   "family_name": "Doe",
 *   "realm_access": {
 *     "roles": ["user", "offline_access"]
 *   },
 *   "iss": "http://localhost:8080/realms/mi-realm",
 *   "aud": "account",
 *   "exp": 1234567890,
 *   "iat": 1234567800
 * }
 *
 * Este DTO extrae la información relevante y la devuelve al cliente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {
    /**
     * Username del usuario (preferred_username en JWT)
     * - Requerido cuando se crea usuario
     * - Mínimo 3 caracteres, máximo 50
     */
    @NotBlank(message = "El username es requerido", groups = CreateUser.class)
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    private String username;

    /**
     * Email del usuario
     * - Requerido cuando se crea usuario
     * - Debe ser formato válido de email
     */
    @NotBlank(message = "El email es requerido", groups = CreateUser.class)
    @Email(message = "El email debe tener formato válido")
    private String email;

    /**
     * Nombre completo del usuario
     */
    private String name;

    /**
     * Nombre (first name)
     */
    private String givenName;

    /**
     * Apellido (last name)
     */
    private String familyName;

    /**
     * Roles del usuario en Keycloak
     */
    private List<String> roles;

    /**
     * Si el email fue verificado
     */
    private Boolean emailVerified;

    /**
     * Validation group para creación de usuario
     * Permite validaciones diferentes según el contexto
     */
    public interface CreateUser {}
}
