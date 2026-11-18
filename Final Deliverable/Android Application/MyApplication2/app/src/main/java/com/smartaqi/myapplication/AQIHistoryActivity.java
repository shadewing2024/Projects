package com.smartaqi.myapplication;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.firestore.*;

import java.util.ArrayList;

import android.app.DatePickerDialog;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AQIHistoryActivity extends AppCompatActivity {


    LineChart lineChart;
    TextView startTime, endTime, filterView;
    ListView listView;
    ImageButton filterInput;
    FirebaseFirestore db;

    ManageAQI manageAQI = new ManageAQI();
    ArrayList<DocumentSnapshot> list;
    ArrayAdapter<DocumentSnapshot> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aqihistory);

        lineChart = findViewById(R.id.lineChart);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);

        listView = findViewById(R.id.listView);
        filterInput = findViewById(R.id.filterInput);
        filterView = findViewById(R.id.filterView);

        filterInput.setFocusable(false); // 👈 Prevents keyboard from opening
        filterInput.setClickable(true);  // Enables clicking
        filterInput.setOnClickListener(v -> showDatePicker());

        db = FirebaseFirestore.getInstance();

        list = new ArrayList<>();

        manageAQI.getColorCollection(colors -> {
            if (colors.length > 0) {
                View aqiBar = findViewById(R.id.aqiBar);

                GradientDrawable gradient = new GradientDrawable(
                        GradientDrawable.Orientation.BOTTOM_TOP,
                        colors
                );

                gradient.setCornerRadius(10f);
                aqiBar.setBackground(gradient);
            }
        });

        setupChart();
        loadChartData("Karachi"); // 🔥 REQUIRED

        adapter = new ArrayAdapter<DocumentSnapshot>(this, 0, list) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_history, parent, false);
                }

                TextView txtAQI = convertView.findViewById(R.id.txtAQI);
                TextView txtTemp = convertView.findViewById(R.id.txtTemp);
                TextView txtHumidity = convertView.findViewById(R.id.txtHumidity);
                TextView txtWind = convertView.findViewById(R.id.txtWind);
                TextView txtTime = convertView.findViewById(R.id.txtTime);

                DocumentSnapshot doc = list.get(position);

                int aqi = doc.getDouble("aqi") != null ? doc.getDouble("aqi").intValue() : 0;
                int temp = doc.getDouble("temp") != null ? (int) Math.round(doc.getDouble("temp")) : 0;
                int humidity = doc.getDouble("humidity") != null ? doc.getDouble("humidity").intValue() : 0;
                double wind = doc.getDouble("wind") != null ? doc.getDouble("wind") : 0;
                String time = doc.getString("formattedTime") != null ? doc.getString("formattedTime") : null;

                txtAQI.setText("AQI: " + aqi);
                txtTemp.setText("Temp: " + temp);
                txtHumidity.setText("Humidity: " + humidity);
                txtWind.setText(String.format(Locale.getDefault(), "Wind: %.1f km/h", wind));
                txtTime.setText(time);

                manageAQI.returnAQI(aqi, (color) -> {
                    txtAQI.setTextColor(Color.parseColor(color));
                });

                return convertView;
            }
        };

        listView.setAdapter(adapter);

        loadData();

        // DELETE
        listView.setOnItemLongClickListener((parent, view, position, id) -> {

            String docId = list.get(position).getId();

            new AlertDialog.Builder(this)
                    .setTitle("Delete Record?")
                    .setMessage("Are you sure you want to delete this AQI record?")
                    .setPositiveButton("Yes", (d, w) -> deleteRecord(docId))
                    .setNegativeButton("No", null)
                    .show();

            return true;
        });
    }

    private void setupChart() {

        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);

        manageAQI.getLastAddedMaxValue(lastMaxValue -> {
            if (lastMaxValue > 0) {

                YAxis left = lineChart.getAxisLeft();
                left.setAxisMinimum(0f);
                left.setAxisMaximum(lastMaxValue);
                left.setTextColor(Color.argb(255, 170, 170, 170));
                left.setDrawGridLines(false);

            }
        });


        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(Color.argb(255, 170, 170, 170));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
    }

    private void loadChartData(String city) {
        db.collection("AQI")
                .document(city)
                .collection("history")
                .orderBy("timstamp", Query.Direction.ASCENDING) // ✅ Corrected field name
                .get()
                .addOnSuccessListener(query -> {
                    Log.d("CHART_LOG", "Documents found: " + query.size());

                    List<Entry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    List<String> fullTimes = new ArrayList<>();

                    int index = 0;
                    long now = System.currentTimeMillis();
                    long last24Hours = 24 * 60 * 60 * 1000;

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Double aqiVal = doc.getDouble("aqi");
                        String time = doc.getString("formattedTime");

                        if (aqiVal != null && time != null) {
                            try {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                java.util.Date recordDate = sdf.parse(time);

                                if (recordDate == null) continue;
                                long recordTime = recordDate.getTime();

                                // ✅ Filter for last 24 hours
                                if ((now - recordTime) <= last24Hours) {
                                    String hourMinute = time.substring(11, 16);

                                    entries.add(new Entry(index, aqiVal.floatValue()));
                                    labels.add(hourMinute);
                                    fullTimes.add(time);
                                    index++;
                                }
                            } catch (Exception e) {
                                Log.e("CHART_LOG", "Parsing error: " + e.getMessage());
                            }
                        }
                    }

                    if (entries.isEmpty()) {
                        Log.d("CHART_LOG", "No data found for the last 24 hours.");
                        return;
                    }

                    // ✅ UPDATE START & END DATE TEXT VIEWS
                    try {
                        String firstTime = fullTimes.get(0);
                        String lastTime = fullTimes.get(fullTimes.size() - 1);

                        java.text.SimpleDateFormat fullFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        java.text.SimpleDateFormat onlyDate = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                        startTime.setText(onlyDate.format(fullFormat.parse(firstTime)));
                        endTime.setText(onlyDate.format(fullFormat.parse(lastTime)));
                    } catch (Exception e) {
                        Log.e("CHART_LOG", "Date display error: " + e.getMessage());
                    }

                    // ✅ SETUP DATASET
                    LineDataSet set = new LineDataSet(entries, "");
                    set.setDrawCircles(false);
                    set.setDrawValues(false);
                    set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    set.setLineWidth(2f);
                    set.setColor(Color.WHITE);

                    // ✅ SETUP X-AXIS
                    XAxis xAxis = lineChart.getXAxis();
                    xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels));
                    xAxis.setGranularity(1f);
                    xAxis.setGranularityEnabled(true);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setTextColor(Color.LTGRAY);

                    // ✅ FETCH COLORS & SET GRADIENT
                    manageAQI.getColorCollection(colors -> {
                        if (colors != null && colors.length > 0) {
                            int[] chartFillColors = new int[colors.length];
                            for (int j = 0; j < colors.length; j++) {
                                chartFillColors[j] = Color.argb(180, Color.red(colors[j]), Color.green(colors[j]), Color.blue(colors[j]));
                            }
                            GradientDrawable chartGradient = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, chartFillColors);
                            set.setDrawFilled(true);
                            set.setFillDrawable(chartGradient);
                        }

                        LineData data = new LineData(set);
                        lineChart.setData(data);

                        // ✅ CONFIGURE VIEW
                        lineChart.setVisibleXRangeMaximum(6);
                        lineChart.moveViewToX(entries.size());
                        lineChart.setDragEnabled(true);
                        lineChart.setScaleEnabled(true);
                        lineChart.setPinchZoom(true);

                        // Add Marker
                        AQIMarkerView marker = new AQIMarkerView(this);
                        lineChart.setMarker(marker);

                        lineChart.invalidate();
                    });
                })
                .addOnFailureListener(e -> Log.e("CHART_ERROR", "Firestore Error: " + e.getMessage()));
    }

    // ================= LOAD =================
    private void loadData() {

        db.collection("AQI")
                .document("Karachi")
                .collection("history")
                .orderBy("timstamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {

                    list.clear();

                    for (DocumentSnapshot doc : query) {
                        list.add(doc);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Log.e("ERROR", e.getMessage())
                );
    }

    // ================= FILTER =================
    private void filterData(String getDate) {
        db.collection("AQI")
                .document("Karachi")
                .collection("history")
                // ✅ Fix: Use GreaterThan for the start of the day
                // and append a high value (like \uf8ff) to catch the whole day
                .whereGreaterThanOrEqualTo("formattedTime", getDate)
                .whereLessThanOrEqualTo("formattedTime", getDate + "\uf8ff")
                .get()
                .addOnSuccessListener(query -> {
                    list.clear();
                    for (DocumentSnapshot doc : query) {
                        list.add(doc);
                    }
                    adapter.notifyDataSetChanged();

                    if (list.isEmpty()) {
                        Toast.makeText(this, "No records found for this date", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FILTER_ERROR", e.getMessage());
                    Toast.makeText(this, "Filter failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    // ✅ Formats date as yyyy-MM-dd to match Firestore
                    String selectedDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                    filterView.setText(selectedDate);

                    filterData(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    // ================= DELETE =================
    private void deleteRecord(String docId) {

        db.collection("AQI")
                .document("Karachi")
                .collection("history")
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadData();
                });
    }
}