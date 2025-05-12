package com.example.assitant.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpenAIRequest {
    @SerializedName("model")
    private String model;
    
    @SerializedName("messages")
    private List<OpenAIMessage> messages;
    
    @SerializedName("temperature")
    private float temperature;
    
    @SerializedName("max_tokens")
    private int maxTokens;
    
    @SerializedName("stream")
    private boolean stream;
    
    public OpenAIRequest(String model, List<OpenAIMessage> messages, float temperature, int maxTokens, boolean stream) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stream = stream;
    }
} 