package com.thalesgroup.tshpaysample.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;


public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // installSplashScreen() is meant to be called BEFORE super.onCreate()
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        boolean completedOnce = PrerequisitesActivity.havePrerequisitesBeenCompletedOnce(this);

        if (completedOnce) {
            // User has passed setup before. Do a silent check.
            if (PrerequisitesActivity.havePrerequisitesBeenMet(this)) {
                // Everything is still fine, go to main app.
                navigateToMainApp();
            } else {
                // Something is wrong (e.g., NFC was disabled), show the screen again.
                startActivity(new Intent(this, PrerequisitesActivity.class));
            }
        } else {
            // First time launch, must show the prerequisites screen.
            startActivity(new Intent(this, PrerequisitesActivity.class));
        }
        // Finish this activity so it's removed from the back stack.
        finish();
    }

    private void navigateToMainApp() {
        Intent intent = new Intent(this, CardListActivity.class);
        startActivity(intent);
    }
}
