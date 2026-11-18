package com.smartaqi.myapplication;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import android.graphics.Color; // 👈 Add this
import android.util.Log;
// ... other imports

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class ManageAQI {

    public interface adviceCallback {
        void onRetun(String color/*, String startDate, String endDate*/);
    }

    public void returnAQI(int predictedAQI, adviceCallback callback) {

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("AQI_Collection")
                    .document("AQI_Doc")
                    .collection("Catalog")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {

                        boolean found = false;

                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {

                            Long maxValLong = document.getLong("maximum");
                            Long minValLong = document.getLong("minimum");

                            int maxVal = maxValLong != null ? maxValLong.intValue() : 0;
                            int minVal = minValLong != null ? minValLong.intValue() : 0;

                            // ✅ CORRECT RANGE CHECK
                            if (predictedAQI >= minVal && predictedAQI <= maxVal) {

                                found = true;

                                String category = document.getString("category");
                                String impact = document.getString("health_impact");
                                String action = document.getString("recommendation");
                                String color = document.getString("color");
                                String date = document.getString("formatted_time");

                                /*if ( date != null && date.length() >= 10) {

                                    String datePart = date.substring(0, 10);

                                    LocalDate endLocalDate = LocalDate.parse(datePart);
                                    LocalDate startLocalDate = endLocalDate.minusDays(1);

                                    String startDate = startLocalDate.toString();
                                    String endDate = endLocalDate.toString();

                                    Log.d("ALERT_DEBUG", "MATCH FOUND: " + document.getData());

                                    callback.onRetun(color, startDate, endDate);
                                }*/

                                callback.onRetun(color);


                                break; // stop loop
                            }
                        }

                        if (!found) {
                            Log.e("ALERT_DEBUG", "❌ No matching AQI range found");
                        }

                    })
                    .addOnFailureListener(e -> {
                        Log.e("ALERT_DEBUG", "🔥 ERROR: " + e.getMessage());
                    });

    }


    // 1. Define the Callback Interface
    public interface ColorCallback {
        void onColorsLoaded(int[] colors);
    }

    // Get Color Collection
    public void getColorCollection(ColorCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("AQI_Collection")
                .document("AQI_Doc")
                .collection("Catalog")
                .orderBy("maximum", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Integer> colorList = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {

                        // Inside getColorCollection addOnSuccessListener loop:
                        String colorStr = document.getString("color");
                        if (colorStr != null) {
                            // ✅ Use the standard android.graphics.Color
                            colorList.add(Color.parseColor(colorStr));
                        }
                    }

                    // Convert List to primitive int array
                    int[] colorsArray = new int[colorList.size()];
                    for (int i = 0; i < colorList.size(); i++) {
                        colorsArray[i] = colorList.get(i);
                    }

                    // 2. Send the array back to the Activity
                    callback.onColorsLoaded(colorsArray);
                })
                .addOnFailureListener(e -> Log.e("FIREBASE", "Error: " + e.getMessage()));
    }

    // 2. Define the Callback Interface
    public interface maxValueCallback {
        void onMaxLoaded(int lastMaxValue);
    }

    public void getLastAddedMaxValue(maxValueCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("AQI_Collection")
                .document("AQI_Doc")
                .collection("Catalog")
                .orderBy("maximum", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (!querySnapshot.isEmpty()) {

                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                        Long maxValLong = doc.getLong("maximum");
                        int lastMaxVal = maxValLong != null ? maxValLong.intValue() : 0;

                        // 2. Send the array back to the Activity
                        callback.onMaxLoaded(lastMaxVal-50);
                    }

                })
                .addOnFailureListener(e -> Log.e("FIREBASE", "Error: " + e.getMessage()));
    }

    public interface firstTimeCallback {
        void onFirstTime(boolean noFirst);
    }

    public void firstTime(firstTimeCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) return;

        String uID = user.getUid();

        db.collection("users") // ✅ FIXED
                .document(uID)
                .get()
                .addOnSuccessListener(snap -> {

                    if (!snap.exists()) {
                        callback.onFirstTime(true); // ✅ new user
                        return;
                    }

                    Boolean isFirst = snap.getBoolean("FirstTime");

                    if (isFirst == null) isFirst = true;

                    callback.onFirstTime(isFirst);
                });
    }

}
