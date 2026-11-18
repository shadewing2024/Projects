package com.smartaqi.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Locale;

public class ManualEnteriesHistories extends AppCompatActivity {



    ListView listManualView;
    TextView txtTime, txtAQI, txtTemp, txtHumidity, txtWind;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ArrayList<DocumentSnapshot> list;
    ArrayAdapter<DocumentSnapshot> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_enteries_histories);

        listManualView = findViewById(R.id.listManualView);

        String docUId = getIntent().getStringExtra("docId");

        list = new ArrayList<>();
        adapter = new ArrayAdapter<DocumentSnapshot>(this, 0, list) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_history, parent, false);
                }

                txtAQI = convertView.findViewById(R.id.txtAQI);
                txtTemp = convertView.findViewById(R.id.txtTemp);
                txtHumidity = convertView.findViewById(R.id.txtHumidity);
                txtWind = convertView.findViewById(R.id.txtWind);
                txtTime = convertView.findViewById(R.id.txtTime);

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

                return convertView;
            }
        };

        listManualView.setAdapter(adapter);

        loadData(docUId);

        // Delete
        listManualView.setOnItemLongClickListener((parent, view, position, id) -> {

            String docId = list.get(position).getId();

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Record?")
                    .setMessage("Are you sure you want to delete this AQI record?")
                    .setPositiveButton("Yes", (d, w) -> deleteRecord(docId, docUId))
                    .setNegativeButton("No", null)
                    .show();

            return true;
        });

    }

    public void loadData(String docID) {

        db.collection("ManualData")
                .document(docID)
                .collection("ManualHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
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

    // ================= DELETE =================
    private void deleteRecord(String docId, String docUId) {

        db.collection("ManualData")
                .document(docUId)
                .collection("ManualHistory")
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadData(docUId);
                });
    }


}