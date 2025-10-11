package com.example.bridge.models;

public class SessionItem {
    private String type;
    private String title;
    private String description;
    private long timestamp;

    public SessionItem(String type, String title, String description, long timestamp) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}