package com.example.BridgeServer.models;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

@Document(collection = "activities")
public class Activity {
    @Id
    private String id;

    @Field("created_at")
    private LocalDateTime createdAt;

    private String type;

    private String title;

    private String userId;

    private String description;




}
