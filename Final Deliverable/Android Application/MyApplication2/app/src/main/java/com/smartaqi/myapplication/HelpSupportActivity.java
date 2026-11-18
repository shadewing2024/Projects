/* This activity takes feedback from users */

package com.smartaqi.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class HelpSupportActivity extends AppCompatActivity {

    TextView q1, a1, q2, a2;
    Button feedbackBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        q1 = findViewById(R.id.q1);
        a1 = findViewById(R.id.a1);
        q2 = findViewById(R.id.q2);
        a2 = findViewById(R.id.a2);
        feedbackBtn = findViewById(R.id.feedbackBtn);

        // Toggle FAQ
        q1.setOnClickListener(v -> toggleFAQ(a1));
        q2.setOnClickListener(v -> toggleFAQ(a2));

        // Feedback (Firestore)
        feedbackBtn.setOnClickListener(v -> sendFeedbackDialog());
    }

    // Toggle FAQ
    private void toggleFAQ(TextView answer) {

        if (answer.getVisibility() == View.GONE) {

            answer.setVisibility(View.VISIBLE);
            answer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down));

        } else {

            answer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));

            // delay hiding after animation
            new Handler().postDelayed(() -> answer.setVisibility(View.GONE), 200);
        }
    }

    private void sendFeedbackDialog() {

        // 🔹 Create layout (vertical)
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText userName = new EditText(this);
        userName.setHint("Enter your name");

        EditText userText = new EditText(this);
        userText.setHint("Enter your feedback");

        layout.addView(userName);
        layout.addView(userText);

        new AlertDialog.Builder(this)
                .setTitle("Send Feedback")
                .setView(layout)
                .setPositiveButton("Send", (d, w) -> {

                    String name = userName.getText().toString().trim();
                    String text = userText.getText().toString().trim();

                    // ✅ Proper validation
                    if (name.isEmpty() || text.isEmpty()) {
                        Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    // 🔹 Time
                    String formattedTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(new java.util.Date());

                    Map<String, Object> feedBack = new HashMap<>();
                    feedBack.put("text", text);
                    feedBack.put("time", formattedTime);
                    feedBack.put("millisTime", System.currentTimeMillis());
                    feedBack.put("sender", name);

                    db.collection("Feedback")
                            .document(userId)
                            .collection("messages")
                            .add(feedBack)
                            .addOnSuccessListener(doc ->
                                    Toast.makeText(this, "Feedback sent", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );

                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}