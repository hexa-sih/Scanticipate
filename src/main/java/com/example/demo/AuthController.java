package com.example.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists!");
        }
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String role = request.get("role");

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()
                && userOpt.get().getPassword().equals(password)
                && userOpt.get().getRole().equalsIgnoreCase(role)) {

            User user = userOpt.get();

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("role", user.getRole());
            response.put("message", "Login successful!");

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(401).body("Invalid username, password, or role");
    }
}
