package com.example.BridgeServer.repos;

import com.example.BridgeServer.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepo extends MongoRepository<User,String> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}
