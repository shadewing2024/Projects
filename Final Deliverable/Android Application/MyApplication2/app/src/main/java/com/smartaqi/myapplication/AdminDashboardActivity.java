/* This activity admin home page */

package com.smartaqi.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;

public class AdminDashboardActivity extends AppCompatActivity {

    Button btnAQIHistory, btnThreshold, btnUsers, btnAlerts, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // 🔗 Bind UI
        btnAQIHistory = findViewById(R.id.btnAQIHistory);
        btnThreshold = findViewById(R.id.btnThreshold);
        btnUsers = findViewById(R.id.btnUsers);
        btnAlerts = findViewById(R.id.btnAlerts);
        btnLogout = findViewById(R.id.btnLogout);

        // 📊 AQI History
        btnAQIHistory.setOnClickListener(v ->
                openActivity(AQIHistoryActivity.class)
        );

        // ⚙️ Threshold Settings
        btnThreshold.setOnClickListener(v ->
                openActivity(AdminAQIHandler.class)
        );

        // 👥 User Logs (optional feature)
        btnUsers.setOnClickListener(v -> {
            Toast.makeText(this, "User logs coming soon", Toast.LENGTH_SHORT).show();
             openActivity(UserLogsActivity.class);
        });

        // 🚨 ALERT SYSTEM (IMPORTANT)
        btnAlerts.setOnClickListener(v ->
                openActivity(NewAdminAlertLogs.class) // 👈 FIXED (your real history screen)
        );

        // 🚪 Logout
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    // 🔥 Cleaner navigation method
    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
    }

    // 🚪 Logout
    private void logoutUser() {

        LogHelper.log("logout", "User logged out", 0);

        AuthUI.getInstance()
                .signOut(this)
                .addOnSuccessListener(unused -> {
                    startActivity(new Intent(this, FirebaseUIActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Logout failed", Toast.LENGTH_SHORT).show()
                );
    }
}