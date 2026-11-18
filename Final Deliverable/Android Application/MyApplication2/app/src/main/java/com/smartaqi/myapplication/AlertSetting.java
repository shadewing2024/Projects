package com.smartaqi.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AlertSetting extends AppCompatActivity {

    Switch alertSwitch;
    EditText alertLimitEdt;
    Button saveBtn;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_setting);

        alertLimitEdt = findViewById(R.id.alertLimitEdt);
        alertSwitch = findViewById(R.id.alertSwitch);
        saveBtn = findViewById(R.id.saveBtn);

        // Initially disabled
        alertLimitEdt.setEnabled(false);
        alertLimitEdt.setAlpha(0.5f);

        loadSettings();

        // Only UI handling
        alertSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

            alertLimitEdt.setEnabled(isChecked);
            alertLimitEdt.setAlpha(isChecked ? 1f : 0.5f);

        });

        // SAVE BUTTON
        saveBtn.setOnClickListener(v -> {

            boolean isOn = alertSwitch.isChecked();

            alertSetting(isOn);

        });

    }

    private void loadSettings() {

        db.collection("alert_setting")
                .document("settings")
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {

                        // Get values
                        boolean isOn =
                                doc.getBoolean("Switch") != null
                                        ? doc.getBoolean("Switch")
                                        : false;

                        Long limitLong = doc.getLong("Limit");

                        int limit =
                                limitLong != null
                                        ? limitLong.intValue()
                                        : 0;

                        // ✅ Set UI
                        alertSwitch.setChecked(isOn);

                        alertLimitEdt.setText(String.valueOf(limit));

                        alertLimitEdt.setEnabled(isOn);

                        alertLimitEdt.setAlpha(isOn ? 1f : 0.5f);
                    }
                });
    }

    private void alertSetting(boolean isOn) {

        int limit = 0;

        // Only validate when ON
        if (isOn) {

            String getLimit = alertLimitEdt
                    .getText()
                    .toString()
                    .trim();

            if (getLimit.isEmpty()) {

                Toast.makeText(this,
                        "Enter alert limit",
                        Toast.LENGTH_SHORT).show();

                return;
            }

            limit = Integer.parseInt(getLimit);
        }

        Map<String, Object> sets = new HashMap<>();

        sets.put("Switch", isOn);
        sets.put("Limit", limit);

        db.collection("alert_setting")
                .document("settings")
                .set(sets, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this,
                            "Settings Saved",
                            Toast.LENGTH_SHORT).show();

                });
    }
}