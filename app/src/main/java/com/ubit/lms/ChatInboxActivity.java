package com.ubit.lms;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ChatInboxActivity extends AppCompatActivity {

    private LinearLayout chatInboxContainer;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUid, userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_inbox);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUid = mAuth.getCurrentUser().getUid();

        chatInboxContainer = findViewById(R.id.chatInboxContainer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserRoleThenChats();
    }

    private void loadUserRoleThenChats() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    userRole = doc.getString("role");

                    if ("teacher".equals(userRole)) {
                        loadTeacherChats();
                    } else {
                        loadStudentChats();
                    }
                });
    }

    private void loadTeacherChats() {
        db.collection("courses").whereEqualTo("teacherId", currentUid).get()
                .addOnSuccessListener(courseSnapshot -> {
                    chatInboxContainer.removeAllViews();

                    if (courseSnapshot.isEmpty()) {
                        showEmptyMessage();
                        return;
                    }

                    for (QueryDocumentSnapshot courseDoc : courseSnapshot) {
                        String courseId = courseDoc.getId();
                        String courseName = courseDoc.getString("name");
                        String crUid = courseDoc.getString("crStudentId");
                        String grUid = courseDoc.getString("grStudentId");

                        if (crUid != null) {
                            addChatRow(courseId + "_" + crUid, courseName + " — CR", courseId, crUid, false);
                        }
                        if (grUid != null) {
                            addChatRow(courseId + "_" + grUid, courseName + " — GR", courseId, grUid, false);
                        }
                        if (crUid != null || grUid != null) {
                            addChatRow(courseId + "_group", courseName + " — Group", null, null, true);
                        }
                    }
                });
    }

    private void loadStudentChats() {
        chatInboxContainer.removeAllViews();

        db.collection("courses").whereEqualTo("crStudentId", currentUid).get()
                .addOnSuccessListener(crSnapshot -> {
                    for (QueryDocumentSnapshot doc : crSnapshot) {
                        addStudentChatEntries(doc);
                    }

                    db.collection("courses").whereEqualTo("grStudentId", currentUid).get()
                            .addOnSuccessListener(grSnapshot -> {
                                for (QueryDocumentSnapshot doc : grSnapshot) {
                                    addStudentChatEntries(doc);
                                }

                                if (chatInboxContainer.getChildCount() == 0) {
                                    showEmptyMessage();
                                }
                            });
                });
    }

    private void addStudentChatEntries(QueryDocumentSnapshot courseDoc) {
        String courseId = courseDoc.getId();
        String courseName = courseDoc.getString("name");

        addChatRow(courseId + "_" + currentUid, courseName + " — Teacher", null, null, false);
        addChatRow(courseId + "_group", courseName + " — Group", null, null, true);
    }

    private void addChatRow(String chatId, String title, String courseId, String otherUid, boolean isGroup) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.item_chat_inbox_row, chatInboxContainer, false);

        TextView titleText = row.findViewById(R.id.inboxChatTitleText);
        TextView lastMessageText = row.findViewById(R.id.inboxLastMessageText);
        View unreadDot = row.findViewById(R.id.unreadDot);

        titleText.setText(title);
        lastMessageText.setText("Tap to open chat");

        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(msgSnapshot -> {
                    if (!msgSnapshot.isEmpty()) {
                        String lastText = msgSnapshot.getDocuments().get(0).getString("text");
                        lastMessageText.setText(lastText != null ? lastText : "Tap to open chat");

                        Timestamp lastMessageTime = msgSnapshot.getDocuments().get(0).getTimestamp("timestamp");
                        checkUnread(chatId, lastMessageTime, unreadDot);
                    }
                });

        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("chatId", chatId);
            intent.putExtra("otherPartyName", title);
            startActivity(intent);
        });

        chatInboxContainer.addView(row);
    }

    private void checkUnread(String chatId, Timestamp lastMessageTime, View unreadDot) {
        if (lastMessageTime == null) return;

        db.collection("chats").document(chatId).get()
                .addOnSuccessListener(doc -> {
                    Timestamp lastRead = doc.getTimestamp("lastRead_" + currentUid);

                    boolean isUnread = lastRead == null ||
                            lastMessageTime.toDate().getTime() > lastRead.toDate().getTime();

                    unreadDot.setVisibility(isUnread ? View.VISIBLE : View.GONE);
                });
    }

    private void showEmptyMessage() {
        TextView empty = new TextView(this);
        empty.setText("No chats yet.");
        empty.setTextColor(0xFF607D8B);
        chatInboxContainer.addView(empty);
    }
}