package com.example.order.client;

import com.example.order.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign Client para Product Service
 *
 * Similar a UserServiceClient, pero llama al Product Service.
 *
 * Llama a: http://product-service/products/*
 *
 * JWT se propaga automáticamente por FeignClientInterceptor.
 */
@FeignClient(name = "product-service", path = "/api")  // ← Nombre en Eureka + base path
public interface ProductServiceClient {

    /**
     * Lista todos los productos.
     *
     * Llama a: GET http://product-service/api/products
     *
     * @return Lista de productos
     */
    @GetMapping("/products")
    List<ProductDTO> getAllProducts();

    /**
     * Obtiene un producto específico.
     *
     * Llama a: GET http://product-service/api/products/{id}
     *
     * @param id ID del producto
     * @return Producto solicitado
     */
    @GetMapping("/products/{id}")
    ProductDTO getProductById(@PathVariable Long id);
}
