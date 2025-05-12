package com.example.assitant.api;

import com.google.gson.annotations.SerializedName;

public class OpenAIMessage {
    @SerializedName("role")
    private String role;
    
    @SerializedName("content")
    private String content;
    
    public OpenAIMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    public String getRole() {
        return role;
    }
    
    public String getContent() {
        return content;
    }
} 