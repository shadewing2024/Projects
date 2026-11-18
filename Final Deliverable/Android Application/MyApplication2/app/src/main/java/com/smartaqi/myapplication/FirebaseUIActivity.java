/*  This is a Firebase Authentication UI (Signup/Login) page */

package com.smartaqi.myapplication;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

public class FirebaseUIActivity extends AppCompatActivity {

    // 🔹 Official launcher (from Firebase docs)
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(
                    new FirebaseAuthUIActivityResultContract(),
                    new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                        @Override
                        public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                            onSignInResult(result);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔥 Start FirebaseUI directly
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 1. Check if the user is already logged in (even if offline)
        if (currentUser != null) {
            navigateToNextScreen(currentUser);
            return;
        }

        // 2. If not logged in and NO internet, go to MainActivity (Offline Mode)
        if (!isInternetAvailable()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // 3. Otherwise, show the sign-in UI
        createSignInIntent();
    }

    private void createSignInIntent() {

        // 🔹 Only required providers (as per your requirement)
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        // 🔹 Create intent (OFFICIAL METHOD)
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setCredentialManagerEnabled(false) // 👈 ADD THIS LINE
                .build();

        // 🔹 Launch FirebaseUI
        signInLauncher.launch(signInIntent);
    }

    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void navigateToNextScreen(FirebaseUser user) {
        boolean isAdmin = user != null &&
                user.getEmail() != null &&
                user.getEmail().equals("bc220402268mti@vu.edu.pk");

        if (isAdmin) {
            LogHelper.log("login", "Admin logged in", 0);
            startActivity(new Intent(this, AdminDashboardActivity.class));
        } else {
            LogHelper.log("login", "Citizen logged in", 0);
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }


    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        if (result.getResultCode() == RESULT_OK) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            navigateToNextScreen(user);
        } else {
            // If sign-in failed (user pressed back or error),
            // and we are offline, we can still try to go to MainActivity
            if (!isInternetAvailable()) {
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        }
    }
}