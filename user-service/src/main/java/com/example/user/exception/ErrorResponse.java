package com.example.user.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Error Response DTO
 *
 * Estructura uniforme para todas las respuestas de error.
 *
 * FORMATO:
 * {
 *   "timestamp": "2025-01-22T10:30:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "details": {
 *     "field1": "error message 1",
 *     "field2": "error message 2"
 *   },
 *   "path": "/api/users"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    /**
     * Timestamp del error
     */
    private LocalDateTime timestamp;

    /**
     * HTTP status code (400, 401, 403, 404, 500, etc.)
     */
    private int status;

    /**
     * Nombre del error (Bad Request, Unauthorized, etc.)
     */
    private String error;

    /**
     * Mensaje general del error
     */
    private String message;

    /**
     * Detalles específicos del error
     * - Para validación: campo → mensaje de error
     * - Para otros: información adicional
     */
    private Map<String, String> details;

    /**
     * Path del endpoint que generó el error (opcional)
     */
    private String path;
}
