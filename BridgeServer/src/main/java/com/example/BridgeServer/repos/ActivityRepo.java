package com.example.BridgeServer.repos;

import com.example.BridgeServer.models.Activity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityRepo extends MongoRepository<Activity,String> {
    Optional<Activity>findByUserId(String userId);
}
