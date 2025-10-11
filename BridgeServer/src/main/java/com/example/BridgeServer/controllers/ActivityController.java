package com.example.BridgeServer.controllers;

import com.example.BridgeServer.models.Activity;
import com.example.BridgeServer.repos.ActivityRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("users/{userId}/activity")
public class ActivityController {
    @Autowired
    private ActivityRepo activityRepo;



    @GetMapping("/{id}")
    public ResponseEntity<Activity> getActivityById(@PathVariable String userId, @PathVariable String id) {
        return activityRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Activity createActivity(@PathVariable String userId, @RequestBody Activity activity) {
        activity.setUserId(userId);
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUserId(userId);
        return activityRepo.save(activity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Activity> updateActivity(@PathVariable String userId, @PathVariable String id,
                                                   @RequestBody Activity activityDetails) {
        return activityRepo.findById(id)
                .map(activity -> {
                    activity.setType(activityDetails.getType());
                    activity.setDescription(activityDetails.getDescription());
                    activity.setCreatedAt(activityDetails.getCreatedAt());
                    return ResponseEntity.ok(activityRepo.save(activity));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable String userId, @PathVariable String id) {
        return activityRepo.findById(id)
                .map(activity -> {
                    activityRepo.delete(activity);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }


}
