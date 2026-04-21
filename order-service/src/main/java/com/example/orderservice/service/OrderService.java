package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderItemRequest;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.dto.OrderItemDto;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderItem;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.repository.OrderItemRepository;
import com.example.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    
    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }
    
    @Transactional
    public OrderDto createOrder(CreateOrderRequest request) {
        logger.info("Creating order for user: {}", request.getUserId());
        
        // Create order items
        List<OrderItem> orderItems = request.getOrderItems().stream()
                .map(this::createOrderItemFromRequest)
                .collect(Collectors.toList());
        
        // Calculate total amount
        Double totalAmount = orderItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        
        // Create order
        Order order = new Order(request.getUserId(), orderItems, totalAmount);
        
        // Set order reference for each item
        orderItems.forEach(item -> item.setOrder(order));
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        
        logger.info("Order created successfully with ID: {}", savedOrder.getId());
        
        return convertToDto(savedOrder);
    }
    
    public List<OrderDto> getAllOrders() {
        logger.info("Fetching all orders");
        
        List<Order> orders = orderRepository.findAll();
        
        logger.info("Returning {} orders", orders.size());
        
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<OrderDto> getOrdersByUserId(Long userId) {
        logger.info("Fetching orders for user: {}", userId);
        
        List<Order> orders = orderRepository.findByUserId(userId);
        
        logger.info("Returning {} orders for user: {}", orders.size(), userId);
        
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public OrderDto getOrderById(Long id) {
        logger.info("Fetching order with ID: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        
        logger.info("Returning order: {}", order.getId());
        
        return convertToDto(order);
    }
    
    public OrderDto getOrderByIdAndUserId(Long id, Long userId) {
        logger.info("Fetching order with ID: {} for user: {}", id, userId);
        
        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id + " for user: " + userId));
        
        logger.info("Returning order: {} for user: {}", order.getId(), userId);
        
        return convertToDto(order);
    }
    
    @Transactional
    public OrderDto updateOrderStatus(Long id, OrderStatus status) {
        logger.info("Updating status for order ID: {} to: {}", id, status);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        logger.info("Order status updated successfully for ID: {}", id);
        
        return convertToDto(updatedOrder);
    }
    
    private OrderItem createOrderItemFromRequest(CreateOrderItemRequest request) {
        return new OrderItem(request.getProductId(), request.getQuantity(), request.getPrice());
    }
    
    private OrderDto convertToDto(Order order) {
        List<OrderItemDto> orderItemDtos = order.getOrderItems().stream()
                .map(this::convertOrderItemToDto)
                .collect(Collectors.toList());
        
        return new OrderDto(
                order.getId(),
                order.getUserId(),
                order.getOrderDate(),
                order.getStatus().name(),
                orderItemDtos,
                order.getTotalAmount()
        );
    }
    
    private OrderItemDto convertOrderItemToDto(OrderItem orderItem) {
        return new OrderItemDto(
                orderItem.getId(),
                orderItem.getProductId(),
                orderItem.getQuantity(),
                orderItem.getPrice()
        );
    }
}
