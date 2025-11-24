package com.example.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para crear una orden
 *
 * ⭐ CON VALIDACIONES BEAN VALIDATION ⭐
 *
 * Las anotaciones de validación aseguran que:
 * - Los campos requeridos no sean null
 * - Los valores estén en rangos válidos
 * - Los datos cumplan reglas de negocio
 *
 * ANOTACIONES USADAS:
 * - @NotNull: El campo no puede ser null
 * - @Min: Valor mínimo permitido
 *
 * FLUJO DE VALIDACIÓN:
 * 1. Request llega al controller
 * 2. Spring valida el objeto con @Valid
 * 3. Si hay errores → MethodArgumentNotValidException
 * 4. GlobalExceptionHandler captura y devuelve 400 Bad Request
 * 5. Si OK → continúa al método del controller
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    /**
     * ID del producto a ordenar
     * - No puede ser null
     * - Debe ser mayor a 0
     */
    @NotNull(message = "El ID del producto es requerido")
    @Min(value = 1, message = "El ID del producto debe ser mayor a 0")
    private Long productId;

    /**
     * Cantidad a ordenar
     * - No puede ser null
     * - Debe ser al menos 1
     */
    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;
}
