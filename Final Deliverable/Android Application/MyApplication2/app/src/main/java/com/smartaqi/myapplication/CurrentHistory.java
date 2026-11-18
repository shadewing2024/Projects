package com.smartaqi.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import android.app.DatePickerDialog;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;


public class CurrentHistory extends AppCompatActivity {

    LineChart lineChart;
    TextView startTime, endTime, filterView;
    ListView listView;
    ImageButton filterInput;
    SQLiteData dbHelper;

    ManageAQI manageAQI = new ManageAQI();

    List<String> historyList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_history);

        lineChart = findViewById(R.id.lineChart);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);

        filterInput = findViewById(R.id.filterInput);
        filterView = findViewById(R.id.filterView);

        filterInput.setFocusable(false); // 👈 Prevents keyboard from opening
        filterInput.setClickable(true);  // Enables clicking
        filterInput.setOnClickListener(v -> showDatePicker());

        dbHelper = new SQLiteData(this);

        listView = findViewById(R.id.listViewHistory);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);
        loadHistory();


        // ✅ AQI vertical gradient bar
        ManageAQI manageAQI = new ManageAQI();

        manageAQI.getColorCollection(colors -> {
            if (colors.length > 0) {
                View aqiBar = findViewById(R.id.aqiBar);
                GradientDrawable gradient = new GradientDrawable(
                        GradientDrawable.Orientation.BOTTOM_TOP,
                        colors // 👈 Using your dynamic array here
                );
                gradient.setCornerRadius(10f);
                aqiBar.setBackground(gradient);
            }
        });

        setupChart();
        loadChartData("Karachi");
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
        // 1. Get all history from SQLite
        Cursor cursor = dbHelper.getAllAQI(city);

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> fullDates = new ArrayList<>();

        // Temporary lists to store the matches (since SQLite cursor is newest-first)
        List<Integer> tempAqi = new ArrayList<>();
        List<String> tempLabels = new ArrayList<>();
        List<String> tempFullDates = new ArrayList<>();

        long now = System.currentTimeMillis();
        long last24Hours = 24 * 60 * 60 * 1000;

        if (cursor != null && cursor.moveToFirst()) {
            int aqiIndex = cursor.getColumnIndexOrThrow("aqi");
            int timeIndex = cursor.getColumnIndexOrThrow("formattedTime");

            do {
                int aqi = cursor.getInt(aqiIndex);
                String time = cursor.getString(timeIndex);

                if (aqi != 0 && time != null) {
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        java.util.Date recordDate = sdf.parse(time);

                        if (recordDate != null) {
                            long recordTime = recordDate.getTime();

                            // ✅ FILTER FOR LAST 24 HOURS ONLY
                            if ((now - recordTime) <= last24Hours) {
                                tempAqi.add(aqi);
                                tempLabels.add(time.substring(11, 16)); // Time (HH:mm) for X-axis
                                tempFullDates.add(time.substring(0, 10)); // Date (yyyy-MM-dd) for Start/End
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (tempAqi.isEmpty()) return;

        // ✅ REVERSE DATA: Put oldest data on the LEFT (index 0) and newest on the RIGHT
        int xIndex = 0;
        for (int j = tempAqi.size() - 1; j >= 0; j--) {
            entries.add(new Entry(xIndex, tempAqi.get(j)));
            labels.add(tempLabels.get(j));
            fullDates.add(tempFullDates.get(j));
            xIndex++;
        }

        // ✅ SET START & END DATE TEXT (Based on actual graph data)
        startTime.setText(fullDates.get(0));
        endTime.setText(fullDates.get(fullDates.size() - 1));

        // ==========================================
        // ✅ STYLING THE GRAPH
        // ==========================================
        LineDataSet set = new LineDataSet(entries,"");
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        set.setLineWidth(2.5f);
        set.setColor(Color.WHITE);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.LTGRAY);

        // ==========================================
        // ✅ GRADIENT & VIEW CONFIGURATION
        // ==========================================
        manageAQI.getColorCollection(colors -> {
            if (colors != null && colors.length > 0) {
                int[] chartFillColors = new int[colors.length];
                for (int k = 0; k < colors.length; k++) {
                    chartFillColors[k] = Color.argb(180, Color.red(colors[k]), Color.green(colors[k]), Color.blue(colors[k]));
                }
                set.setDrawFilled(true);
                set.setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, chartFillColors));
            }

            LineData data = new LineData(set);
            lineChart.setData(data);

            // Zoom and marker settings
            lineChart.setVisibleXRangeMaximum(6);
            lineChart.moveViewToX(entries.size());
            lineChart.setMarker(new AQIMarkerView(this));
            lineChart.setDragEnabled(true);
            lineChart.setScaleEnabled(true);
            lineChart.setPinchZoom(true);

            lineChart.invalidate();
        });
    }

    private void loadHistory() {

        Cursor cursor = dbHelper.getAllAQI("Karachi");

        historyList.clear();

        if (cursor != null && cursor.moveToFirst()) {

            int aqiIndex = cursor.getColumnIndex("aqi");
            int tempIndex = cursor.getColumnIndex("temprature");
            int humIndex = cursor.getColumnIndex("humidity");
            int windIndex = cursor.getColumnIndex("wind");
            int timeIndex = cursor.getColumnIndex("formattedTime");
            int colorIndex = cursor.getColumnIndex("color");

            do {

                int aqi = cursor.getInt(aqiIndex);
                int temp = cursor.getInt(tempIndex);
                int humidity = cursor.getInt(humIndex);
                int wind = cursor.getInt(windIndex);
                String time = cursor.getString(timeIndex);

                // ✅ color per row
                String color = "#888888";
                if (colorIndex != -1) {
                    String dbColor = cursor.getString(colorIndex);
                    if (dbColor != null && !dbColor.isEmpty()) {
                        color = dbColor;
                    }
                }

                // ✅ store everything in ONE string
                String item = time + "|" + aqi + "|" + temp + "|" + humidity + "|" + wind + "|" + color;

                historyList.add(item);

            } while (cursor.moveToNext());

            cursor.close();
        }

        adapter = new ArrayAdapter<String>(this, 0, historyList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_history, parent, false);
                }

                TextView timeText = convertView.findViewById(R.id.txtTime);
                TextView aqiText = convertView.findViewById(R.id.txtAQI);
                TextView tempText = convertView.findViewById(R.id.txtTemp);
                TextView humidityText = convertView.findViewById(R.id.txtHumidity);
                TextView windText = convertView.findViewById(R.id.txtWind);

                try {

                    String[] data = historyList.get(position).split("\\|");

                    String time = data[0];
                    int aqi = Integer.parseInt(data[1]);
                    String temp = data[2];
                    String humidity = data[3];
                    String wind = data[4];
                    String color = data[5];

                    timeText.setText("Time: " + time);
                    aqiText.setText("AQI: " + aqi);
                    tempText.setText("Temp: " + temp + "°C");
                    humidityText.setText("Humidity: " + humidity + "%");
                    windText.setText("Wind: " + wind + " km/h");

                    // ✅ APPLY COLOR (your goal)
                    try {
                        aqiText.setTextColor(Color.parseColor(color));
                    } catch (Exception e) {
                        aqiText.setTextColor(Color.GRAY);
                    }

                } catch (Exception e) {
                    Log.e("PARSE_ERROR", e.getMessage());
                }

                return convertView;
            }
        };

        listView.setAdapter(adapter);
    }

    // ================= FILTER =================
    private void filterData(String getDate) {
        // 1. Get all data from the database
        Cursor cursor = dbHelper.getAllAQI("Karachi");

        // 2. Clear the current list
        historyList.clear();

        if (cursor != null && cursor.moveToFirst()) {
            int aqiIndex = cursor.getColumnIndex("aqi");
            int tempIndex = cursor.getColumnIndex("temprature");
            int humIndex = cursor.getColumnIndex("humidity");
            int windIndex = cursor.getColumnIndex("wind");
            int timeIndex = cursor.getColumnIndex("formattedTime");
            int colorIndex = cursor.getColumnIndex("color");

            do {
                String time = cursor.getString(timeIndex);

                // ✅ THE FILTER LOGIC: Check if this record belongs to the selected date
                // This works because "2024-05-20 14:30" starts with "2024-05-20"
                if (time != null && time.startsWith(getDate)) {

                    int aqi = cursor.getInt(aqiIndex);
                    int temp = cursor.getInt(tempIndex);
                    int humidity = cursor.getInt(humIndex);
                    int wind = cursor.getInt(windIndex);

                    String color = "#888888";
                    if (colorIndex != -1) {
                        String dbColor = cursor.getString(colorIndex);
                        if (dbColor != null && !dbColor.isEmpty()) {
                            color = dbColor;
                        }
                    }

                    String item = time + "|" + aqi + "|" + temp + "|" + humidity + "|" + wind + "|" + color;
                    historyList.add(item);
                }

            } while (cursor.moveToNext());

            cursor.close();
        }

        // 3. ✅ CRITICAL: Tell the list to refresh with the filtered data
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        // 4. Optional: Show a message if no data was found
        if (historyList.isEmpty()) {
            Toast.makeText(this, "No history found for " + getDate, Toast.LENGTH_SHORT).show();
        }
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
}