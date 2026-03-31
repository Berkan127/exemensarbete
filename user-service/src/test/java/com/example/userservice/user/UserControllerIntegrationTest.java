package com.example.userservice.user;

import com.example.userservice.user.dto.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateAndGetUser() {
        CreateUserRequest request = new CreateUserRequest("Alice", "alice@example.com");
        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/users", new HttpEntity<>(request), Map.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        Integer id = (Integer) createResponse.getBody().get("id");
        assertThat(id).isNotNull();

        ResponseEntity<Map> getResponse = restTemplate.getForEntity("/users/" + id, Map.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().get("email")).isEqualTo("alice@example.com");
    }

    @Test
    void shouldReturn404WhenUserMissing() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/users/999999", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("message");
    }
}
