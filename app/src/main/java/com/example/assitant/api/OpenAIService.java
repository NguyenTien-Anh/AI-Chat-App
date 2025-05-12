package com.example.assitant.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface OpenAIService {
    @Headers({
            "Content-Type: application/json"
    })
    @POST("chat/completions")
    Call<OpenAIResponse> createChatCompletion(
            @Header("Authorization") String authorization,
            @Body OpenAIRequest request
    );
    
    @Headers({
            "Content-Type: application/json"
    })
    @POST("chat/completions")
    Call<ResponseBody> createStreamingChatCompletion(
            @Header("Authorization") String authorization,
            @Body OpenAIRequest request
    );
} 