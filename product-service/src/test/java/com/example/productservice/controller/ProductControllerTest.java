package com.example.productservice.controller;

import com.example.productservice.dto.CreateProductRequest;
import com.example.productservice.dto.ProductDto;
import com.example.productservice.exception.ProductNotFoundException;
import com.example.productservice.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductDto sampleProduct;
    private CreateProductRequest createRequest;

    @BeforeEach
    void setUp() {
        sampleProduct = new ProductDto(1L, "Test Product", "Description", new BigDecimal("99.99"), 10);
        createRequest = new CreateProductRequest("Test Product", "Description", new BigDecimal("99.99"), 10);
    }

    @Test
    void createProduct_ShouldReturnCreatedProduct() throws Exception {
        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(sampleProduct);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.stockQuantity").value(10));
    }

    @Test
    void getAllProducts_ShouldReturnAllProducts() throws Exception {
        List<ProductDto> products = Arrays.asList(sampleProduct);
        when(productService.getAllProducts()).thenReturn(products);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Product"));
    }

    @Test
    void getProductById_ShouldReturnProduct() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleProduct);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void getProductById_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        when(productService.getProductById(1L)).thenThrow(new ProductNotFoundException("Product not found"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStock_ShouldReturnUpdatedProduct() throws Exception {
        ProductDto updatedProduct = new ProductDto(1L, "Test Product", "Description", new BigDecimal("99.99"), 15);
        when(productService.updateStock(eq(1L), eq(5))).thenReturn(updatedProduct);

        mockMvc.perform(put("/api/products/1/stock")
                .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(15));
    }

    @Test
    void checkStock_ShouldReturnTrue_WhenStockIsSufficient() throws Exception {
        when(productService.checkStock(eq(1L), eq(5))).thenReturn(true);

        mockMvc.perform(get("/api/products/1/stock/check")
                .param("requiredQuantity", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
