package com.example.userservice.user;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email) {
        super("User with email " + email + " already exists");
    }
}
