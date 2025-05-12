package com.example.assitant.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface CompletionService {
    @Headers({
            "Content-Type: application/json"
    })
    @POST("v1/completions")
    Call<CompletionResponse> createCompletion(
            @Body CompletionRequest request
    );
} 