package com.example.BridgeServer.controllers;

import com.example.BridgeServer.models.Activity;
import com.example.BridgeServer.models.User;
import com.example.BridgeServer.repos.ActivityRepo;
import com.example.BridgeServer.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

record LoginRequest(String email, String password) {
}

record RegisterRequest(String username, String password, String email, String fullName) {
}

record AuthResponse(String message, User user) {
}


@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ActivityRepo activityRepo;
    

    @GetMapping
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return userRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userRepo.save(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User userDetails) {
        return userRepo.findById(id)
                .map(user -> {
                    user.setUsername(userDetails.getUsername());
                    user.setFullName(userDetails.getFullName());
                    user.setEmail(userDetails.getEmail());
                    user.setPassword(userDetails.getPassword());
                    user.setDp(userDetails.getDp());
                    return ResponseEntity.ok(userRepo.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        return userRepo.findById(id)
                .map(user -> {
                    userRepo.delete(user);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userRepo.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return userRepo.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/activities")
    public Activity getUserActivities(@PathVariable String id) {
        return activityRepo.findByUserId(id)
                .orElseThrow(() -> new RuntimeException("Activity not found for user"));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (userRepo.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest().body(new AuthResponse("Username already exists", null));
        }
        if (userRepo.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.badRequest().body(new AuthResponse("Email already exists", null));
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setEmail(request.email());
        user.setFullName(request.fullName());

        User savedUser = userRepo.save(user);
        return ResponseEntity.ok(new AuthResponse("User registered successfully", savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return userRepo.findByEmail(request.email())
                .map(user -> {
                    if (user.getPassword().equals(request.password())) {
                        return ResponseEntity.ok(new AuthResponse("Login successful", user));
                    } else {
                        return ResponseEntity.status(401)
                                .body(new AuthResponse("Invalid password", null));
                    }
                })
                .orElse(ResponseEntity.status(404)
                        .body(new AuthResponse("User not found", null)));
    }
    
    
    
    


}
