/* Profile Page */
package com.smartaqi.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Profile extends AppCompatActivity {

    LinearLayout editProfileBtn, contactBtn, helpBtn, logoutBtn, levelBtn;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        editProfileBtn = findViewById(R.id.editProfileBtn);
        contactBtn = findViewById(R.id.contactBtn);
        helpBtn = findViewById(R.id.helpBtn);
        levelBtn = findViewById(R.id.levelBtn);
        logoutBtn = findViewById(R.id.logoutBtn);

        editProfileBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, CitienProfile.class));
        });

        contactBtn.setOnClickListener(v -> {
            contactBox();
        });

        helpBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, HelpSupportActivity.class));
        });

        levelBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, AQILevelForCitizens.class));
        });

        logoutBtn.setOnClickListener(v -> {
            logoutUser();
        });

    }

    // ==========================================
    // CONTACT BOX
    // ==========================================
    private void contactBox() {

        View view = getLayoutInflater()
                .inflate(R.layout.dialog_support, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        view.findViewById(R.id.emailBtn)
                .setOnClickListener(v -> {

                    Intent emailIntent =
                            new Intent(Intent.ACTION_SEND);

                    emailIntent.setType("message/rfc822");

                    emailIntent.putExtra(
                            Intent.EXTRA_EMAIL,
                            new String[]{"airpridiction@gmail.com"}
                    );

                    emailIntent.putExtra(
                            Intent.EXTRA_SUBJECT,
                            "AQI App Support"
                    );

                    startActivity(
                            Intent.createChooser(
                                    emailIntent,
                                    "Send Email"
                            )
                    );
                });

        view.findViewById(R.id.whatsappBtn)
                .setOnClickListener(v -> {

                    String phone = "923333212031";

                    String url =
                            "https://wa.me/" +
                                    phone +
                                    "?text=I need help";

                    Intent i =
                            new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                            );

                    try {

                        startActivity(i);

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "WhatsApp not installed",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

        dialog.show();
    }

    // ==========================================
    // LOGOUT
    // ==========================================
    private void logoutUser() {

        if (user == null) return;

        Map<String, Object> offline = new HashMap<>();

        offline.put("Status", false);

        offline.put("Status_color", "RED");

        db.collection("users")
                .document(user.getUid())
                .update(offline)
                .addOnCompleteListener(t -> {

                    AuthUI.getInstance()
                            .signOut(this)
                            .addOnCompleteListener(task -> {

                                startActivity(
                                        new Intent(
                                                this,
                                                FirebaseUIActivity.class
                                        )
                                );

                                finish();
                            });
                });
    }
}
