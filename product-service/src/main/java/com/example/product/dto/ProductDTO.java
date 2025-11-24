package com.example.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de Producto
 *
 * Representa un producto en el sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    /**
     * ID del producto
     */
    private Long id;

    /**
     * Nombre del producto
     */
    private String name;

    /**
     * Descripción del producto
     */
    private String description;

    /**
     * Precio del producto
     */
    private BigDecimal price;

    /**
     * Stock disponible
     */
    private Integer stock;
}
