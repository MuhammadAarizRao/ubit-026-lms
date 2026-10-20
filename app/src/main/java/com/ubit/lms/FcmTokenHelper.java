package com.ubit.lms;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class FcmTokenHelper {

    public static void saveTokenForCurrentUser() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Map<String, Object> update = new HashMap<>();
                    update.put("fcmToken", token);

                    FirebaseFirestore.getInstance().collection("users").document(uid)
                            .update(update);
                });
    }
}