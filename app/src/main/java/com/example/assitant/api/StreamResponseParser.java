package com.example.assitant.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.function.Consumer;

public class StreamResponseParser {
    private final Gson gson = new Gson();
    
    public void parseStreamResponse(String chunk, Consumer<String> contentConsumer) {
        if (chunk.isEmpty() || chunk.equals("[DONE]")) {
            return;
        }
        
        try {
            // Remove the "data: " prefix if present
            if (chunk.startsWith("data: ")) {
                chunk = chunk.substring(6);
            }
            
            JsonObject jsonObject = JsonParser.parseString(chunk).getAsJsonObject();
            
            if (jsonObject.has("choices") && 
                    !jsonObject.get("choices").getAsJsonArray().isEmpty()) {
                
                JsonObject choice = jsonObject.get("choices").getAsJsonArray().get(0).getAsJsonObject();
                
                if (choice.has("delta") && 
                        choice.get("delta").getAsJsonObject().has("content")) {
                    
                    String content = choice.get("delta").getAsJsonObject().get("content").getAsString();
                    contentConsumer.accept(content);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 