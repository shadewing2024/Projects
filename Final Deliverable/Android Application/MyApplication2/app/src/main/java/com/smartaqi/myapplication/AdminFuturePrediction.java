package com.smartaqi.myapplication;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AdminFuturePrediction extends AppCompatActivity {


    ListView listView;
    ImageButton filterInput;
    TextView filterView;
    LineChart lineChart;
    FirebaseFirestore db;
    ManageAQI manageAQI = new ManageAQI();

    ArrayList<DocumentSnapshot> list;

    ArrayAdapter<DocumentSnapshot> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_future_prediction);

        lineChart = findViewById(R.id.lineChart);
        db = FirebaseFirestore.getInstance();

        setupChartStyle();
        fetchLatestDataAndPredict("Karachi");
    }

    private void setupChartStyle() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setGridColor(Color.parseColor("#F0F0F0"));
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

    private void fetchLatestDataAndPredict(String city) {
        // Fetch the SINGLE most recent record from Firestore to use as a baseline
        db.collection("AQI")
                .document(city)
                .collection("history")
                .orderBy("timstamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot latestDoc = queryDocumentSnapshots.getDocuments().get(0);
                        float currentAQI = latestDoc.getDouble("aqi").floatValue();
                        generateFutureGraph(currentAQI);
                    } else {
                        Toast.makeText(this, "No history found to base prediction on", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e("FIRE_PREDICT", e.getMessage()));
    }

    private void generateFutureGraph(float startAQI) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        // Logic for Dynamic Time (e.g., if now is 4:34, labels start at 5:34)
        Calendar calendar = Calendar.getInstance();
        int currentMinutes = calendar.get(Calendar.MINUTE);
        String minStr = String.format(Locale.getDefault(), "%02d", currentMinutes);

        float lastValue = startAQI;

        for (int i = 0; i < 10; i++) {
            // Predictor logic: simulate minor fluctuations
            // In a real app, you'd pull weather factors here
            float fluctuation = (float) (Math.random() * 10 - 4); // Random change between -4 and +6
            lastValue += fluctuation;
            if (lastValue < 0) lastValue = 10; // Floor at 10

            entries.add(new Entry(i, lastValue));

            // Increment time by 1 hour
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            labels.add(calendar.get(Calendar.HOUR_OF_DAY) + ":" + minStr);
        }

        renderChart(entries, labels);
    }

    private void renderChart(ArrayList<Entry> entries, ArrayList<String> labels) {
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        LineDataSet dataSet = new LineDataSet(entries, "Predicted AQI");

        // ✅ THE ORANGE LINE STYLING
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.15f);
        dataSet.setColor(Color.parseColor("#FF8C00")); // Dark Orange
        dataSet.setLineWidth(3.5f);
        dataSet.setDrawCircles(false); // No circles
        dataSet.setDrawValues(false);  // No values on points

        // ✅ GRADIENT FILL
        dataSet.setDrawFilled(true);
        int[] fillColors = {
                Color.parseColor("#FFCC80"), // Light Orange (Top)
                Color.argb(0, 255, 204, 128)  // Transparent (Bottom)
        };
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                fillColors);
        dataSet.setFillDrawable(gradient);

        // ✅ HIGHLIGHTER / MARKER
        dataSet.setHighlightEnabled(true);
        dataSet.setDrawHorizontalHighlightIndicator(false);
        dataSet.setHighLightColor(Color.parseColor("#FF8C00"));

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Add the Marker (the black popup box)
        AQIMarkerView marker = new AQIMarkerView(this);
        marker.setChartView(lineChart);
        lineChart.setMarker(marker);

        lineChart.animateY(1000);
        lineChart.invalidate();
    }
}