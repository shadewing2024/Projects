package com.smartaqi.myapplication;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AutomaticallyPrediction {

    private static final String BASE_URL = "https://api.openweathermap.org/";
    private static final String API_KEY = "c6d98266a5bed995192f700305948ca7";

    private final AiModel aiModel;
    private SQLiteData sqLiteData;

    private ManageAQI manageAQI;
    private Context context;
    FirebaseFirestore db = FirebaseFirestore.getInstance();


    private ApiService apiService = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService.class);

    // ✅ Constructor
    public AutomaticallyPrediction(Context context) {
        this.context = context;
        this.aiModel = new AiModel(context);
        this.sqLiteData = new SQLiteData(context);
    }
    public void startPrediction() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("location") // ✅ FIXED
                .document("Karachi")
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        Double latObj = document.getDouble("latitude");
                        Double lonObj = document.getDouble("longitude");

                        if (latObj == null || lonObj == null) {
                            Log.e("FIRESTORE", "Lat/Lon missing");
                            return;
                        }

                        double lat = latObj;
                        double lon = lonObj;

                        fetchWeather(lat, lon);

                    } else {
                        Log.e("CITY", "No such document");
                    }
                });
    }

    // =========================
    // FETCH WEATHER + AI
    // =========================
    public void fetchWeather(double lat, double lon) {

        apiService.getWeather(lat, lon, API_KEY)
                .enqueue(new Callback<WeatherResponse>() {

                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {

                        if (response.isSuccessful() && response.body() != null) {

                            double tempK = response.body().getMain().getTemp();
                            double tempC = tempK - 273.15;

                            int humidity = response.body().getMain().getHumidity();
                            double pressure = response.body().getMain().getPressure();
                            double wind = response.body().getWind().getSpeed();

                            // 🔥 AI INPUT
                            float[] input = new float[]{
                                    (float) tempC,
                                    humidity,
                                    (float) wind
                            };

                            float[] result = aiModel.predict(input);

                            if (result != null && result.length > 0) {

                                float predictedAQI = result[0];
                                Log.d("AQI", "Predicted: " + predictedAQI);

                                // ✅ Save data
                                saveToFirestore(predictedAQI, input[0], input[1], input[2]);

                                /*//Send to AQI Manager
                                manageAQI.returnAQI(predictedAQI);*/

                                // ✅ Send alert ONLY for dangerous levels
                                alertLimitSeter(predictedAQI);

                            } else {
                                Log.e("AI", "No result");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        Log.e("WEATHER", "Error: " + t.getMessage());
                    }
                });
    }

    private void alertLimitSeter(float aqi) {

        db.collection("alert_setting")
                .document("settings")
                .get()
                .addOnSuccessListener( documentSnapshot -> {
                    if (documentSnapshot.exists()){
                        int limit = documentSnapshot.getDouble("Limit") != null ? documentSnapshot.getDouble("Limit").intValue() : 0;

                        if (limit == 0) {
                            return;
                        } else {
                            processAlert(aqi, limit);
                        }

                    }
                });

    }

    private void processAlert(float predictedAQI, int alertLimit) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ✅ KEEP YOUR CONDITION (threshold)
        if (predictedAQI < alertLimit) {
            return; // no alert below 150
        }

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

                            Log.d("ALERT_DEBUG", "MATCH FOUND: " + document.getData());

                            new Alert(context).sendAlert(predictedAQI, category, impact, action);
                            saveAlertToFirestore(category, predictedAQI, category, impact, action);

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

    // =========================
    // SAVE TO FIRESTORE
    // =========================
    public void saveToFirestore(float aqi, float temp, float humidity, float wind) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String city = "Karachi";

        Log.d("CITY", "City: " + city);

        long timestamp = System.currentTimeMillis();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);
        System.out.println(formattedDateTime); // Output: 2026-04-23 23:16:06

        try {

            Map<String, Object> data = new HashMap<>();

            data.put("aqi", aqi);
            data.put("temp", temp);
            data.put("humidity", humidity);
            data.put("wind", wind);
            data.put("timstamp", timestamp);
            data.put("formattedTime", formattedDateTime);

            db.collection("AQI")
                    .document(city)
                    .collection("history")
                    .add(data)   // ✅ CORRECT
                    .addOnSuccessListener(doc -> Log.d("FIRESTORE", "Saved"))
                    .addOnFailureListener(e -> Log.e("FIRESTORE", e.getMessage()));

            sqLiteData.insertAQI(city);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Save Alert to Firestore
    private void saveAlertToFirestore(String message, float aqi, String category, String impact, String action) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);
        System.out.println(formattedDateTime); // Output: 2026-04-23 23:16:06

        Map<String, Object> alert = new HashMap<>();
        alert.put("message", message);
        alert.put("aqi", aqi);
        alert.put("category", category);
        alert.put("impact", impact);
        alert.put("action", action);
        alert.put("time", android.text.format.DateFormat.format("hh:mm a", new java.util.Date()));
        alert.put("date", android.text.format.DateFormat.format("yyyy-MM-dd", new java.util.Date()));
        alert.put("formattedTime", formattedDateTime);
        alert.put("timestamp", System.currentTimeMillis());

        db.collection("aqi_alerts")
                .add(alert)
                .addOnSuccessListener(doc ->
                        Log.d("ALERT", "Saved: " + doc.getId()))
                .addOnFailureListener(e ->
                        Log.e("ALERT", "Failed", e));
    }
}