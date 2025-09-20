package com.example.bridge.models;

public class ChatModel {
    public static final int SENT_BY_ME = 1;
    public static final int SENT_BY_OTHER = 2;
    String message;
    int sendBy;

    public ChatModel(String message, int sendBy) {
        this.message = message;
        this.sendBy = sendBy;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getSendBy() {
        return sendBy;
    }

    public void setSendBy(int sendBy) {
        this.sendBy = sendBy;
    }
}
