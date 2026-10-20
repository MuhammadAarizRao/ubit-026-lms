package com.ubit.lms;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String chatId, otherPartyName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        chatId = getIntent().getStringExtra("chatId");
        otherPartyName = getIntent().getStringExtra("otherPartyName");

        TextView titleText = findViewById(R.id.chatTitleText);
        titleText.setText(otherPartyName != null ? otherPartyName : "Chat");

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.chatMessageInput);
        sendButton = findViewById(R.id.sendMessageButton);

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        sendButton.setOnClickListener(v -> sendMessage());

        listenForMessages();
        markChatAsRead();
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", uid);
        message.put("text", text);
        message.put("timestamp", FieldValue.serverTimestamp());

        db.collection("chats").document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener(docRef -> messageInput.setText(""))
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void listenForMessages() {
        String currentUid = mAuth.getCurrentUser().getUid();

        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) return;

                    List<ChatMessageItem> messages = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String senderId = doc.getString("senderId");
                        String text = doc.getString("text");
                        java.util.Date timestamp = doc.getDate("timestamp");
                        messages.add(new ChatMessageItem(senderId, text, timestamp));
                    }

                    ChatAdapter adapter = new ChatAdapter(messages, currentUid);
                    chatRecyclerView.setAdapter(adapter);
                    if (!messages.isEmpty()) {
                        chatRecyclerView.scrollToPosition(messages.size() - 1);
                    }

                    markChatAsRead();
                });
    }

    private void markChatAsRead() {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> readStatus = new HashMap<>();
        readStatus.put("lastRead_" + uid, FieldValue.serverTimestamp());

        db.collection("chats").document(chatId)
                .set(readStatus, SetOptions.merge());
    }
}