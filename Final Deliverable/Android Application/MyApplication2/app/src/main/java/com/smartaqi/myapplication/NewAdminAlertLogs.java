package com.smartaqi.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Locale;

public class NewAdminAlertLogs extends AppCompatActivity {

    ListView listAlertView;
    ImageButton alertSettingBtn;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ManageAQI manageAQI = new ManageAQI();
    ArrayList<DocumentSnapshot> list;
    ArrayAdapter<DocumentSnapshot> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_admin_alert_logs);

        listAlertView = findViewById(R.id.listAlertView);
        alertSettingBtn = findViewById(R.id.alertSettingBtn);

        alertSettingBtn.setOnClickListener(v -> {

            startActivity(new Intent(this, AlertSetting.class));
        });

        list = new ArrayList<>();
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

        listAlertView.setAdapter(adapter);
        loadData();

        // DELETE
        listAlertView.setOnItemLongClickListener((parent, view, position, id) -> {
            String docId = list.get(position).getId();
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Record?")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (d, w) -> deleteRecord(docId))
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    public void loadData() {

        db.collection("alert_setting")
                .document("settings")
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        return;
                    }

                    boolean isOn =
                            doc.getBoolean("Switch") != null
                                    ? doc.getBoolean("Switch")
                                    : false;

                    Long limitLong = doc.getLong("Limit");

                    int limit =
                            limitLong != null
                                    ? limitLong.intValue()
                                    : 0;

                    Query query = db.collection("AQI")
                            .document("Karachi")
                            .collection("history");

                    // Apply filter only when alert switch is ON
                    if (isOn) {
                        query = query.whereGreaterThan("aqi", limit);
                    }

                    query.get()
                            .addOnSuccessListener(querySnapshot -> {

                                list.clear();

                                for (DocumentSnapshot doc2 : querySnapshot) {
                                    list.add(doc2);
                                }

                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e ->
                                    Log.e("ERROR", "Load failed: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e ->
                        Log.e("ERROR", "Settings failed: " + e.getMessage())
                );
    }

    private void deleteRecord(String docId) {
        db.collection("aqi_alerts").document(docId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadData();
                });
    }
}
