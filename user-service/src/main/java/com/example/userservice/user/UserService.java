package com.example.userservice.user;

import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.dto.UserDto;
import com.example.userservice.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        userRepository.findByUsername(request.getUsername())
                .ifPresent(existing -> {
                    throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
                });

        userRepository.findByEmail(request.getEmail())
                .ifPresent(existing -> {
                    throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
                });

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(
                request.getUsername(),
                encodedPassword,
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                Role.USER
        );
        
        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername() + " " + user.getFirstName() + " " + user.getLastName(),
                user.getEmail()
        );
    }
}
