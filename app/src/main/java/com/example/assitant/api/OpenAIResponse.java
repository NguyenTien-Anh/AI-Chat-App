package com.example.assitant.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpenAIResponse {
    @SerializedName("id")
    private String id;
    
    @SerializedName("object")
    private String object;
    
    @SerializedName("created")
    private long created;
    
    @SerializedName("choices")
    private List<Choice> choices;
    
    public String getId() {
        return id;
    }
    
    public String getObject() {
        return object;
    }
    
    public long getCreated() {
        return created;
    }
    
    public List<Choice> getChoices() {
        return choices;
    }
    
    public String getFirstMessageContent() {
        if (choices != null && !choices.isEmpty() && choices.get(0).getMessage() != null) {
            return choices.get(0).getMessage().getContent();
        }
        return null;
    }
    
    public static class Choice {
        @SerializedName("index")
        private int index;
        
        @SerializedName("message")
        private OpenAIMessage message;
        
        @SerializedName("finish_reason")
        private String finishReason;
        
        public int getIndex() {
            return index;
        }
        
        public OpenAIMessage getMessage() {
            return message;
        }
        
        public String getFinishReason() {
            return finishReason;
        }
    }
} 