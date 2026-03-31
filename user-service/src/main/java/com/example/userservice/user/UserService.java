package com.example.userservice.user;

import com.example.userservice.user.dto.CreateUserRequest;
import com.example.userservice.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        userRepository.findByEmail(request.email())
                .ifPresent(existing -> {
                    throw new UserAlreadyExistsException(request.email());
                });

        User user = userRepository.save(new User(request.name(), request.email()));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
