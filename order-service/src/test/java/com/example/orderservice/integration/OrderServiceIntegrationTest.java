package com.example.orderservice.integration;

import com.example.orderservice.dto.CreateOrderItemRequest;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderItem;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.repository.OrderItemRepository;
import com.example.orderservice.repository.OrderRepository;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebMvc
class OrderServiceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        orderRepository.deleteAll();
        orderItemRepository.deleteAll();
    }

    @Test
    void shouldCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                List.of(
                        new CreateOrderItemRequest(1L, 2, 99.99),
                        new CreateOrderItemRequest(2L, 1, 49.99)
                )
        );

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(249.97))
                .andExpect(jsonPath("$.orderItems.length()").value(2));
    }

    @Test
    void shouldGetAllOrders() throws Exception {
        // Create test orders
        Order order1 = createTestOrder(1L);
        Order order2 = createTestOrder(2L);
        orderRepository.saveAll(List.of(order1, order2));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[1].userId").value(2));
    }

    @Test
    void shouldGetOrderById() throws Exception {
        Order order = createTestOrder(1L);
        Order savedOrder = orderRepository.save(order);

        mockMvc.perform(get("/orders/" + savedOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedOrder.getId()))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn404WhenOrderNotFound() throws Exception {
        mockMvc.perform(get("/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetOrdersByUserId() throws Exception {
        // Create test orders for user 1
        Order order1 = createTestOrder(1L);
        Order order2 = createTestOrder(1L);
        Order order3 = createTestOrder(2L);
        orderRepository.saveAll(List.of(order1, order2, order3));

        mockMvc.perform(get("/orders/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[1].userId").value(1));
    }

    @Test
    void shouldUpdateOrderStatus() throws Exception {
        Order order = createTestOrder(1L);
        Order savedOrder = orderRepository.save(order);

        mockMvc.perform(put("/orders/" + savedOrder.getId() + "/status")
                .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldPersistOrderInDatabase() {
        // Given
        Order order = createTestOrder(1L);
        
        // When
        Order savedOrder = orderRepository.save(order);
        
        // Then
        assertTrue(orderRepository.existsById(savedOrder.getId()));
        
        Order retrievedOrder = orderRepository.findById(savedOrder.getId()).orElse(null);
        assertNotNull(retrievedOrder);
        assertEquals(1L, retrievedOrder.getUserId());
        assertEquals(OrderStatus.PENDING, retrievedOrder.getStatus());
        assertEquals(199.98, retrievedOrder.getTotalAmount());
        assertEquals(2, retrievedOrder.getOrderItems().size());
    }

    @Test
    void shouldPersistOrderItemsInDatabase() {
        // Given
        Order order = createTestOrder(1L);
        Order savedOrder = orderRepository.save(order);
        
        // When
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(savedOrder.getId());
        
        // Then
        assertEquals(2, orderItems.size());
        assertEquals(1L, orderItems.get(0).getProductId());
        assertEquals(2, orderItems.get(0).getQuantity());
        assertEquals(99.99, orderItems.get(0).getPrice());
    }

    private Order createTestOrder(Long userId) {
        OrderItem item1 = new OrderItem(1L, 2, 99.99);
        OrderItem item2 = new OrderItem(2L, 1, 0.0); // Will be set in constructor
        
        Order order = new Order(userId, List.of(item1, item2), 199.98);
        item1.setOrder(order);
        item2.setOrder(order);
        item2.setPrice(0.0); // Set correct price
        
        return order;
    }
}
