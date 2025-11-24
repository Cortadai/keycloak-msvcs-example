package com.example.user.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 *
 * ⭐ MANEJO CENTRALIZADO DE ERRORES ⭐
 *
 * ¿QUÉ HACE?
 * ==========
 *
 * Captura TODAS las excepciones lanzadas en los controllers
 * y devuelve respuestas JSON consistentes y descriptivas.
 *
 * SIN ESTE HANDLER:
 * -----------------
 * - Excepciones muestran stack traces
 * - Respuestas inconsistentes
 * - Información técnica expuesta al cliente
 * - Difícil de debuggear
 *
 * CON ESTE HANDLER:
 * -----------------
 * - Respuestas JSON uniformes
 * - Mensajes descriptivos
 * - Status codes apropiados
 * - Logs estructurados
 * - Información sensible protegida
 *
 * TIPOS DE EXCEPCIONES MANEJADAS:
 * ================================
 *
 * 1. MethodArgumentNotValidException
 *    - Se lanza cuando @Valid falla
 *    - Devuelve 400 Bad Request
 *    - Incluye detalles de cada campo inválido
 *
 * 2. AccessDeniedException
 *    - Se lanza cuando @PreAuthorize falla
 *    - Usuario autenticado pero sin permisos
 *    - Devuelve 403 Forbidden
 *
 * 3. AuthenticationException
 *    - Se lanza cuando JWT es inválido
 *    - Usuario no autenticado
 *    - Devuelve 401 Unauthorized
 *
 * 4. ResourceNotFoundException (custom)
 *    - Se lanza cuando un recurso no existe
 *    - Devuelve 404 Not Found
 *
 * 5. Exception (catch-all)
 *    - Cualquier otra excepción no manejada
 *    - Devuelve 500 Internal Server Error
 *    - Loggea el error completo
 *
 * FORMATO DE RESPUESTA:
 * =====================
 *
 * {
 *   "timestamp": "2025-01-22T10:30:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "details": {
 *     "username": "El username es requerido",
 *     "email": "El email debe tener formato válido"
 *   },
 *   "path": "/users"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja errores de validación (@Valid)
     *
     * CUÁNDO SE LANZA:
     * - Request body con campos inválidos
     * - @NotNull, @Min, @Email, etc. fallan
     *
     * EJEMPLO:
     * POST /orders
     * {
     *   "productId": null,  ← @NotNull falla
     *   "quantity": 0       ← @Min(1) falla
     * }
     *
     * @param ex Excepción con detalles de validación
     * @return 400 Bad Request con detalles de errores
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Extraer errores de cada campo
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
            ));

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message("Validation failed")
            .details(fieldErrors)
            .build();

        log.warn("Validation Error: {}", fieldErrors);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    /**
     * Maneja errores de autorización (@PreAuthorize)
     *
     * CUÁNDO SE LANZA:
     * - Usuario autenticado (JWT válido)
     * - Pero NO tiene el role requerido
     *
     * EJEMPLO:
     * Usuario con role "USER" intenta:
     * GET /users/admin-only  ← Requiere role "ADMIN"
     *
     * @param ex Excepción de acceso denegado
     * @return 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Forbidden")
            .message("No tienes permisos para acceder a este recurso")
            .details(Map.of("reason", "Insufficient privileges"))
            .build();

        log.warn("Access Denied: {}", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(errorResponse);
    }

    /**
     * Maneja errores de autenticación (JWT inválido)
     *
     * CUÁNDO SE LANZA:
     * - JWT ausente
     * - JWT expirado
     * - JWT con firma inválida
     * - JWT con issuer incorrecto
     *
     * EJEMPLO:
     * GET /users/me
     * Authorization: Bearer expired-or-invalid-token
     *
     * @param ex Excepción de autenticación
     * @return 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationError(AuthenticationException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .message("Authentication failed")
            .details(Map.of("reason", ex.getMessage()))
            .build();

        log.warn("Authentication Error: {}", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(errorResponse);
    }

    /**
     * Maneja errores de recurso no encontrado
     *
     * CUÁNDO SE LANZA:
     * - GET /users/999 (usuario no existe)
     * - GET /orders/999 (orden no existe)
     *
     * @param ex Excepción custom
     * @return 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .details(Map.of("resource", ex.getResourceName()))
            .build();

        log.warn("Resource Not Found: {}", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse);
    }

    /**
     * Maneja errores genéricos (catch-all)
     *
     * CUÁNDO SE LANZA:
     * - Cualquier excepción no manejada arriba
     * - Errores inesperados
     * - Bugs en el código
     *
     * @param ex Excepción genérica
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .details(Map.of("type", ex.getClass().getSimpleName()))
            .build();

        // Log completo del error para debugging
        log.error("Unexpected Error - Type: {}, Message: {}", ex.getClass().getName(), ex.getMessage(), ex);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }

    /**
     * TESTING:
     * ========
     *
     * 1. Validation Error (400):
     *    curl -X POST http://localhost:8081/api/orders \
     *      -H "Authorization: Bearer $TOKEN" \
     *      -H "Content-Type: application/json" \
     *      -d '{"productId": null, "quantity": 0}'
     *
     * 2. Access Denied (403):
     *    curl -H "Authorization: Bearer $USER_TOKEN" \
     *      http://localhost:8081/api/users/admin-only
     *
     * 3. Unauthorized (401):
     *    curl http://localhost:8081/api/users/me
     *    (sin JWT)
     *
     * 4. Not Found (404):
     *    curl -H "Authorization: Bearer $TOKEN" \
     *      http://localhost:8081/api/users/999
     */
}
