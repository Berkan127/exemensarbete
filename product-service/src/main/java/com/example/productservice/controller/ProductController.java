package com.example.productservice.controller;

import com.example.productservice.dto.CreateProductRequest;
import com.example.productservice.dto.ProductDto;
import com.example.productservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody CreateProductRequest request) {
        logger.info("Received request to create product: {}", request.getName());
        
        ProductDto createdProduct = productService.createProduct(request);
        logger.info("Product created successfully with ID: {}", createdProduct.getId());
        
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        logger.info("Received request to get all products");
        
        List<ProductDto> products = productService.getAllProducts();
        logger.info("Returning {} products", products.size());
        
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        logger.info("Received request to get product with ID: {}", id);
        
        ProductDto product = productService.getProductById(id);
        logger.info("Returning product: {}", product.getName());
        
        return ResponseEntity.ok(product);
    }
    
    @PutMapping("/{id}/stock")
    public ResponseEntity<ProductDto> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        logger.info("Received request to update stock for product ID: {} with quantity: {}", id, quantity);
        
        ProductDto updatedProduct = productService.updateStock(id, quantity);
        logger.info("Stock updated successfully for product ID: {}", id);
        
        return ResponseEntity.ok(updatedProduct);
    }
    
    @GetMapping("/{id}/stock/check")
    public ResponseEntity<Boolean> checkStock(
            @PathVariable Long id,
            @RequestParam Integer requiredQuantity) {
        logger.info("Received request to check stock for product ID: {}, required quantity: {}", id, requiredQuantity);
        
        boolean hasStock = productService.checkStock(id, requiredQuantity);
        logger.info("Stock check result for product ID: {}: {}", id, hasStock);
        
        return ResponseEntity.ok(hasStock);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto) {
        logger.info("Received request to update product with ID: {}", id);
        
        // Skapa en CreateProductRequest från ProductDto för att matcha befintlig metod
        CreateProductRequest request = new CreateProductRequest();
        request.setName(productDto.getName());
        request.setDescription(productDto.getDescription());
        request.setPrice(productDto.getPrice());
        request.setStockQuantity(productDto.getStockQuantity());
        
        ProductDto updatedProduct = productService.updateStock(id, productDto.getStockQuantity() - request.getStockQuantity());
        logger.info("Product updated successfully for ID: {}", id);
        
        return ResponseEntity.ok(updatedProduct);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        logger.info("Received request to delete product with ID: {}", id);
        
        productService.deleteProduct(id);
        logger.info("Product deleted successfully for ID: {}", id);
        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDto>> getProductsByCategory(@PathVariable String category) {
        logger.info("Received request to get products by category: {}", category);
        
        List<ProductDto> products = productService.getProductsByCategory(category);
        logger.info("Returning {} products for category: {}", products.size(), category);
        
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<ProductDto>> searchProducts(@RequestParam String name) {
        logger.info("Received request to search products with name: {}", name);
        
        List<ProductDto> products = productService.searchProducts(name);
        logger.info("Returning {} products for search: {}", products.size(), name);
        
        return ResponseEntity.ok(products);
    }
}
