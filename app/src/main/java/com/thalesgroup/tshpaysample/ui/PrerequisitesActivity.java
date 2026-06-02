package com.thalesgroup.tshpaysample.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;

import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.helpers.HceHelper;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.util.HashMap;
import java.util.Map;

public class PrerequisitesActivity extends AppCompatActivity {

    private static final String TAG = PrerequisitesActivity.class.getSimpleName();

    public static final String PREFS_NAME = "AppPrefs";
    public static final String KEY_PREREQUISITES_ONCE_COMPLETED = "prerequisites_once_completed";

    private enum Check {
        NFC_HCE_CAPABILITY, NFC_ENABLED, DEFAULT_PAYMENT_APP, SECURE_DEVICE_LOCK, NOTIFICATIONS
    }

    private enum CheckStatus {
        PASSED, WARNING, FAILED
    }

    private final Map<Check, CheckStatus> mCheckResults = new HashMap<>();

    // Launchers for getting results back from system activities
    private final ActivityResultLauncher<Intent> mStartActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // When we return from a settings screen, re-run all checks.
                runAllChecks();
            });

    private final ActivityResultLauncher<String> mRequestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                // When we get a permission result, re-run all checks.
                runAllChecks();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prerequisites);

        findViewById(R.id.btn_continue_to_app).setOnClickListener(v -> onAllChecksPassed());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Run checks every time the activity is resumed, as the user might
        // have changed settings in the background.
        runAllChecks();
    }

    private void runAllChecks() {
        // Run all checks and store their results
        mCheckResults.put(Check.NFC_HCE_CAPABILITY, checkNfcHceCapability(this) ? CheckStatus.PASSED : CheckStatus.FAILED);
        mCheckResults.put(Check.NFC_ENABLED, checkNfcEnabled(this) ? CheckStatus.PASSED : CheckStatus.FAILED);

        // Logic for Default Payment App: PASSED if default, WARNING if not default but foreground allowed, FAILED otherwise
        final boolean isDefault = HceHelper.isHceServiceSetAsDefault(this);
        final boolean isForegroundAllowed = HceHelper.isForegroundPreferenceAllowed(this);
        if (isDefault) {
            mCheckResults.put(Check.DEFAULT_PAYMENT_APP, CheckStatus.PASSED);
        } else if (isForegroundAllowed) {
            mCheckResults.put(Check.DEFAULT_PAYMENT_APP, CheckStatus.WARNING);
        } else {
            mCheckResults.put(Check.DEFAULT_PAYMENT_APP, CheckStatus.FAILED);
        }

        mCheckResults.put(Check.SECURE_DEVICE_LOCK, checkDeviceSecureLock(this) ? CheckStatus.PASSED : CheckStatus.FAILED);
        mCheckResults.put(Check.NOTIFICATIONS, checkNotificationsPermission(this) ? CheckStatus.PASSED : CheckStatus.FAILED);

        // Update the UI for each check based on its result
        updateCheckUI(findViewById(R.id.check_nfc_hce_capability), Check.NFC_HCE_CAPABILITY, "NFC & HCE capable", "Device must be equipped with HW & SW support for NFC payments.", null, null);
        updateCheckUI(findViewById(R.id.check_nfc_enabled), Check.NFC_ENABLED, "NFC enabled", "NFC must be turned on to pay.", "Enable", v -> openSettings(Settings.ACTION_NFC_SETTINGS));


        if (mCheckResults.get(Check.DEFAULT_PAYMENT_APP) == CheckStatus.WARNING) {
            updateCheckUI(findViewById(R.id.check_default_payment_app), Check.DEFAULT_PAYMENT_APP, "App allowed to use NFC", "The app is NOT set as default. While it will work when open, we recommend setting it as default.", "Set Up", v -> setAsTapAndPayDefault());
        } else {
            updateCheckUI(findViewById(R.id.check_default_payment_app), Check.DEFAULT_PAYMENT_APP, "App allowed to use NFC", "The app should be set as the default Tap & Pay app for the best experience.", "Set Up", v -> openSettings(Settings.ACTION_NFC_PAYMENT_SETTINGS));
        }


        updateCheckUI(findViewById(R.id.check_biometric_setup), Check.SECURE_DEVICE_LOCK, "Secure device lock enabled", "A secure device unlock method is required to protect your payments.", "Set Up", v -> configureSecureLock());
        updateCheckUI(findViewById(R.id.check_notification_permission), Check.NOTIFICATIONS, "Notifications allowed", "Allow notifications to receive important updates about the enrolled cards.", "Allow", v -> mRequestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS));

        evaluateOverallStatus();
    }

    private void updateCheckUI(View checkView, Check checkType, String title, String description, String actionText, View.OnClickListener action) {
        TextView tvTitle = checkView.findViewById(R.id.tv_check_title);
        TextView tvDescription = checkView.findViewById(R.id.tv_check_description);
        ImageView ivStatus = checkView.findViewById(R.id.iv_status_icon);
        Button btnAction = checkView.findViewById(R.id.btn_action);

        tvTitle.setText(title);
        tvDescription.setText(description);

        CheckStatus status = mCheckResults.getOrDefault(checkType, CheckStatus.FAILED);

        int iconRes;
        if (status == CheckStatus.PASSED) {
            iconRes = R.drawable.ic_baseline_check_circle_24;
        } else if (status == CheckStatus.WARNING) {
            iconRes = R.drawable.baseline_warning_24;
        } else {
            iconRes = R.drawable.ic_baseline_cancel_24;
        }
        ivStatus.setImageResource(iconRes);

        if (status == CheckStatus.PASSED || action == null) {
            btnAction.setVisibility(View.GONE);
        } else {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText(actionText);
            btnAction.setOnClickListener(action);
        }

        final boolean supportsHce = mCheckResults.getOrDefault(Check.NFC_HCE_CAPABILITY, CheckStatus.FAILED) == CheckStatus.PASSED;
        if(!supportsHce && (checkType == Check.NFC_ENABLED || checkType == Check.DEFAULT_PAYMENT_APP)){
            btnAction.setVisibility(View.GONE);
        }

    }

    private void evaluateOverallStatus() {
        // If the critical capability check fails, it's a hard stop.
        if (mCheckResults.getOrDefault(Check.NFC_HCE_CAPABILITY, CheckStatus.FAILED) != CheckStatus.PASSED) {
            Toast.makeText(this, "This device is not compatible.", Toast.LENGTH_LONG).show();
            // You might want to disable all action buttons except for this one row.
            return;
        }

        // Check if all items are PASSED or WARNING (if they are allowed to be WARNING)
        boolean canContinue = mCheckResults.entrySet().stream().allMatch(entry -> {
            CheckStatus status = entry.getValue();
            if (status == CheckStatus.PASSED) return true;
            // Only DEFAULT_PAYMENT_APP is allowed to be in WARNING state to continue
            return status == CheckStatus.WARNING && entry.getKey() == Check.DEFAULT_PAYMENT_APP;
        });

        TextView tvTitle = findViewById(R.id.tv_prerequisites_title);
        if (canContinue) {
            // Check if there are any warnings
            boolean hasWarnings = mCheckResults.containsValue(CheckStatus.WARNING);
            if (hasWarnings) {
                tvTitle.setText(R.string.prerequisites_almost_there);
                tvTitle.setTextColor(ContextCompat.getColor(this, R.color.purple_500)); // Using a color that indicates attention but not success/error
            } else {
                tvTitle.setText(R.string.prerequisites_all_ok);
                tvTitle.setTextColor(ContextCompat.getColor(this, R.color.colorSuccess));
            }
        } else {
            tvTitle.setText(R.string.prerequisites_title);
        }

        findViewById(R.id.btn_continue_to_app).setVisibility(canContinue ? View.VISIBLE : View.GONE);
    }

    private void onAllChecksPassed() {
        // Set flag that user has passed this screen once.
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(KEY_PREREQUISITES_ONCE_COMPLETED, true)
                .apply();

        navigateToMainApp();
    }

    private void navigateToMainApp() {
        Intent intent = new Intent(this, CardListActivity.class);
        intent.putExtra(CardListActivity.EXTRA_START_ENROLLMENT, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Remove this activity from the back stack
    }

    private void openSettings(String action) {
        mStartActivityLauncher.launch(new Intent(action));
    }

    private void configureSecureLock() {
        Intent intent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            intent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
            intent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BiometricManager.Authenticators.BIOMETRIC_STRONG);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // Android 9 & 10 (NOT TESTED)
            intent = new Intent(Settings.ACTION_FINGERPRINT_ENROLL);

        } else { // Below Android 9 (API 28) (NOT TESTED)
            intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        }

        AppLoggerHelper.debug(TAG, "About to launch intent: " + intent);

        // --- Safeguard the Intent ---
        // Check if there is an activity that can handle this intent before launching it.
        if (intent.resolveActivity(getPackageManager()) != null) {
            mStartActivityLauncher.launch(intent);
        } else {
            // Fallback: If the specific intent is not available (highly unlikely for ACTION_SECURITY_SETTINGS),
            // try launching the main settings screen as a last resort.
            Intent fallbackIntent = new Intent(Settings.ACTION_SETTINGS);
            if (fallbackIntent.resolveActivity(getPackageManager()) != null) {
                Toast.makeText(this, "Please, find and set up a screen lock in your device settings.", Toast.LENGTH_LONG).show();
                mStartActivityLauncher.launch(fallbackIntent);
            } else {
                // Very rare case where even the main Settings app cannot be found.
                Toast.makeText(this, "Sorry, could not open device settings.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setAsTapAndPayDefault(){
        Intent intent = HceHelper.buildHceServiceAsDefaultRequestIntent(this);
        mStartActivityLauncher.launch(intent);
    }

    // --- Static methods for SplashActivity ---

    public static boolean havePrerequisitesBeenMet(Context context) {
        final PrerequisitesActivity checker = new PrerequisitesActivity();

        boolean arePrerequisitesMet = true;

        arePrerequisitesMet &= checker.checkNfcHceCapability(context);
        AppLoggerHelper.info(TAG, "Checking NFC HCE Capability: " + arePrerequisitesMet);

        arePrerequisitesMet &= checker.checkNfcEnabled(context);
        AppLoggerHelper.info(TAG, "Checking NFC Enabled: " + arePrerequisitesMet);

        // For silent check, we want it to be fully meeting all criteria (no warnings)
        // so that the user sees the warning if they haven't set the app as default yet.
        arePrerequisitesMet &= HceHelper.isHceServiceSetAsDefault(context);
        AppLoggerHelper.info(TAG, "Checking if HCE Service is default: " + arePrerequisitesMet);

        arePrerequisitesMet &= checker.checkDeviceSecureLock(context);
        AppLoggerHelper.info(TAG, "Checking if device secure lock is enrolled: " + arePrerequisitesMet);

        arePrerequisitesMet &=  checker.checkNotificationsPermission(context);
        AppLoggerHelper.info(TAG, "Checking if notification permission granted: " + arePrerequisitesMet);

        return arePrerequisitesMet;
    }

    public static boolean havePrerequisitesBeenCompletedOnce(Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_PREREQUISITES_ONCE_COMPLETED, false);
    }

    // Private methods for checking used in both silent & visual use cases
    private boolean checkNfcHceCapability(final Context c) { return HceHelper.doesDeviceSupportHCE(c); }
    private boolean checkNfcEnabled(final Context c) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(c);
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }

    private boolean checkDeviceSecureLock(final Context c) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Even though we use Androidx.BiometricManager API the support is limited to Android 11 and above
            // see: https://developer.android.com/reference/androidx/biometric/BiometricManager#canAuthenticate(int)

            BiometricManager biometricManager = BiometricManager.from(c);
            int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;

        } else {

            // For older Android versions (Android 10 and below), the BiometricManager API
            // is less reliable for checking device credentials. We fall back to the
            // KeyguardManager to see if any secure screen lock (PIN, pattern, password) is set.
            // This is a sufficient check for security on these older OS versions.
            android.app.KeyguardManager keyguardManager = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                return keyguardManager.isDeviceSecure();
            }
            return false; // If we can't get the manager, assume it's not secure.

        }
    }

    private boolean checkNotificationsPermission(final Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not required for older versions
    }
}
