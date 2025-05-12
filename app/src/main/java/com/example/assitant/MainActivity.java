package com.example.assitant;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assitant.api.ApiClientFactory;
import com.example.assitant.api.CompletionRequest;
import com.example.assitant.api.CompletionResponse;
import com.example.assitant.api.CompletionService;
import com.example.assitant.api.GeminiRequest;
import com.example.assitant.api.GeminiResponse;
import com.example.assitant.api.GeminiService;
import com.example.assitant.api.OpenAIMessage;
import com.example.assitant.api.OpenAIRequest;
import com.example.assitant.api.OpenAIService;
import com.example.assitant.api.StreamResponseParser;
import com.example.assitant.utils.Constants;
import com.example.assitant.utils.KeyboardUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "MainActivity";

    private TextView welcomeMessage;
    private TextView modelIndicator;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton settingsButton;
    private ImageButton soundWaveButton;
    private ImageButton newChatButton;
    private LinearLayout chatInputArea;
    private Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech textToSpeech;
    private boolean isSpeaking = false;
    
    private RecyclerView messageRecyclerView;
    private MessageAdapter messageAdapter;
    private LinearLayout emptyStateView;
    
    private OpenAIService openAIService;
    private CompletionService llamaService;
    private CompletionService qwenService;
    private GeminiService geminiService;
    private List<OpenAIMessage> conversationHistory = new ArrayList<>();
    private StreamResponseParser streamParser = new StreamResponseParser();
    private StringBuilder currentStreamingResponse = new StringBuilder();

    private final String[] welcomeMessages = {
            "How can I help you today?",
            "How can I assist you this evening?",
            "What would you like to know?",
            "What's on your mind?",
            "Ask me anything!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Initialize services
        openAIService = ApiClientFactory.getOpenAiClient().create(OpenAIService.class);
        llamaService = ApiClientFactory.getLlamaClient().create(CompletionService.class);
        qwenService = ApiClientFactory.getQwenClient().create(CompletionService.class);
        geminiService = ApiClientFactory.getGeminiClient().create(GeminiService.class);
        
        // Initialize views
        welcomeMessage = findViewById(R.id.welcomeMessage);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        settingsButton = findViewById(R.id.settingsButton);
        soundWaveButton = findViewById(R.id.soundWaveButton);
        newChatButton = findViewById(R.id.newChatButton);
        emptyStateView = findViewById(R.id.emptyStateView);
        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        modelIndicator = findViewById(R.id.modelIndicator);
        chatInputArea = findViewById(R.id.chatInputArea);
        
        // Update model indicator with currently selected model
        updateModelIndicator();
        
        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);
        
        // Setup status bar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Setup RecyclerView
        messageAdapter = new MessageAdapter();
        messageRecyclerView.setAdapter(messageAdapter);
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Setup keyboard visibility listener
        setupKeyboardListener();
        
        // Setup click listeners
        sendButton.setOnClickListener(v -> sendMessage());
        settingsButton.setOnClickListener(v -> showModelSelectionMenu(v));
        soundWaveButton.setOnClickListener(v -> handleSoundWaveClick());
        newChatButton.setOnClickListener(v -> startNewChat());
        
        // Setup click listener for input field to ensure it's visible
        messageInput.setOnClickListener(v -> {
            handler.postDelayed(() -> {
                if (messageAdapter.getItemCount() > 0) {
                    messageRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                }
            }, 200);
        });
        
        // Start text streaming effect
        startTextStreaming();
    }
    
    private void setupKeyboardListener() {
        // Set up keyboard visibility listener
        KeyboardUtils.setUpKeyboardVisibilityListener(this, findViewById(R.id.main), chatInputArea);
        
        // Set focus listener for input field to ensure it's visible when selected
        messageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                handler.postDelayed(() -> {
                    if (messageAdapter.getItemCount() > 0) {
                        messageRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                }, 300);
            }
        });
    }
    
    private void startTextStreaming() {
        // Choose a random welcome message
        int randomIndex = (int) (Math.random() * welcomeMessages.length);
        String message = welcomeMessages[randomIndex];
        
        // Set empty text initially
        welcomeMessage.setText("");
        
        // Start streaming effect
        final int[] charIndex = {0};
        final Handler handler = new Handler();
        
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    if (charIndex[0] < message.length()) {
                        welcomeMessage.setText(message.substring(0, ++charIndex[0]));
                    } else {
                        timer.cancel();
                    }
                });
            }
        }, 0, 100); // Display a new character every 100ms
    }
    
    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty()) {
            // Show message list if this is the first message
            if (messageAdapter.getItemCount() == 0) {
                showMessageList();
            }
            
            // Add user message to the list
            messageAdapter.addMessage(new MessageItem(message, MessageItem.TYPE_USER));
            
            // Add message to conversation history
            conversationHistory.add(new OpenAIMessage("user", message));
            
            // Clear the input field
            messageInput.setText("");
            
            // Scroll to the bottom
            messageRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
            
            // Generate AI response using appropriate API
            sendMessageToAI(message);
        }
    }
    
    private void sendMessageToAI(String userMessage) {
        // Create an initial empty AI message
        MessageItem aiMessage = new MessageItem("", MessageItem.TYPE_AI);
        aiMessage.setStreaming(true);
        messageAdapter.addMessage(aiMessage);
        
        // Scroll to the bottom
        messageRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
        
        // Reset streaming response
        currentStreamingResponse = new StringBuilder();
        
        // Get the selected model
        String selectedModel = Constants.getSelectedModel(this);
        
        // Dispatch to the appropriate API based on the selected model
        if (selectedModel.equals(Constants.MODEL_GEMINI_2_0)) {
            sendMessageToGemini(userMessage);
        } else if (selectedModel.equals(Constants.MODEL_LLAMA_3_2)) {
            sendMessageToLlama(userMessage);
        } else if (selectedModel.equals(Constants.MODEL_QWEN_2_5)) {
            sendMessageToQwen(userMessage);
        } else {
            // Default to OpenAI
            sendMessageToOpenAI();
        }
    }
    
    private void sendMessageToOpenAI() {
        // Create and send the OpenAI API request with streaming enabled
        OpenAIRequest request = new OpenAIRequest(
                Constants.getSelectedModel(this),  // Use the selected model
                conversationHistory,
                Constants.DEFAULT_TEMPERATURE,
                Constants.DEFAULT_MAX_TOKENS,
                true // Enable streaming
        );
        
        Call<ResponseBody> call = openAIService.createStreamingChatCompletion(
                Constants.getAuthHeader(this),
                request
        );
        
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Process streaming response
                    try {
                        processStreamingResponse(response.body());
                    } catch (IOException e) {
                        handleApiError("Error processing stream: " + e.getMessage());
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        handleApiError("API Error: " + response.code() + " " + response.message() + "\n" + errorBody);
                    } catch (IOException e) {
                        handleApiError("API Error: " + response.code() + " " + response.message());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                handleApiError("API Call Failed: " + t.getMessage());
            }
        });
    }
    
    private void sendMessageToLlama(String userMessage) {
        // Setup Llama API request
        CompletionRequest request = new CompletionRequest(
                "meta-llama/Llama-3.2-1B-Instruct",  // Model identifier for Llama
                userMessage,
                Constants.DEFAULT_MAX_TOKENS,
                Constants.DEFAULT_TEMPERATURE
        );
        
        Call<CompletionResponse> call = llamaService.createCompletion(request);
        
        call.enqueue(new Callback<CompletionResponse>() {
            @Override
            public void onResponse(Call<CompletionResponse> call, Response<CompletionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseText = response.body().getFirstChoiceText();
                    if (responseText != null) {
                        updateAIResponse(responseText);
                    } else {
                        handleApiError("No text in Llama response");
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        handleApiError("Llama API Error: " + response.code() + " " + response.message() + "\n" + errorBody);
                    } catch (IOException e) {
                        handleApiError("Llama API Error: " + response.code() + " " + response.message());
                    }
                }
            }

            @Override
            public void onFailure(Call<CompletionResponse> call, Throwable t) {
                handleApiError("Llama API Call Failed: " + t.getMessage());
            }
        });
    }
    
    private void sendMessageToQwen(String userMessage) {
        // Setup Qwen API request
        CompletionRequest request = new CompletionRequest(
                "Qwen/Qwen2.5-1.5B-Instruct",  // Model identifier for Qwen
                userMessage,
                Constants.DEFAULT_MAX_TOKENS,
                Constants.DEFAULT_TEMPERATURE
        );
        
        Call<CompletionResponse> call = qwenService.createCompletion(request);
        
        call.enqueue(new Callback<CompletionResponse>() {
            @Override
            public void onResponse(Call<CompletionResponse> call, Response<CompletionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseText = response.body().getFirstChoiceText();
                    if (responseText != null) {
                        updateAIResponse(responseText);
                    } else {
                        handleApiError("No text in Qwen response");
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        handleApiError("Qwen API Error: " + response.code() + " " + response.message() + "\n" + errorBody);
                    } catch (IOException e) {
                        handleApiError("Qwen API Error: " + response.code() + " " + response.message());
                    }
                }
            }

            @Override
            public void onFailure(Call<CompletionResponse> call, Throwable t) {
                handleApiError("Qwen API Call Failed: " + t.getMessage());
            }
        });
    }
    
    private void sendMessageToGemini(String userMessage) {
        // Setup Gemini API request
        GeminiRequest request = new GeminiRequest(userMessage);
        
        Call<GeminiResponse> call = geminiService.generateContent(
                Constants.getGeminiApiKey(this),
                request
        );
        
        call.enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseText = response.body().getFirstCandidateText();
                    if (responseText != null) {
                        updateAIResponse(responseText);
                    } else {
                        handleApiError("No text in Gemini response");
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        handleApiError("Gemini API Error: " + response.code() + " " + response.message() + "\n" + errorBody);
                    } catch (IOException e) {
                        handleApiError("Gemini API Error: " + response.code() + " " + response.message());
                    }
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                handleApiError("Gemini API Call Failed: " + t.getMessage());
            }
        });
    }
    
    private void updateAIResponse(String response) {
        // Update the UI with the response
        messageAdapter.updateLastMessage(response);
        
        // Add to conversation history
        OpenAIMessage assistantMessage = new OpenAIMessage("assistant", response);
        conversationHistory.add(assistantMessage);
        
        // Scroll to the bottom
        messageRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }
    
    private void processStreamingResponse(ResponseBody responseBody) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
        String line;
        
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) continue;
            
            // Extract content from the stream
            streamParser.parseStreamResponse(line, content -> {
                // Append to the current response
                currentStreamingResponse.append(content);
                
                // Update UI on the main thread
                runOnUiThread(() -> {
                    // Update the last AI message with the current content
                    messageAdapter.updateLastMessage(currentStreamingResponse.toString());
                    
                    // Keep scrolling to the bottom as the message grows
                    messageRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                });
            });
        }
        
        // When streaming is complete, add the message to the conversation history
        if (currentStreamingResponse.length() > 0) {
            OpenAIMessage assistantMessage = new OpenAIMessage("assistant", currentStreamingResponse.toString());
            conversationHistory.add(assistantMessage);
        }
    }
    
    private void handleApiError(String errorMessage) {
        Log.e(TAG, errorMessage);
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            
            // Update the AI message to show the error
            String errorResponse = "Sorry, I encountered an error. Please try again.";
            messageAdapter.updateLastMessage(errorResponse);
            
            // Add error response to conversation history
            OpenAIMessage assistantMessage = new OpenAIMessage("assistant", errorResponse);
            conversationHistory.add(assistantMessage);
        });
    }
    
    private void showMessageList() {
        emptyStateView.setVisibility(View.GONE);
        messageRecyclerView.setVisibility(View.VISIBLE);
    }
    
    private void startVoiceInput() {
        if (isSpeaking) {
            if (textToSpeech != null) {
                textToSpeech.stop();
            }
            isSpeaking = false;
        } else {
            // In a real app, this would trigger voice recognition
            Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show();
            
            // For demo, we'll just speak the current welcome message or last AI message
            String textToSpeak;
            if (messageAdapter.getItemCount() > 0) {
                // Find the last AI message
                int lastAiMessageIndex = -1;
                for (int i = messageAdapter.getItemCount() - 1; i >= 0; i--) {
                    if (i < messageAdapter.getItemCount() && 
                        messageAdapter.getItemViewType(i) == MessageItem.TYPE_AI) {
                        lastAiMessageIndex = i;
                        break;
                    }
                }
                
                if (lastAiMessageIndex != -1) {
                    // Speak the last AI message
                    MessageItem lastAiMessage = messageAdapter.getItem(lastAiMessageIndex);
                    textToSpeak = lastAiMessage.getContent();
                } else {
                    textToSpeak = welcomeMessage.getText().toString();
                }
            } else {
                textToSpeak = welcomeMessage.getText().toString();
            }
            
            if (!textToSpeak.isEmpty()) {
                speakText(textToSpeak);
            }
        }
    }
    
    private void speakText(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            isSpeaking = true;
        }
    }
    
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language
            int result = textToSpeech.setLanguage(Locale.US);
            
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    
    private void handleSoundWaveClick() {
        // Mở màn hình Live Audio khi nhấn vào nút
        Intent intent = new Intent(this, LiveAudioActivity.class);
        startActivity(intent);
    }
    
    private void showModelSelectionMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.model_selection_menu, popup.getMenu());
        
        // Get the currently selected model
        String currentModel = Constants.getSelectedModel(this);
        
        // Add checkmark to the currently selected model
        MenuItem gpt4oItem = popup.getMenu().findItem(R.id.model_gpt4o);
        MenuItem geminiItem = popup.getMenu().findItem(R.id.model_gemini);
        MenuItem llamaItem = popup.getMenu().findItem(R.id.model_llama);
        MenuItem qwenItem = popup.getMenu().findItem(R.id.model_qwen);
        
        gpt4oItem.setChecked(currentModel.equals(Constants.MODEL_GPT_4O_MINI));
        geminiItem.setChecked(currentModel.equals(Constants.MODEL_GEMINI_2_0));
        llamaItem.setChecked(currentModel.equals(Constants.MODEL_LLAMA_3_2));
        qwenItem.setChecked(currentModel.equals(Constants.MODEL_QWEN_2_5));
        
        // Enable icons in popup menu using reflection
        try {
            Class<?> popupMenuClass = Class.forName("android.widget.PopupMenu");
            java.lang.reflect.Field field = popupMenuClass.getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuPopupHelper = field.get(popup);
            Class<?> menuPopupHelperClass = Class.forName(menuPopupHelper.getClass().getName());
            java.lang.reflect.Method setForceShowIcon = menuPopupHelperClass.getDeclaredMethod("setForceShowIcon", boolean.class);
            setForceShowIcon.setAccessible(true);
            setForceShowIcon.invoke(menuPopupHelper, true);
        } catch (Exception e) {
            Log.e(TAG, "Error showing menu icons", e);
        }
        
        popup.setOnMenuItemClickListener(item -> {
            String selectedModel;
            int itemId = item.getItemId();
            
            if (itemId == R.id.model_gpt4o) {
                selectedModel = Constants.MODEL_GPT_4O_MINI;
            } else if (itemId == R.id.model_gemini) {
                selectedModel = Constants.MODEL_GEMINI_2_0;
            } else if (itemId == R.id.model_llama) {
                selectedModel = Constants.MODEL_LLAMA_3_2;
            } else if (itemId == R.id.model_qwen) {
                selectedModel = Constants.MODEL_QWEN_2_5;
            } else {
                return false;
            }
            
            // Save the selected model
            Constants.saveSelectedModel(this, selectedModel);
            
            // Update the model indicator
            updateModelIndicator();
            
            Toast.makeText(this, "Model changed to " + selectedModel, Toast.LENGTH_SHORT).show();
            return true;
        });
        
        popup.show();
    }
    
    private void updateModelIndicator() {
        String currentModel = Constants.getSelectedModel(this);
        modelIndicator.setText(currentModel);
        modelIndicator.setVisibility(View.VISIBLE);
    }
    
    private void startNewChat() {
        // Clear the conversation history
        conversationHistory.clear();
        
        // Clear the message adapter
        messageAdapter.clearMessages();
        
        // Show the empty state view
        emptyStateView.setVisibility(View.VISIBLE);
        messageRecyclerView.setVisibility(View.GONE);
        
        // Start a new welcome message animation
        startTextStreaming();
        
        // Show a toast to confirm
        Toast.makeText(this, "Started a new chat", Toast.LENGTH_SHORT).show();
    }
}