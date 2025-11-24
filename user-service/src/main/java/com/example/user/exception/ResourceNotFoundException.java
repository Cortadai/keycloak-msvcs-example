package com.example.user.exception;

import lombok.Getter;

/**
 * Resource Not Found Exception
 *
 * Excepción custom para cuando un recurso no existe.
 *
 * EJEMPLOS DE USO:
 * ================
 *
 * throw new ResourceNotFoundException("User", "id", userId);
 * throw new ResourceNotFoundException("Order", "id", orderId);
 * throw new ResourceNotFoundException("Product", "id", productId);
 *
 * MENSAJE GENERADO:
 * User not found with id: 123
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    /**
     * Constructor simplificado cuando solo importa el nombre del recurso
     */
    public ResourceNotFoundException(String resourceName) {
        super(String.format("%s not found", resourceName));
        this.resourceName = resourceName;
        this.fieldName = "unknown";
        this.fieldValue = "unknown";
    }
}
