/* This activity get users manually entered data and store in Firestore */

package com.smartaqi.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EnterManually extends AppCompatActivity {

    Button predictBtn;
    TextView predictionTextViewIn;
    EditText tempIn;
    EditText humidityIn;
    EditText windIn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_manually);


            predictBtn = findViewById(R.id.predictBtn);
            predictionTextViewIn = findViewById(R.id.predictionTextViewIn);
            tempIn = findViewById(R.id.tempIn);
            humidityIn = findViewById(R.id.humidityIn);
            windIn = findViewById(R.id.windIn);

            predictBtn.setOnClickListener(v -> {
                prediction();
            });

    }

    private void prediction() {

        String tempStr = tempIn.getText().toString().trim();
        String humStr = humidityIn.getText().toString().trim();
        String windStr = windIn.getText().toString().trim();

        // ✅ Prevent crash
        if (tempStr.isEmpty() || humStr.isEmpty() || windStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT);
            return;
        }

        float getTemp, getHumidity, getWind;

        try {
            getTemp = Float.parseFloat(tempStr);
            getHumidity = Float.parseFloat(humStr);
            getWind = Float.parseFloat(windStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter only numbers", Toast.LENGTH_SHORT).show();
            return;
        }



        AiModel aiModel = new AiModel(this);

        // 🔥 AI INPUT (NO Kelvin conversion)
        float[] input = new float[]{
                getTemp,
                getHumidity,
                getWind
        };

        float[] result = aiModel.predict(input);

        if (result != null && result.length > 0) {

            float predictedAQI = result[0];
            int intAQI = (int) predictedAQI;
            String strAQI = String.valueOf(intAQI);

            predictionTextViewIn.setText(strAQI);

            // ✅ Get Firebase UID
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;

            if (uid != null) {
                saveToFirestore(uid, predictedAQI, input[0], input[1], input[2]);
            }

            Log.d("AQI", "Predicted: " + predictedAQI);

        } else {
            Log.e("AI", "No result");
        }
    }


    private void saveToFirestore(String uid, float aqi, float temp, float humidity, float wind) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ✅ Correct check
        if (uid == null) {
            Log.e("FIRESTORE", "User not logged in");
            return;
        }

        long timestamp = System.currentTimeMillis();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        Map<String, Object> data = new HashMap<>();
        data.put("aqi", aqi);
        data.put("temp", temp);
        data.put("humidity", humidity);
        data.put("wind", wind);
        data.put("timestamp", timestamp); // fixed typo
        data.put("formattedTime", formattedDateTime);

        db.collection("ManualData")
                .document(uid)
                .collection("ManualHistory")
                .add(data)
                .addOnSuccessListener(doc -> Log.d("FIRESTORE", "Saved"))
                .addOnFailureListener(e -> Log.e("FIRESTORE", e.getMessage()));
    }
}