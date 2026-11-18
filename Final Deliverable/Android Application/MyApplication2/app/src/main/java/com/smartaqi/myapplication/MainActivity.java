package com.smartaqi.myapplication;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.firebase.ui.auth.AuthUI;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.ResourceDescriptor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    // UI
    TextView predictionTextView;
    TextView viewTemp;
    TextView viewHumidity;
    TextView viewWind;

    TextView textCategory;
    TextView textImpact;
    TextView textAction;

    Button manual, history, profile;

    CardView statusCardContainer;

    // Graph
    LineChart lineChart;

    // Database
    SQLiteData sqLiteData;
    ManageAQI manageAQI;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    // ==========================================
    // ON CREATE
    // ==========================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ==========================================
        // INITIALIZE UI
        // ==========================================
        predictionTextView = findViewById(R.id.predictionTextView);

        viewTemp = findViewById(R.id.viewTemp);
        viewHumidity = findViewById(R.id.viewHumidity);
        viewWind = findViewById(R.id.viewWind);

        textCategory = findViewById(R.id.textCategory);
        textImpact = findViewById(R.id.textImpact);
        textAction = findViewById(R.id.textAction);

        manual = findViewById(R.id.manual);
        history = findViewById(R.id.history);
        profile = findViewById(R.id.profile);

        statusCardContainer = findViewById(R.id.statusCardContainer);

        lineChart = findViewById(R.id.homePredictionChart);

        // ==========================================
        // DATABASE
        // ==========================================
        sqLiteData = new SQLiteData(this);
        manageAQI = new ManageAQI();

        // ==========================================
        // GRAPH
        // ==========================================
        setupChartStyle();
//        loadPredictionData();

        // ==========================================
        // NOTIFICATION
        // ==========================================
        checkNotificationChannel();

        // ==========================================
        // WORK MANAGER
        // ==========================================
        startWorkManager();

        // ==========================================
        // BOTTOM NAVIGATION
        // ==========================================
        manual.setOnClickListener(v -> {
            startActivity(new Intent(this, EnterManually.class));
        });

        history.setOnClickListener(v -> {
            startActivity(new Intent(this, CurrentHistory.class));
        });

        profile.setOnClickListener(v -> {
            startActivity(new Intent(this, Profile.class));
        });


        // ==========================================
        // FIREBASE USER STATUS
        // ==========================================
        if (user != null) {

            Map<String, Object> online = new HashMap<>();
            online.put("Status", true);
            online.put("Status_color", "GREEN");

            db.collection("users")
                    .document(user.getUid())
                    .update(online);

            manageAQI.firstTime(isFirst -> {

                readUserData();

                if (isFirst) {


                    writeUserData(false);
                }
            });
        }

        // ==========================================
        // LIVE SYNC
        // ==========================================
        sqLiteData.startLiveSync("Karachi", new SQLiteData.OnSyncCompleteListener() {

                    @Override
                    public void onSyncSuccess() {

                        runOnUiThread(() -> {

                            getAQI("Karachi");
                            loadPredictionData();
                        });
                    }
                });
    }

    /*// ==========================================
    // ON RESUME
    // ==========================================
    @Override
    protected void onResume() {
        super.onResume();

        getAQI("Karachi");

        loadPredictionData();
    }*/

    // ==========================================
    // WORK MANAGER
    // ==========================================
    private void startWorkManager() {
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(PredictionWorker.class, 45, TimeUnit.MINUTES)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED) // Run when connected to internet
                                // .setRequiresCharging(true) // REMOVE THIS
                                .build())
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "AQI_WORK",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
        );
    }

    // ==========================================
    // SETUP CHART STYLE
    // ==========================================
    // ============graph==========
    private void setupChartStyle() {
        if (lineChart == null) return;

        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getAxisRight().setEnabled(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.GRAY);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setAxisMinimum(0f);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelCount(10, true);
    }

    private void loadPredictionData() {
        Cursor cursor = sqLiteData.getFutureAQI("Karachi");
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        String minStr = String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.MINUTE));

        int index = 0;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int aqiCol = cursor.getColumnIndex("predictedAQI");
                if (aqiCol != -1) {
                    float aqi = cursor.getFloat(aqiCol);
                    entries.add(new Entry(index, aqi));

                    calendar.add(Calendar.HOUR_OF_DAY, 1);
                    labels.add(calendar.get(Calendar.HOUR_OF_DAY) + ":" + minStr);
                    index++;
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        // CRASH FIX: Don't update chart if no data
        if (entries.isEmpty() || lineChart == null) return;

        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        String color = "#89CFF0";

        LineDataSet dataSet = new LineDataSet(entries, "Future AQI");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(Color.parseColor(color)); // Dark Orange
        dataSet.setLineWidth(3.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);

        // Gradient Fill
        dataSet.setDrawFilled(true);
        int[] fillColors = {Color.parseColor(color), Color.argb(0, 255, 204, 128)};
        dataSet.setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, fillColors));

        // Highlighting
        dataSet.setHighlightEnabled(true);
        dataSet.setDrawHorizontalHighlightIndicator(false);
        dataSet.setHighLightColor(Color.parseColor("#FF8C00"));

        lineChart.setData(new LineData(dataSet));

        // Marker
        AQIMarkerView marker = new AQIMarkerView(this);
        marker.setChartView(lineChart);
        lineChart.setMarker(marker);

        lineChart.animateY(1000);
        lineChart.invalidate();
    }

    // ==========================================
    // GET AQI
    // ==========================================
    private void getAQI(String location) {

        Cursor cursor = null;

        try {

            cursor = sqLiteData.getLatestAQI(location);

            if (cursor != null && cursor.moveToFirst()) {

                int aqi =
                        cursor.getInt(
                                cursor.getColumnIndexOrThrow("aqi")
                        );

                float temp =
                        cursor.getFloat(
                                cursor.getColumnIndexOrThrow("temprature")
                        );

                float humidity =
                        cursor.getFloat(
                                cursor.getColumnIndexOrThrow("humidity")
                        );

                float wind =
                        cursor.getFloat(
                                cursor.getColumnIndexOrThrow("wind")
                        );

                String color =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("color")
                        );

                String category =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("category")
                        );

                String impact =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("impact")
                        );

                String action =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("re_Action")
                        );

                // AQI
                predictionTextView.setText(String.valueOf(aqi));

                // WEATHER
                viewTemp.setText(temp + "°C");

                viewHumidity.setText(humidity + "%");

                viewWind.setText(wind + " km/h");

                // CATEGORY
                textCategory.setText(category);

                textCategory.setTextColor(
                        Color.parseColor(color)
                );

                textCategory.setGravity(Gravity.CENTER);

                textImpact.setText(impact);

                textAction.setText(action);

                // CARD COLOR
                statusCardContainer.setCardBackgroundColor(Color.WHITE);
            }

        } catch (Exception e) {

            Log.e("AQI_ERROR", e.getMessage());

        } finally {

            if (cursor != null) {

                cursor.close();
            }
        }
    }

    // ==========================================
    // TUTORIAL
    // ==========================================
    private void showTutorial() {

        new AlertDialog.Builder(this)
                .setTitle("Welcome to Smart AQI")
                .setMessage(
                        "• View air quality\n" +
                                "• Get alerts\n" +
                                "• Check history\n" +
                                "• Use manual input"
                )
                .setPositiveButton("Got it", null)
                .show();
    }



    // ==========================================
    // NOTIFICATION
    // ==========================================
    private void checkNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    101
            );
        }

        Alert alert = new Alert(this);

        alert.createNotificationChannel();
    }

    // ==========================================
    // WRITE USER
    // ==========================================
    private void writeUserData(boolean isFirstTime) {

        if (user == null) return;

        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd HH:mm:ss"
                );

        String formattedDateTime =
                now.format(formatter);

        Map<String, Object> userInfo =
                new HashMap<>();

        userInfo.put("ID", user.getUid());

        userInfo.put("Email", user.getEmail());

        userInfo.put("Name", "");

        userInfo.put("Phone", "");

        userInfo.put("Location", "");

        userInfo.put("Status_color", "RED");

        userInfo.put("Status", false);

        userInfo.put("Login_Time", formattedDateTime);

        userInfo.put("Logout_Time", "");

        userInfo.put("FirstTime", isFirstTime);

        userInfo.put("timestamp", System.currentTimeMillis());

        userInfo.put("Time", formattedDateTime);

        db.collection("users")
                .document(user.getUid())
                .set(userInfo, SetOptions.merge());
    }

    // ==========================================
    // READ USER
    // ==========================================
    private void readUserData() {

        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String name =
                                document.getString("Name");

                        String email =
                                user.getEmail();

                        String phone =
                                document.getString("Phone");

                        if (name != null &&
                                !name.trim().isEmpty()) {

                            sqLiteData.insertAndUpdateUser(
                                    user.getUid(),
                                    name,
                                    email,
                                    phone
                            );

                        } else {

                            startActivity(
                                    new Intent(
                                            MainActivity.this,
                                            CitienProfile.class
                                    )
                            );

                            finish();
                        }
                    }
                });
    }
}