package com.example.assitant.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CompletionResponse {
    @SerializedName("id")
    private String id;
    
    @SerializedName("object")
    private String object;
    
    @SerializedName("created")
    private long created;
    
    @SerializedName("model")
    private String model;
    
    @SerializedName("choices")
    private List<Choice> choices;
    
    @SerializedName("usage")
    private Usage usage;
    
    public String getId() {
        return id;
    }
    
    public String getObject() {
        return object;
    }
    
    public long getCreated() {
        return created;
    }
    
    public String getModel() {
        return model;
    }
    
    public List<Choice> getChoices() {
        return choices;
    }
    
    public Usage getUsage() {
        return usage;
    }
    
    public String getFirstChoiceText() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getText();
        }
        return null;
    }
    
    public static class Choice {
        @SerializedName("index")
        private int index;
        
        @SerializedName("text")
        private String text;
        
        @SerializedName("finish_reason")
        private String finishReason;
        
        @SerializedName("stop_reason")
        private String stopReason;
        
        public int getIndex() {
            return index;
        }
        
        public String getText() {
            return text;
        }
        
        public String getFinishReason() {
            return finishReason;
        }
        
        public String getStopReason() {
            return stopReason;
        }
    }
    
    public static class Usage {
        @SerializedName("prompt_tokens")
        private int promptTokens;
        
        @SerializedName("completion_tokens")
        private int completionTokens;
        
        @SerializedName("total_tokens")
        private int totalTokens;
        
        public int getPromptTokens() {
            return promptTokens;
        }
        
        public int getCompletionTokens() {
            return completionTokens;
        }
        
        public int getTotalTokens() {
            return totalTokens;
        }
    }
} 