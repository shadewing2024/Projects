package com.smartaqi.myapplication;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class AppLifecycleObserver implements DefaultLifecycleObserver {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d("APP_LIFECYCLE", "App is in FOREGROUND");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            updateUserOnline(true, user);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        Log.d("APP_LIFECYCLE", "App is in BACKGROUND");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            updateUserOnline(false, user);
        }
    }

    private void updateUserOnline(boolean isOnline, FirebaseUser user) {
        if (user == null) return;

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        Map<String, Object> userInfo = new HashMap<>();

        if (isOnline) {
            userInfo.put("Status", true);
            userInfo.put("Status_color", "GREEN");
            userInfo.put("Login_Time", formattedDateTime);
        } else {
            userInfo.put("Status", false);
            userInfo.put("Status_color", "RED");
            userInfo.put("Logout_Time", formattedDateTime);
        }

        db.collection("users")
                .document(user.getUid())
                .update(userInfo)
                .addOnFailureListener(e -> {
                    // Log error if needed: Log.e("LOG", e.getMessage());
                });
    }
}