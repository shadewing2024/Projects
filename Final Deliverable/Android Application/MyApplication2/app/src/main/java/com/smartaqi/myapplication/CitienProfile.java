package com.smartaqi.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CitienProfile extends AppCompatActivity {

    EditText editName;
    EditText editPhone;
    EditText editLocation;
    TextView userEmail;
    Button saveBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_citien_profile);

        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);
        editLocation = findViewById(R.id.editLocation);
        userEmail = findViewById(R.id.userEmail);
        saveBtn = findViewById(R.id.saveBtn);

        readUserData();

        saveBtn.setOnClickListener(v -> {
            saveUserData();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

    }

    private void readUserData() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return;
        }

        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String name = document.getString("Name");
                        String phone = document.getString("Phone");
                        String location = document.getString("Location");
                        String email = user.getEmail();

                        // ✅ Null safety
                        if (name == null) name = "N/A";
                        if (phone == null) phone = "N/A";
                        if (location == null) location = "N/A";
                        if (email == null) email = "N/A";

                        // ✅ Correct setText
                        editName.setText("Name: " + name);
                        editPhone.setText("Phone: " + phone);
                        editLocation.setText("Location: " + location);
                        userEmail.setText("Email: " + email);

                    } else {
                        Toast.makeText(this,"User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,"Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserData() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (user == null) {
            System.out.println("User not logged in");
            return;
        }

        String uid = user.getUid();

        String getName = editName.getText().toString();
        String getPhone = editPhone.getText().toString();
        String getLocation = editLocation.getText().toString();

        Map<String, Object> userInfo = new HashMap<>();

        if (!getName.isEmpty()) {
            userInfo.put("Name", getName);
        }

        if (!getPhone.isEmpty()) {
            userInfo.put("Phone", getPhone);
        }

        if (!getLocation.isEmpty()) {
            userInfo.put("Location", getLocation);
        }

        if (userInfo.isEmpty()) {
            System.out.println("Please enter data");
            return;
        }

        db.collection("users")
                .document(uid)
                .set(userInfo, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Data saved successfully");
                })
                .addOnFailureListener(e -> {
                    System.out.println("Error: " + e.getMessage());
                });
    }
}