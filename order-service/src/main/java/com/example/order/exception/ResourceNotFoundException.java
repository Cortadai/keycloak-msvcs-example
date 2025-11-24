package com.example.order.exception;

import lombok.Getter;

/**
 * Resource Not Found Exception
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

    public ResourceNotFoundException(String resourceName) {
        super(String.format("%s not found", resourceName));
        this.resourceName = resourceName;
        this.fieldName = "unknown";
        this.fieldValue = "unknown";
    }
}
