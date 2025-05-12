package com.example.assitant.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Constants {
    // OpenAI models
    public static final String MODEL_GPT_4O_MINI = "gpt-4o-mini";
    public static final String MODEL_GEMINI_2_0 = "gemini-2.0";
    public static final String MODEL_LLAMA_3_2 = "llama-3.2";
    public static final String MODEL_QWEN_2_5 = "qwen-2.5";
    public static final String DEFAULT_MODEL = MODEL_GPT_4O_MINI;
    
    // API Base URLs
    public static final String LLAMA_API_BASE_URL = "https://9077-34-126-159-156.ngrok-free.app";
    public static final String QWEN_API_BASE_URL = "https://53eb-34-125-229-144.ngrok-free.app";
    
    // Default API settings
    public static final float DEFAULT_TEMPERATURE = 0.7f;
    public static final int DEFAULT_MAX_TOKENS = 512;
    
    // Hardcoded API Keys
    private static final String HARDCODED_API_KEY = "<openai_api_key>";
    private static final String HARDCODED_GEMINI_API_KEY = "<gemini_api_key>"; // Replace with your actual Gemini API key
    
    // SharedPreferences settings
    private static final String PREFS_NAME = "AIAssistantPrefs";
    private static final String KEY_API_KEY = "openai_api_key";
    private static final String KEY_MODEL = "openai_model";
    
    public static String getApiKey(Context context) {
        // Always return the hardcoded API key
        return HARDCODED_API_KEY;
    }
    
    public static void saveApiKey(Context context, String apiKey) {
        // Method kept for compatibility, but doesn't do anything
    }
    
    public static String getSelectedModel(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_MODEL, DEFAULT_MODEL);
    }
    
    public static void saveSelectedModel(Context context, String model) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_MODEL, model);
        editor.apply();
    }
    
    public static String getAuthHeader(Context context) {
        return "Bearer " + HARDCODED_API_KEY;
    }
    
    public static String getOpenAIApiKey(Context context) {
        // In a real app, you would retrieve this from secure storage or BuildConfig
        return HARDCODED_API_KEY;
    }
    
    public static boolean isApiKeySet(Context context) {
        // Always return true since we have a hardcoded key
        return true;
    }
    
    public static String getGeminiApiKey(Context context) {
        // In a real app, you would retrieve this from secure storage or BuildConfig
        return HARDCODED_GEMINI_API_KEY;
    }
}