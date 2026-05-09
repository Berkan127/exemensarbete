package com.example.productservice.service;

import com.example.productservice.dto.CreateProductRequest;
import com.example.productservice.dto.ProductDto;
import com.example.productservice.exception.ProductNotFoundException;
import com.example.productservice.exception.InsufficientStockException;
import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    private final ProductRepository productRepository;
    
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    public ProductDto createProduct(CreateProductRequest request) {
        logger.info("Creating new product: {}", request.getName());
        
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        
        Product savedProduct = productRepository.save(product);
        logger.info("Product created with ID: {}", savedProduct.getId());
        
        return convertToDto(savedProduct);
    }
    
    public List<ProductDto> getAllProducts() {
        logger.info("Fetching all products from database");
        
        List<Product> products = productRepository.findAll();
        logger.info("Found {} products", products.size());
        
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public ProductDto getProductById(Long id) {
        logger.info("Fetching product with ID: {} from database", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));
        
        logger.info("Found product: {}", product.getName());
        return convertToDto(product);
    }
    
    public ProductDto updateStock(Long productId, Integer quantity) {
        logger.info("Updating stock for product ID: {} with quantity: {}", productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
        
        if (product.getStockQuantity() + quantity < 0) {
            throw new InsufficientStockException("Insufficient stock for product ID: " + productId);
        }
        
        product.setStockQuantity(product.getStockQuantity() + quantity);
        Product savedProduct = productRepository.save(product);
        
        logger.info("Stock updated for product ID: {}. New stock: {}", productId, savedProduct.getStockQuantity());
        return convertToDto(savedProduct);
    }
    
    public boolean checkStock(Long productId, Integer requiredQuantity) {
        logger.info("Checking stock for product ID: {}, required quantity: {}", productId, requiredQuantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
        
        boolean hasStock = product.getStockQuantity() >= requiredQuantity;
        logger.info("Stock check result for product ID: {}: {}", productId, hasStock);
        
        return hasStock;
    }
    
    public ProductDto getProductByIdAsync(Long id) {
        logger.info("Fetching product with ID: {} asynchronously", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));
        
        return convertToDto(product);
    }
    
    public void deleteProduct(Long id) {
        logger.info("Deleting product with ID: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));
        
        productRepository.delete(product);
        logger.info("Product deleted with ID: {}", id);
    }
    
    public List<ProductDto> getProductsByCategory(String category) {
        logger.info("Fetching products by category: {}", category);
        
        List<Product> products = productRepository.findByCategory(category);
        logger.info("Found {} products in category: {}", products.size(), category);
        
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<ProductDto> searchProducts(String name) {
        logger.info("Searching products with name containing: {}", name);
        
        List<Product> products = productRepository.findByNameContainingIgnoreCase(name);
        logger.info("Found {} products matching: {}", products.size(), name);
        
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    private ProductDto convertToDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCategory()
        );
    }
}
