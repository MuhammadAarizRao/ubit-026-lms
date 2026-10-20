package com.ubit.lms;

import java.util.Date;

public class ChatMessageItem {
    private String senderId;
    private String text;
    private Date timestamp;

    public ChatMessageItem(String senderId, String text, Date timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public Date getTimestamp() { return timestamp; }
}