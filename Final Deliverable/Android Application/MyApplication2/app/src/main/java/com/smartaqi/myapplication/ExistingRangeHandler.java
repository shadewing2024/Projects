package com.smartaqi.myapplication;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ExistingRangeHandler {

    private final FirebaseFirestore db;
    private final DocumentReference aqiDocument;
    private final String currentId;

    public ExistingRangeHandler(String id) {
        this.db = FirebaseFirestore.getInstance();
        this.currentId = id;

        // This points specifically to the document you want to update
        aqiDocument = db.collection("AQI_Collection")
                .document("AQI_Doc")
                .collection("Catalog")
                .document(id);
    }

    public interface RangeCheckCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // ✅ Renamed to match the call in AdminAQIHandler
    public void updateAQI(int start, int end, String category, String color, String impact, String recommendation, RangeCheckCallback callback) {

        if (start >= end) {
            callback.onError("Minimum must be less than Maximum");
            return;
        }

        // ✅ FIX: Query the whole collection to check for overlaps
        db.collection("AQI_Collection")
                .document("AQI_Doc")
                .collection("Catalog")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isOverlapping = false;

                        // task.getResult() is now a list (QuerySnapshot), so you can loop!
                        for (DocumentSnapshot doc : task.getResult()) {

                            // ⚠️ Skip the document we are currently editing
                            if (doc.getId().equals(currentId)) continue;

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
        String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("timestamp", System.currentTimeMillis());
        updateData.put("formatted_time", formattedTime);
        updateData.put("minimum", start);
        updateData.put("maximum", end);
        updateData.put("category", category);
        updateData.put("color", color);
        updateData.put("health_impact", impact);
        updateData.put("recommendation", recommendation);

        // ✅ Perform the UPDATE
        aqiDocument.update(updateData)
                .addOnSuccessListener(unused -> callback.onSuccess("AQI Updated Successfully"))
                .addOnFailureListener(e -> callback.onError("Update failed: " + e.getMessage()));
    }
}