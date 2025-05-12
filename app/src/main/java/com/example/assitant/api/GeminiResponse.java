package com.example.assitant.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeminiResponse {
    @SerializedName("candidates")
    private List<Candidate> candidates;
    
    @SerializedName("usageMetadata")
    private UsageMetadata usageMetadata;
    
    @SerializedName("modelVersion")
    private String modelVersion;
    
    public List<Candidate> getCandidates() {
        return candidates;
    }
    
    public UsageMetadata getUsageMetadata() {
        return usageMetadata;
    }
    
    public String getModelVersion() {
        return modelVersion;
    }
    
    public String getFirstCandidateText() {
        if (candidates != null && !candidates.isEmpty() && 
            candidates.get(0).getContent() != null && 
            candidates.get(0).getContent().getParts() != null && 
            !candidates.get(0).getContent().getParts().isEmpty()) {
            return candidates.get(0).getContent().getParts().get(0).getText();
        }
        return null;
    }
    
    public static class Candidate {
        @SerializedName("content")
        private Content content;
        
        @SerializedName("finishReason")
        private String finishReason;
        
        @SerializedName("avgLogprobs")
        private double avgLogprobs;
        
        public Content getContent() {
            return content;
        }
        
        public String getFinishReason() {
            return finishReason;
        }
        
        public double getAvgLogprobs() {
            return avgLogprobs;
        }
    }
    
    public static class Content {
        @SerializedName("parts")
        private List<Part> parts;
        
        @SerializedName("role")
        private String role;
        
        public List<Part> getParts() {
            return parts;
        }
        
        public String getRole() {
            return role;
        }
    }
    
    public static class Part {
        @SerializedName("text")
        private String text;
        
        public String getText() {
            return text;
        }
    }
    
    public static class UsageMetadata {
        @SerializedName("promptTokenCount")
        private int promptTokenCount;
        
        @SerializedName("candidatesTokenCount")
        private int candidatesTokenCount;
        
        @SerializedName("totalTokenCount")
        private int totalTokenCount;
        
        public int getPromptTokenCount() {
            return promptTokenCount;
        }
        
        public int getCandidatesTokenCount() {
            return candidatesTokenCount;
        }
        
        public int getTotalTokenCount() {
            return totalTokenCount;
        }
    }
}