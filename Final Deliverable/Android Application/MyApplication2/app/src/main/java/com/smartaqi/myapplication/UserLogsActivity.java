package com.smartaqi.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;

public class UserLogsActivity extends AppCompatActivity {

    private ListView userList;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ArrayList<DocumentSnapshot> list;
    private ArrayAdapter<DocumentSnapshot> adapter;

    // Listener variable to manage real-time updates
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_logs);

        // Initialize UI
        userList = findViewById(R.id.userList);

        list = new ArrayList<>();

        // Custom Adapter with fixed view references
        adapter = new ArrayAdapter<DocumentSnapshot>(this, 0, list) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.user_list_item, parent, false);
                }

                // Local references to prevent data mixing during scroll
                TextView itemStatus = convertView.findViewById(R.id.txtStatus);
                TextView itemName = convertView.findViewById(R.id.txtName);
                TextView itemLocation = convertView.findViewById(R.id.txtLocation);
                TextView itemEmail = convertView.findViewById(R.id.txtEmail);

                DocumentSnapshot doc = list.get(position);

                String uName = doc.getString("Name") != null ? doc.getString("Name") : "N/A";
                String uLocation = doc.getString("Location") != null ? doc.getString("Location") : "N/A";
                String uEmail = doc.getString("Email") != null ? doc.getString("Email") : "N/A";
                boolean uStatus = doc.getBoolean("Status") != null && doc.getBoolean("Status");
                String uStatusColor = doc.getString("Status_color") != null ? doc.getString("Status_color") : "RED";

                itemName.setText(uName);
                itemLocation.setText(uLocation);
                itemEmail.setText(uEmail);

                if (uStatus) {
                    itemStatus.setText("Online");
                    itemStatus.setTextColor(Color.GREEN);
                } else {
                    itemStatus.setText("Offline");
                    itemStatus.setTextColor(Color.RED);
                }

                return convertView;
            }
        };

        userList.setAdapter(adapter);

        // Start Real-time Sync
        startSyncingUsers();

        // Click listener for details
        userList.setOnItemClickListener((parent, view, position, id) -> {
            String docId = list.get(position).getId();
            hiddenDetail(docId);
        });
    }

    /**
     * Set up SnapshotListener for real-time syncing with Firestore
     */
    private void startSyncingUsers() {
        userListener = db.collection("users")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIRESTORE_SYNC", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        list.clear();
                        list.addAll(value.getDocuments());
                        adapter.notifyDataSetChanged();

                    }
                });
    }

    private void hiddenDetail(String docId) {
        db.collection("users")
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String uTime = doc.getString("Time") != null ? doc.getString("Time") : "N/A";
                        String uID = doc.getString("ID") != null ? doc.getString("ID") : "N/A";
                        String uEmail = doc.getString("Email") != null ? doc.getString("Email") : "N/A";
                        String uName = doc.getString("Name") != null ? doc.getString("Name") : "N/A";
                        String uPhone = doc.getString("Phone") != null ? doc.getString("Phone") : "N/A";
                        String uLocation = doc.getString("Location") != null ? doc.getString("Location") : "N/A";
                        boolean uStatus = doc.getBoolean("Status") != null && doc.getBoolean("Status");
                        String uLogin_Time = doc.getString("Login_Time") != null ? doc.getString("Login_Time") : "N/A";
                        String uLogout_Time = doc.getString("Logout_Time") != null ? doc.getString("Logout_Time") : "N/A";

                        showDetail(uTime, uID, uEmail, uName, uPhone, uLocation, uStatus, uLogin_Time, uLogout_Time);
                    }
                });
    }

    private void showDetail(String uTime, String uID, String uEmail, String uName, String uPhone, String uLocation, boolean uStatus, String uLogin_Time, String uLogout_Time) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // Setup common text appearance
        int textSize = 16;

        // Populate Layout
        layout.addView(createTextView("ID: " + uID, textSize, Color.BLACK));
        layout.addView(createTextView("Registered: " + uTime, textSize, Color.GRAY));
        layout.addView(createDivider());

        TextView statusTxt = createTextView(uStatus ? "Status: Online" : "Status: Offline", textSize, uStatus ? Color.GREEN : Color.RED);
        layout.addView(statusTxt);
        layout.addView(createTextView("Login: " + uLogin_Time, textSize, Color.BLACK));
        layout.addView(createTextView("Logout: " + uLogout_Time, textSize, Color.BLACK));
        layout.addView(createDivider());

        layout.addView(createTextView("Name: " + uName, textSize, Color.BLACK));
        layout.addView(createTextView("Phone: " + uPhone, textSize, Color.BLACK));
        layout.addView(createTextView("Email: " + uEmail, textSize, Color.BLACK));
        layout.addView(createTextView("Location: " + uLocation, textSize, Color.BLACK));

        Button manualBtn = new Button(this);
        manualBtn.setText("View Manual History");
        manualBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, ManualEnteriesHistories.class);
            i.putExtra("docId", uID);
            startActivity(i);
        });
        layout.addView(manualBtn);

        Button feedbckBtn = new Button(this);
        feedbckBtn.setText("View Feedbck");
        feedbckBtn.setOnClickListener( v -> {
            Intent i = new Intent(this, FeedbckHistories.class);
            i.putExtra("docId", uID);
            startActivity(i);
        });
        layout.addView(feedbckBtn);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("User Details")
                .setView(layout)
                .setPositiveButton("Close", null)
                .create();

        dialog.show();
        dialog.getWindow().setBackgroundDrawableResource(
                R.drawable.dialog_white_bg
        );
    }

    // Helper to create TextViews quickly
    private TextView createTextView(String text, int size, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);
        tv.setPadding(0, 5, 0, 5);
        return tv;
    }

    private View createDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        params.setMargins(0, 15, 0, 15);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.LTGRAY);
        return divider;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Crucial: Stop the listener to avoid memory leaks
        if (userListener != null) {
            userListener.remove();
        }
    }
}