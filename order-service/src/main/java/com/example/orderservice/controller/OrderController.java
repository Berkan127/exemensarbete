package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest request) {
        logger.info("Received request to create order for user: {}", request.getUserId());
        
        OrderDto createdOrder = orderService.createOrder(request);
        logger.info("Order created successfully with ID: {}", createdOrder.getId());
        
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        logger.info("Received request to get all orders");
        
        List<OrderDto> orders = orderService.getAllOrders();
        logger.info("Returning {} orders", orders.size());
        
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        logger.info("Received request to get order with ID: {}", id);
        
        OrderDto order = orderService.getOrderById(id);
        logger.info("Returning order: {}", order.getId());
        
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDto>> getOrdersByUserId(@PathVariable Long userId) {
        logger.info("Received request to get orders for user: {}", userId);
        
        List<OrderDto> orders = orderService.getOrdersByUserId(userId);
        logger.info("Returning {} orders for user: {}", orders.size(), userId);
        
        return ResponseEntity.ok(orders);
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        logger.info("Received request to update status for order ID: {} to: {}", id, status);
        
        OrderDto updatedOrder = orderService.updateOrderStatus(id, status);
        logger.info("Order status updated successfully for ID: {}", id);
        
        return ResponseEntity.ok(updatedOrder);
    }
}
