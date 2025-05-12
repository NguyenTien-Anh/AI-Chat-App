package com.example.assitant;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.assitant.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LiveAudioActivity extends AppCompatActivity {

    private static final String TAG = "LiveAudioActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String API_URL_TRANSCRIPTION = "https://api.openai.com/v1/audio/transcriptions";
    private static final String API_URL_CHAT = "https://api.openai.com/v1/chat/completions";
    private static final String API_URL_TTS = "https://api.openai.com/v1/audio/speech";
    
    // Gemini API endpoints
    private static final String GEMINI_API_URL_GENERATION = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final String GEMINI_API_URL_UPLOAD = "https://generativelanguage.googleapis.com/upload/v1beta/files";
    
    private FrameLayout micButton;
    private FrameLayout endButton;
    private ImageView micIcon;
    private View waveBg;
    private TextView liveText;
    
    private MediaRecorder recorder;
    private MediaPlayer mediaPlayer;
    private String fileName;
    private boolean isRecording = false;
    private boolean permissionGranted = false;
    private boolean isProcessing = false;
    private Handler handler = new Handler();
    private Runnable pulseAnimation;
    private OkHttpClient client;
    private boolean useGemini = false; // Toggle to use Gemini over OpenAI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set fullscreen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.dialog_live_audio);
        
        // Check and request permissions
        String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            permissionGranted = true;
        }
        
        // Initialize views
        micButton = findViewById(R.id.micButton);
        endButton = findViewById(R.id.endButton);
        micIcon = findViewById(R.id.micIcon);
        waveBg = findViewById(R.id.waveBg);
        liveText = findViewById(R.id.liveText);
        
        // Set initial text
        liveText.setText("Hold to record");
        
        // Initialize wave background
        waveBg.setAlpha(0.8f);
        waveBg.setPivotX(waveBg.getWidth() / 2f);
        waveBg.setPivotY(waveBg.getHeight() / 2f);
        
        // Initialize OkHttpClient with timeout
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        
        // Prepare output file
        prepareOutputFile();
        
        // Set up pulse animation
        setupPulseAnimation();
        
        // Setup touch listener for record button
        micButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRecording();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopRecordingAndProcess();
                    return true;
            }
            return false;
        });
        
        // Set click listener for end button
        endButton.setOnClickListener(v -> handleEndClick());
    }
    
    private void setupPulseAnimation() {
        pulseAnimation = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    // Create pulse effect for wave background
                    waveBg.animate()
                        .scaleX(1.05f)
                        .scaleY(1.15f)
                        .alpha(0.9f)
                        .setDuration(400)
                        .withEndAction(() -> {
                            waveBg.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .alpha(0.7f)
                                .setDuration(400)
                                .start();
                        })
                        .start();
                    
                    // Schedule next pulse
                    handler.postDelayed(this, 800);
                }
            }
        };
    }
    
    private void prepareOutputFile() {
        // Create timestamp for unique file name
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = getExternalFilesDir(null);
        fileName = storageDir.getAbsolutePath() + "/AUDIO_" + timestamp + ".mp3";
    }
    
    private void startRecording() {
        if (!permissionGranted) {
            Toast.makeText(this, "Recording permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Start recording only if not already recording and not processing
        if (!isRecording && !isProcessing) {
            try {
                // Set up media recorder
                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setOutputFile(fileName);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioEncodingBitRate(128000); // 128kbps
                recorder.setAudioSamplingRate(44100); // 44.1kHz
                recorder.prepare();
                recorder.start();
                
                // Update UI
                isRecording = true;
                micButton.setBackgroundResource(R.drawable.circle_recording_bg);
                liveText.setText("Recording...");
                
                // Start pulse animation
                handler.post(pulseAnimation);
                
                Log.d(TAG, "Recording started");
                
            } catch (IOException e) {
                Log.e(TAG, "Recording failed: " + e.getMessage());
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void stopRecordingAndProcess() {
        if (isRecording) {
            try {
                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;
                
                // Update UI
                isRecording = false;
                isProcessing = true;
                liveText.setText("Processing...");
                micButton.setBackgroundResource(R.drawable.circle_dark_bg);
                micButton.setEnabled(false);
                
                // Save file path before creating new one
                final String savedFilePath = fileName;
                
                // Prepare for next recording
                prepareOutputFile();
                
                Log.d(TAG, "Recording stopped and saved as MP3: " + savedFilePath);
                
                // Process the audio in a separate thread
                new Thread(() -> {
                    try {
                        String transcriptionResult;
                        String aiResponse;
                        
                        // Step 1: Send audio for transcription
                        if (useGemini) {
                            transcriptionResult = transcribeAudioGemini(savedFilePath);
                        } else {
                            transcriptionResult = transcribeAudio(savedFilePath);
                        }
                        
                        if (transcriptionResult == null) {
                            runOnUiThread(() -> {
                                Toast.makeText(LiveAudioActivity.this, 
                                        "Không thể chuyển đổi âm thanh thành văn bản", Toast.LENGTH_SHORT).show();
                                resetProcessingState();
                            });
                            return;
                        }
                        
                        // Update UI with transcription
                        runOnUiThread(() -> {
                            liveText.setText("Thinking...");
                        });

                        // Step 2: Send transcription to chat API
                        if (useGemini) {
                            aiResponse = getGeminiResponse(transcriptionResult);
                        } else {
                            aiResponse = getAiResponse(transcriptionResult);
                        }
                        
                        if (aiResponse == null) {
                            runOnUiThread(() -> {
                                Toast.makeText(LiveAudioActivity.this, 
                                        "Không thể nhận phản hồi từ AI", Toast.LENGTH_SHORT).show();
                                resetProcessingState();
                            });
                            return;
                        }
                        
                        // Step 3: Convert AI response to speech
                        String speechFilePath = textToSpeech(aiResponse);
                        if (speechFilePath == null) {
                            runOnUiThread(() -> {
                                Toast.makeText(LiveAudioActivity.this, 
                                        "Không thể tạo giọng nói", Toast.LENGTH_SHORT).show();
                                resetProcessingState();
                            });
                            return;
                        }
                        
                        // Step 4: Play the speech
                        final String finalTranscription = transcriptionResult;
                        final String finalAiResponse = aiResponse;
                        runOnUiThread(() -> {
                            liveText.setText("AI Assitant");
                            // Hiển thị nội dung transcription và response trong logcat
                            Log.d(TAG, "User said: " + finalTranscription);
                            Log.d(TAG, "AI response: " + finalAiResponse);
                            playAudio(speechFilePath);
                        });
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing audio: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(LiveAudioActivity.this, 
                                    "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            resetProcessingState();
                        });
                    }
                }).start();
                
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording: " + e.getMessage());
                resetProcessingState();
            }
        }
    }
    
    private void resetProcessingState() {
        isProcessing = false;
        micButton.setEnabled(true);
        liveText.setText("Hole to record");
    }
    
    private String transcribeAudio(String audioFilePath) {
        try {
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file does not exist: " + audioFilePath);
                return null;
            }
            
            // Create request body
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.getName(),
                            RequestBody.create(audioFile, MediaType.parse("audio/mpeg")))
                    .addFormDataPart("model", "whisper-1")
                    .build();
            
            // Create request
            Request request = new Request.Builder()
                    .url(API_URL_TRANSCRIPTION)
                    .header("Authorization", "Bearer " + Constants.getOpenAIApiKey(this))
                    .post(requestBody)
                    .build();
            
            // Execute request
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "Transcription failed: " + response.body().string());
                return null;
            }
            
            // Parse response
            String responseBody = response.body().string();
            Log.d(TAG, "Transcription text: "+ responseBody);
            JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
            return jsonObject.get("text").getAsString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error transcribing audio: " + e.getMessage());
            return null;
        }
    }

    private String transcribeAudioGemini(String audioFilePath) {
        try {
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file does not exist: " + audioFilePath);
                return null;
            }
            
            // Get MIME type and file size
            String mimeType = "audio/mpeg"; // Assuming MP3 format
            long fileSize = audioFile.length();
            String displayName = audioFile.getName();
            
            // Step 1: Initial resumable upload request
            RequestBody metadataBody = RequestBody.create(
                "{'file': {'display_name': '" + displayName + "'}}", 
                MediaType.parse("application/json"));
                
            Request initialRequest = new Request.Builder()
                .url(GEMINI_API_URL_UPLOAD + "?key=" + Constants.getGeminiApiKey(this))
                .header("X-Goog-Upload-Protocol", "resumable")
                .header("X-Goog-Upload-Command", "start")
                .header("X-Goog-Upload-Header-Content-Length", String.valueOf(fileSize))
                .header("X-Goog-Upload-Header-Content-Type", mimeType)
                .header("Content-Type", "application/json")
                .post(metadataBody)
                .build();
                
            Response initialResponse = client.newCall(initialRequest).execute();
            if (!initialResponse.isSuccessful()) {
                Log.e(TAG, "Failed to initiate upload: " + initialResponse.code());
                return null;
            }
            
            // Get upload URL from header
            String uploadUrl = initialResponse.header("X-Goog-Upload-URL");
            if (uploadUrl == null) {
                Log.e(TAG, "No upload URL in response");
                return null;
            }
            
            // Step 2: Upload the file
            RequestBody fileBody = RequestBody.create(audioFile, MediaType.parse(mimeType));
            Request uploadRequest = new Request.Builder()
                .url(uploadUrl)
                .header("X-Goog-Upload-Command", "upload, finalize")
                .header("X-Goog-Upload-Offset", "0")
                .post(fileBody)
                .build();
                
            Response uploadResponse = client.newCall(uploadRequest).execute();
            if (!uploadResponse.isSuccessful()) {
                Log.e(TAG, "Failed to upload file: " + uploadResponse.code());
                return null;
            }
            
            // Parse the upload response to get file ID
            String uploadResponseBody = uploadResponse.body().string();
            JsonObject fileInfo = new Gson().fromJson(uploadResponseBody, JsonObject.class);
            String fileId = fileInfo.get("name").getAsString();
            
            // Step 3: Use the file in transcription request
            String transcriptionJson = "{"
                + "\"contents\": [{"
                + "  \"parts\": ["
                + "    {\"file_data\": {\"file_uri\": \"" + fileId + "\", \"mime_type\": \"" + mimeType + "\"}}"
                + "  ]"
                + "}]"
                + "}";
                
            RequestBody transcriptionBody = RequestBody.create(
                transcriptionJson, 
                MediaType.parse("application/json"));
                
            Request transcriptionRequest = new Request.Builder()
                .url(GEMINI_API_URL_GENERATION + "?key=" + Constants.getGeminiApiKey(this))
                .header("Content-Type", "application/json")
                .post(transcriptionBody)
                .build();
                
            Response transcriptionResponse = client.newCall(transcriptionRequest).execute();
            if (!transcriptionResponse.isSuccessful()) {
                Log.e(TAG, "Transcription failed: " + transcriptionResponse.body().string());
                return null;
            }
            
            // Parse transcription response
            String transcriptionResponseBody = transcriptionResponse.body().string();
            JsonObject result = new Gson().fromJson(transcriptionResponseBody, JsonObject.class);
            
            // Extract text from response
            JsonArray candidates = result.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    JsonElement textElement = parts.get(0).getAsJsonObject().get("text");
                    if (textElement != null) {
                        return textElement.getAsString();
                    }
                }
            }
            
            Log.e(TAG, "Could not extract transcription from response: " + transcriptionResponseBody);
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error transcribing audio with Gemini: " + e.getMessage());
            return null;
        }
    }
    
    private String getAiResponse(String userMessage) {
        try {
            // Create request JSON
            String requestJson = "{"
                    + "\"model\": \"gpt-4o\","
                    + "\"messages\": ["
                    + "{"
                    + "\"role\": \"system\","
                    + "\"content\": \"Bạn là một trợ lý ảo tài năng. Hãy trả lời ngắn ngọn nhất có thể.\""
                    + "},"
                    + "{"
                    + "\"role\": \"user\","
                    + "\"content\": \"" + userMessage.replace("\"", "\\\"") + "\""
                    + "}"
                    + "]"
                    + "}";
            
            // Create request body
            RequestBody requestBody = RequestBody.create(requestJson, MediaType.parse("application/json"));
            
            // Create request
            Request request = new Request.Builder()
                    .url(API_URL_CHAT)
                    .header("Authorization", "Bearer " + Constants.getOpenAIApiKey(this))
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();
            
            // Execute request
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "AI response failed: " + response.body().string());
                return null;
            }
            
            // Parse response
            String responseBody = response.body().string();
            Log.d(TAG, "AI response body: " + responseBody);
            
            JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
            String content = jsonObject
                    .getAsJsonArray("choices")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();
            
            return content;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting AI response: " + e.getMessage());
            return null;
        }
    }

    private String getGeminiResponse(String userMessage) {
        try {
            // Create request JSON for Gemini
            String requestJson = "{"
                + "\"contents\": ["
                + "  {"
                + "    \"parts\": ["
                + "      {"
                + "        \"text\": \"" + userMessage.replace("\"", "\\\"") + "\""
                + "      }"
                + "    ]"
                + "  }"
                + "]"
                + "}";
            
            // Create request body
            RequestBody requestBody = RequestBody.create(requestJson, MediaType.parse("application/json"));
            
            // Create request
            Request request = new Request.Builder()
                .url(GEMINI_API_URL_GENERATION + "?key=" + Constants.getGeminiApiKey(this))
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();
            
            // Execute request
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "Gemini response failed: " + response.body().string());
                return null;
            }
            
            // Parse response
            String responseBody = response.body().string();
            Log.d(TAG, "Gemini response body: " + responseBody);
            
            JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
            
            // Extract text from Gemini response
            JsonArray candidates = jsonObject.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    JsonElement textElement = parts.get(0).getAsJsonObject().get("text");
                    if (textElement != null) {
                        return textElement.getAsString();
                    }
                }
            }
            
            Log.e(TAG, "Could not extract response text from Gemini: " + responseBody);
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting Gemini response: " + e.getMessage());
            return null;
        }
    }
    
    private String textToSpeech(String text) {
        try {
            // Create request JSON
            String requestJson = "{"
                    + "\"model\": \"gpt-4o-mini-tts\","
                    + "\"input\": \"" + text.replace("\"", "\\\"") + "\","
                    + "\"instructions\": \"Nói với giọng điệu vui vẻ và tích cực.\","
                    + "\"voice\": \"alloy\""
                    + "}";
            
            // Create request body
            RequestBody requestBody = RequestBody.create(requestJson, MediaType.parse("application/json"));
            
            // Create request
            Request request = new Request.Builder()
                    .url(API_URL_TTS)
                    .header("Authorization", "Bearer " + Constants.getOpenAIApiKey(this))
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();
            
            // Execute request
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "Text-to-speech failed: " + response.body().string());
                return null;
            }
            
            // Save response audio to file
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return null;
            }
            
            // Create new file for speech output
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File storageDir = getExternalFilesDir(null);
            String speechFilePath = storageDir.getAbsolutePath() + "/SPEECH_" + timestamp + ".mp3";
            
            // Write audio data to file
            FileOutputStream fos = new FileOutputStream(speechFilePath);
            fos.write(responseBody.bytes());
            fos.close();
            
            return speechFilePath;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in text-to-speech: " + e.getMessage());
            return null;
        }
    }
    
    private void playAudio(String audioFilePath) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            
            // Đặt trạng thái ban đầu cho wave visual
            waveBg.setScaleX(1.0f);
            waveBg.setScaleY(1.0f);
            waveBg.setAlpha(0.6f);
            
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                startPulseAnimationForPlayback();
            });
            
            mediaPlayer.setOnCompletionListener(mp -> {
                waveBg.animate()
                        .alpha(1.0f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(500)
                        .start();
                resetProcessingState();
            });
            
            mediaPlayer.prepareAsync();
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio: " + e.getMessage());
            resetProcessingState();
        }
    }
    
    private void startPulseAnimationForPlayback() {
        // Set initial state
        waveBg.setAlpha(0.8f);
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    // Create smoother wave effect for playback
                    float randomScale = 1.0f + (float)(Math.random() * 0.1);
                    
                    waveBg.animate()
                            .scaleX(randomScale)
                            .scaleY(randomScale + 0.05f)
                            .alpha(0.9f)
                            .setDuration(300)
                            .withEndAction(() -> {
                                float nextRandomScale = 1.0f + (float)(Math.random() * 0.15);
                                waveBg.animate()
                                        .scaleX(nextRandomScale - 0.05f)
                                        .scaleY(nextRandomScale)
                                        .alpha(0.7f)
                                        .setDuration(300)
                                        .start();
                            })
                            .start();
                    
                    handler.postDelayed(this, 600);
                } else {
                    // Reset state if not playing
                    waveBg.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(1.0f)
                            .setDuration(500)
                            .start();
                }
            }
        });
    }
    
    private void handleEndClick() {
        // Make sure recording is stopped before finishing
        if (isRecording) {
            try {
                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording on close: " + e.getMessage());
            }
        }
        
        // Release media player if active
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "Error releasing media player: " + e.getMessage());
            }
        }
        
        // Close activity
        finish();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionGranted = grantResults.length > 0 && 
                               grantResults[0] == PackageManager.PERMISSION_GRANTED;
            
            if (!permissionGranted) {
                Toast.makeText(this, "Recording permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        handleEndClick();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        handleEndClick();
    }
} 
