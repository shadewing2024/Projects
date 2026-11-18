package com.smartaqi.myapplication;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class LogHelper {

    public static void log(String type, String message, int aqi) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "anonymous";

        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        HashMap<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("message", message);
        map.put("aqi", aqi);
        map.put("userId", userId);
        map.put("time", time);
        map.put("date", date);

        db.collection("user_logs").add(map);
    }
}