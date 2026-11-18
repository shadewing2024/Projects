package com.smartaqi.myapplication;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RangeHandler {

    private final FirebaseFirestore db;
    private final CollectionReference aqiCollection;

    public RangeHandler() {
        this.db = FirebaseFirestore.getInstance();
        // Path matches your specific Firestore structure
        this.aqiCollection = db.collection("AQI_Collection")
                .document("AQI_Doc")
                .collection("Catalog");
    }

    public interface RangeCheckCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public void addAqiRange(int start, int end, String category, String color, String impact, String recommendation, RangeCheckCallback callback) {

        // Validation: Start must be less than end
        if (start >= end) {
            callback.onError("Start range must be less than end range.");
            return;
        }

        // Check for overlaps: (NewStart <= ExistingEnd) AND (NewEnd >= ExistingStart)
        aqiCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean isOverlapping = false;

                for (DocumentSnapshot doc : task.getResult()) {
                    Long existingMin = doc.getLong("minimum");
                    Long existingMax = doc.getLong("maximum");

                    if (existingMin != null && existingMax != null) {
                        if (start <= existingMax && end >= existingMin) {
                            isOverlapping = true;
                            break;
                        }
                    }
                }

                if (isOverlapping) {
                    callback.onError("This range overlaps with an existing category.");
                } else {
                    saveToFirestore(start, end, category, color, impact, recommendation, callback);
                }
            } else {
                callback.onError("Database error: " + task.getException().getMessage());
            }
        });
    }

    private void saveToFirestore(int start, int end, String category, String color, String impact, String recommendation, RangeCheckCallback callback) {
        String formattedTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());

        Map<String, Object> newAQI = new HashMap<>();
        newAQI.put("timestamp", System.currentTimeMillis());
        newAQI.put("formatted_time", formattedTime);
        newAQI.put("minimum", start);
        newAQI.put("maximum", end);
        newAQI.put("category", category);
        newAQI.put("color", color);
        newAQI.put("health_impact", impact);
        newAQI.put("recommendation", recommendation);

        aqiCollection.add(newAQI)
                .addOnSuccessListener(doc -> callback.onSuccess("New AQI added successfully"))
                .addOnFailureListener(e -> callback.onError("Upload failed: " + e.getMessage()));
    }
}