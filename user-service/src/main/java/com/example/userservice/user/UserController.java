package com.example.userservice.user;

import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.user.dto.UserResponse;
import jakarta.validation.Valid;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody RegisterRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}
