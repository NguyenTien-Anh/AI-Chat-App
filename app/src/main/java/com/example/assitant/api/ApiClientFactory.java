package com.example.assitant.api;

import com.example.assitant.utils.Constants;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClientFactory {
    private static final String OPENAI_BASE_URL = "https://api.openai.com/v1/";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/";
    
    private static Retrofit openAiRetrofit = null;
    private static Retrofit llamaRetrofit = null;
    private static Retrofit qwenRetrofit = null;
    private static Retrofit geminiRetrofit = null;
    
    public static Retrofit getOpenAiClient() {
        if (openAiRetrofit == null) {
            openAiRetrofit = createRetrofit(OPENAI_BASE_URL);
        }
        return openAiRetrofit;
    }
    
    public static Retrofit getLlamaClient() {
        if (llamaRetrofit == null) {
            llamaRetrofit = createRetrofit(Constants.LLAMA_API_BASE_URL);
        }
        return llamaRetrofit;
    }
    
    public static Retrofit getQwenClient() {
        if (qwenRetrofit == null) {
            qwenRetrofit = createRetrofit(Constants.QWEN_API_BASE_URL);
        }
        return qwenRetrofit;
    }
    
    public static Retrofit getGeminiClient() {
        if (geminiRetrofit == null) {
            geminiRetrofit = createRetrofit(GEMINI_BASE_URL);
        }
        return geminiRetrofit;
    }
    
    private static Retrofit createRetrofit(String baseUrl) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
} 