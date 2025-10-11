package com.example.BridgeServer.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")

public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Field("full_name")
    private String fullName;

    private String dp;

    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Indexed(unique = true)
    private String email;

    private String password;

}
