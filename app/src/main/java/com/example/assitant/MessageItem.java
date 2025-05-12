package com.example.assitant;

import java.util.Date;

public class MessageItem {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    
    private String content;
    private int type;
    private Date timestamp;
    private boolean isStreaming;

    public MessageItem(String content, int type) {
        this.content = content;
        this.type = type;
        this.timestamp = new Date();
        this.isStreaming = false;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public Date getTimestamp() {
        return timestamp;
    }
    
    public boolean isStreaming() {
        return isStreaming;
    }
    
    public void setStreaming(boolean streaming) {
        isStreaming = streaming;
    }
} 