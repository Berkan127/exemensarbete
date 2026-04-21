package com.example.productservice.service;

import com.example.productservice.dto.CreateProductRequest;
import com.example.productservice.dto.ProductDto;
import com.example.productservice.exception.ProductNotFoundException;
import com.example.productservice.exception.InsufficientStockException;
import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ProductService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    private final ProductRepository productRepository;
    
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    @CacheEvict(value = {"products", "product_by_id"}, allEntries = true)
    public ProductDto createProduct(CreateProductRequest request) {
        logger.info("Creating new product: {}", request.getName());
        
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        
        Product savedProduct = productRepository.save(product);
        logger.info("Product created with ID: {}", savedProduct.getId());
        
        return convertToDto(savedProduct);
    }
    
    @Cacheable(value = "products", key = "'all_products'")
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        logger.info("Fetching all products from database");
        
        List<Product> products = productRepository.findAll();
        logger.info("Found {} products", products.size());
        
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "product_by_id", key = "#id")
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        logger.info("Fetching product with ID: {} from database", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));
        
        logger.info("Found product: {}", product.getName());
        return convertToDto(product);
    }
    
    @CacheEvict(value = {"products", "product_by_id"}, allEntries = true)
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
    
    @Cacheable(value = "product_stock", key = "#productId + '_' + #requiredQuantity")
    @Transactional(readOnly = true)
    public boolean checkStock(Long productId, Integer requiredQuantity) {
        logger.info("Checking stock for product ID: {}, required quantity: {}", productId, requiredQuantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
        
        boolean hasStock = product.getStockQuantity() >= requiredQuantity;
        logger.info("Stock check result for product ID: {}: {}", productId, hasStock);
        
        return hasStock;
    }
    
    @Async
    @Cacheable(value = "product_by_id", key = "#id")
    public CompletableFuture<ProductDto> getProductByIdAsync(Long id) {
        logger.info("Fetching product with ID: {} asynchronously", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));
        
        return CompletableFuture.completedFuture(convertToDto(product));
    }
    
    private ProductDto convertToDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity()
        );
    }
}
