package com.example.microservices.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DatabasePersistenceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Test
    void shouldConnectToProductDatabase() throws Exception {
        // Test connection to product database
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            
            Statement stmt = conn.createStatement();
            
            // Create test table
            stmt.execute("CREATE TABLE IF NOT EXISTS test_products (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "price DECIMAL(10,2) NOT NULL)");
            
            // Insert test data
            stmt.execute("INSERT INTO test_products (name, price) VALUES ('Test Product', 99.99)");
            
            // Verify data persistence
            ResultSet rs = stmt.executeQuery("SELECT name, price FROM test_products WHERE name = 'Test Product'");
            assertTrue(rs.next());
            assertEquals("Test Product", rs.getString("name"));
            assertEquals(99.99, rs.getDouble("price"));
            
            // Clean up
            stmt.execute("DROP TABLE test_products");
        }
    }

    @Test
    void shouldConnectToOrderDatabase() throws Exception {
        // Test connection to order database
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            
            Statement stmt = conn.createStatement();
            
            // Create test tables
            stmt.execute("CREATE TABLE IF NOT EXISTS test_orders (" +
                    "id SERIAL PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "total_amount DECIMAL(10,2) NOT NULL, " +
                    "status VARCHAR(50) NOT NULL)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS test_order_items (" +
                    "id SERIAL PRIMARY KEY, " +
                    "order_id BIGINT NOT NULL, " +
                    "product_id BIGINT NOT NULL, " +
                    "quantity INTEGER NOT NULL, " +
                    "price DECIMAL(10,2) NOT NULL)");
            
            // Insert test data
            stmt.execute("INSERT INTO test_orders (user_id, total_amount, status) VALUES (1, 199.99, 'PENDING')");
            ResultSet rs = stmt.executeQuery("SELECT id FROM test_orders WHERE user_id = 1");
            assertTrue(rs.next());
            Long orderId = rs.getLong("id");
            
            stmt.execute("INSERT INTO test_order_items (order_id, product_id, quantity, price) VALUES (" + orderId + ", 1, 2, 99.99)");
            
            // Verify data persistence with foreign key relationship
            ResultSet itemRs = stmt.executeQuery("SELECT oi.quantity, oi.price FROM test_order_items oi " +
                    "JOIN test_orders o ON oi.order_id = o.id WHERE o.user_id = 1");
            assertTrue(itemRs.next());
            assertEquals(2, itemRs.getInt("quantity"));
            assertEquals(99.99, itemRs.getDouble("price"));
            
            // Clean up
            stmt.execute("DROP TABLE test_order_items");
            stmt.execute("DROP TABLE test_orders");
        }
    }

    @Test
    void shouldConnectToUserDatabase() throws Exception {
        // Test connection to user database
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            
            Statement stmt = conn.createStatement();
            
            // Create test table
            stmt.execute("CREATE TABLE IF NOT EXISTS test_users (" +
                    "id SERIAL PRIMARY KEY, " +
                    "username VARCHAR(255) UNIQUE NOT NULL, " +
                    "email VARCHAR(255) UNIQUE NOT NULL, " +
                    "first_name VARCHAR(255) NOT NULL, " +
                    "last_name VARCHAR(255) NOT NULL)");
            
            // Insert test data
            stmt.execute("INSERT INTO test_users (username, email, first_name, last_name) VALUES " +
                    "('testuser', 'test@example.com', 'Test', 'User')");
            
            // Verify data persistence
            ResultSet rs = stmt.executeQuery("SELECT username, email, first_name, last_name FROM test_users WHERE username = 'testuser'");
            assertTrue(rs.next());
            assertEquals("testuser", rs.getString("username"));
            assertEquals("test@example.com", rs.getString("email"));
            assertEquals("Test", rs.getString("first_name"));
            assertEquals("User", rs.getString("last_name"));
            
            // Test unique constraint
            try {
                stmt.execute("INSERT INTO test_users (username, email, first_name, last_name) VALUES " +
                        "('testuser', 'test2@example.com', 'Test2', 'User2')");
                fail("Should have thrown exception for duplicate username");
            } catch (Exception e) {
                // Expected
            }
            
            // Clean up
            stmt.execute("DROP TABLE test_users");
        }
    }

    @Test
    void shouldHandleTransactions() throws Exception {
        // Test transaction handling
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            
            try {
                // Create test table
                stmt.execute("CREATE TABLE IF NOT EXISTS test_transactions (" +
                        "id SERIAL PRIMARY KEY, " +
                        "data VARCHAR(255) NOT NULL)");
                
                // Insert data
                stmt.execute("INSERT INTO test_transactions (data) VALUES ('test1')");
                stmt.execute("INSERT INTO test_transactions (data) VALUES ('test2')");
                
                // Verify data before commit
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_transactions");
                rs.next();
                assertEquals(2, rs.getInt(1));
                
                // Commit transaction
                conn.commit();
                
                // Verify data after commit
                rs = stmt.executeQuery("SELECT COUNT(*) FROM test_transactions");
                rs.next();
                assertEquals(2, rs.getInt(1));
                
                // Test rollback
                conn.setAutoCommit(false);
                stmt.execute("INSERT INTO test_transactions (data) VALUES ('test3')");
                
                // Rollback
                conn.rollback();
                
                // Verify rollback worked
                rs = stmt.executeQuery("SELECT COUNT(*) FROM test_transactions");
                rs.next();
                assertEquals(2, rs.getInt(1)); // Should still be 2, not 3
                
                // Clean up
                stmt.execute("DROP TABLE test_transactions");
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
