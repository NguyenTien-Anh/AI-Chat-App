package com.example.assitant.api;

import com.google.gson.annotations.SerializedName;

public class CompletionRequest {
    @SerializedName("model")
    private String model;
    
    @SerializedName("prompt")
    private String prompt;
    
    @SerializedName("max_tokens")
    private int maxTokens;
    
    @SerializedName("temperature")
    private float temperature;
    
    public CompletionRequest(String model, String prompt, int maxTokens, float temperature) {
        this.model = model;
        this.prompt = prompt;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }
} 