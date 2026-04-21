package com.example.productservice.integration;

import com.example.productservice.dto.CreateProductRequest;
import com.example.productservice.dto.ProductDto;
import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebMvc
class ProductServiceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        productRepository.deleteAll();
    }

    @Test
    void shouldCreateProduct() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "Test Product", 
                "Test Description", 
                BigDecimal.valueOf(99.99), 
                10
        );

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.stockQuantity").value(10));
    }

    @Test
    void shouldGetAllProducts() throws Exception {
        // Create test products
        Product product1 = new Product("Product 1", "Description 1", BigDecimal.valueOf(10.0), 5);
        Product product2 = new Product("Product 2", "Description 2", BigDecimal.valueOf(20.0), 10);
        productRepository.saveAll(List.of(product1, product2));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Product 1"))
                .andExpect(jsonPath("$[1].name").value("Product 2"));
    }

    @Test
    void shouldGetProductById() throws Exception {
        Product product = new Product("Test Product", "Description", BigDecimal.valueOf(15.0), 8);
        Product savedProduct = productRepository.save(product);

        mockMvc.perform(get("/products/" + savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateStock() throws Exception {
        Product product = new Product("Test Product", "Description", BigDecimal.valueOf(15.0), 8);
        Product savedProduct = productRepository.save(product);

        mockMvc.perform(put("/products/" + savedProduct.getId() + "/stock")
                .param("quantity", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(20));
    }

    @Test
    void shouldCheckStock() throws Exception {
        Product product = new Product("Test Product", "Description", BigDecimal.valueOf(15.0), 8);
        Product savedProduct = productRepository.save(product);

        mockMvc.perform(get("/products/" + savedProduct.getId() + "/stock/check")
                .param("requiredQuantity", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        mockMvc.perform(get("/products/" + savedProduct.getId() + "/stock/check")
                .param("requiredQuantity", "10"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void shouldPersistProductInDatabase() {
        // Given
        Product product = new Product("Persist Test", "Description", BigDecimal.valueOf(25.0), 15);
        
        // When
        Product savedProduct = productRepository.save(product);
        
        // Then
        assertTrue(productRepository.existsById(savedProduct.getId()));
        
        Product retrievedProduct = productRepository.findById(savedProduct.getId()).orElse(null);
        assertNotNull(retrievedProduct);
        assertEquals("Persist Test", retrievedProduct.getName());
        assertEquals(BigDecimal.valueOf(25.0), retrievedProduct.getPrice());
        assertEquals(15, retrievedProduct.getStockQuantity());
    }
}
