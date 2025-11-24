package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de Orden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String username;        // Del User Service
    private Long productId;
    private String productName;      // Del Product Service
    private BigDecimal productPrice; // Del Product Service
    private Integer quantity;
    private BigDecimal totalPrice;   // Calculado: productPrice * quantity
    private LocalDateTime createdAt;
}
