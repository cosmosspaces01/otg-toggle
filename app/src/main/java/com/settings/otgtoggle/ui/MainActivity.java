package com.settings.otgtoggle.ui;

import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.settings.otgtoggle.R;
import com.settings.otgtoggle.tile.OtgSettingsHelper;
import com.settings.otgtoggle.tile.OtgStateHelper;

/**
 * Main UI Activity - OTG toggle screen.
 * Shows status, animated toggle switch, device info, and setup instructions.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OTGToggle";
    private static final String PREFS = "OtgWidgetPrefs";
    private static final String KEY_STATE = "otg_state";

    private boolean isOtgOn = false;
    private View toggleThumb;
    private MaterialCardView toggleTrack;
    private TextView statusLabel;
    private TextView statusDetail;
    private View statusDot;
    private TextView permissionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load saved state
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        isOtgOn = prefs.getBoolean(KEY_STATE, OtgStateHelper.isOtgEnabled(this));

        bindViews();
        applyState(false);
        setupListeners();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions(); // Re-check when user returns from settings
    }

    private void bindViews() {
        toggleThumb = findViewById(R.id.toggle_thumb);
        toggleTrack = findViewById(R.id.toggle_track);
        statusLabel = findViewById(R.id.status_label);
        statusDetail = findViewById(R.id.status_detail);
        statusDot = findViewById(R.id.status_dot);
        permissionStatus = findViewById(R.id.permission_status_text);
    }

    private void setupListeners() {
        toggleTrack.setOnClickListener(v -> {
            try {
                boolean newState = !isOtgOn;

                // Attempt to write the system setting
                boolean written = OtgSettingsHelper.setOtgEnabled(this, newState);

                if (written) {
                    isOtgOn = newState;
                    saveState(isOtgOn);
                    applyState(true);
                    Toast.makeText(this,
                        isOtgOn ? "OTG Enabled ✓" : "OTG Disabled ✓",
                        Toast.LENGTH_SHORT).show();
                } else {
                    // Cannot write — show setup instructions
                    Toast.makeText(this,
                        "Cannot toggle OTG. Grant permission via ADB first (see below ↓)",
                        Toast.LENGTH_LONG).show();
                    // Scroll to the permission card
                    View card = findViewById(R.id.card_adb_setup);
                    if (card != null) card.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error toggling OTG", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Copy ADB command to clipboard
        View btnCopyAdb = findViewById(R.id.btn_copy_adb);
        if (btnCopyAdb != null) {
            btnCopyAdb.setOnClickListener(v -> {
                String cmd = "adb shell pm grant com.settings.otgtoggle android.permission.WRITE_SECURE_SETTINGS";
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", cmd));
                Toast.makeText(this, "ADB command copied to clipboard ✓", Toast.LENGTH_SHORT).show();
            });
        }

        // Open system OTG settings directly
        View btnOpenOtg = findViewById(R.id.btn_open_otg_settings);
        if (btnOpenOtg != null) {
            btnOpenOtg.setOnClickListener(v -> openOtgSettings());
        }

        // Grant permission button
        View btnGrant = findViewById(R.id.btn_grant_permission);
        if (btnGrant != null) {
            btnGrant.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Cannot open WRITE_SETTINGS screen", e);
                }
            });
        }
    }

    // ─── Open device OTG settings ────────────────────────────────────────────

    private void openOtgSettings() {
        // Try manufacturer-specific OTG settings intents
        String[][] settingsTargets = {
            // Vivo / iQOO
            {"com.android.settings", "com.android.settings.OtgSettings"},
            {"com.android.settings", "com.vivo.settings.OtgSettings"},
            // Samsung
            {"com.android.settings", "com.samsung.android.settings.usb.UsbSettingsActivity"},
            // Xiaomi
            {"com.android.settings", "com.android.settings.OtgSettingsActivity"},
            // Generic
            {"com.android.settings", "com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment"},
        };

        for (String[] target : settingsTargets) {
            try {
                Intent intent = new Intent();
                intent.setClassName(target[0], target[1]);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            } catch (Exception ignored) {}
        }

        // Fallback: open general connected devices settings
        try {
            Intent intent = new Intent("android.settings.USB_SETTINGS");
            startActivity(intent);
            return;
        } catch (Exception ignored) {}

        // Last resort: open main settings
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open Settings", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── State ───────────────────────────────────────────────────────────────

    private void saveState(boolean on) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit().putBoolean(KEY_STATE, on).apply();
    }

    // ─── Animated State Application ──────────────────────────────────────────

    private void applyState(boolean animate) {
        try {
            float targetX = isOtgOn
                ? getResources().getDimension(R.dimen.toggle_thumb_on_x)
                : getResources().getDimension(R.dimen.toggle_thumb_off_x);

            if (animate && toggleThumb != null) {
                ObjectAnimator thumbAnim = ObjectAnimator.ofFloat(
                    toggleThumb, "translationX",
                    toggleThumb.getTranslationX(), targetX);
                thumbAnim.setDuration(300);
                thumbAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                thumbAnim.start();

                if (statusDot != null) {
                    ObjectAnimator pulse = ObjectAnimator.ofFloat(statusDot, "scaleX", 1f, 1.4f, 1f);
                    pulse.setDuration(400);
                    pulse.start();
                    ObjectAnimator pulseY = ObjectAnimator.ofFloat(statusDot, "scaleY", 1f, 1.4f, 1f);
                    pulseY.setDuration(400);
                    pulseY.start();
                }
            } else if (toggleThumb != null) {
                toggleThumb.setTranslationX(targetX);
            }

            if (toggleTrack != null) {
                int trackColor = isOtgOn
                    ? ContextCompat.getColor(this, R.color.toggle_on)
                    : ContextCompat.getColor(this, R.color.toggle_off);
                toggleTrack.setCardBackgroundColor(trackColor);
            }

            if (statusDot != null) {
                int dotColor = isOtgOn
                    ? ContextCompat.getColor(this, R.color.status_on)
                    : ContextCompat.getColor(this, R.color.status_off);
                statusDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(dotColor));
            }

            if (statusLabel != null) {
                statusLabel.setText(isOtgOn ? "OTG Active" : "OTG Inactive");
            }
            if (statusDetail != null) {
                statusDetail.setText(isOtgOn
                    ? "USB On-The-Go is enabled. Connect USB devices."
                    : "USB On-The-Go is disabled. Tap the toggle to enable.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI state", e);
        }
    }

    // ─── Permission Check ────────────────────────────────────────────────────

    private void checkPermissions() {
        try {
            boolean hasSecureWrite = OtgSettingsHelper.hasWriteSecureSettings(this);
            boolean hasWriteSettings = Settings.System.canWrite(this);

            View adbCard = findViewById(R.id.card_adb_setup);
            View permCard = findViewById(R.id.card_permission_warning);

            if (permissionStatus != null) {
                if (hasSecureWrite) {
                    permissionStatus.setText("✅ WRITE_SECURE_SETTINGS granted — full OTG control");
                    permissionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_on));
                } else if (hasWriteSettings) {
                    permissionStatus.setText("⚠️ WRITE_SETTINGS granted, but need WRITE_SECURE_SETTINGS for OTG");
                    permissionStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_amber));
                } else {
                    permissionStatus.setText("❌ No write permissions — OTG toggle will not work");
                    permissionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_off));
                }
            }

            // Show/hide setup cards based on permission state
            if (adbCard != null) {
                adbCard.setVisibility(hasSecureWrite ? View.GONE : View.VISIBLE);
            }
            if (permCard != null) {
                permCard.setVisibility(hasWriteSettings ? View.GONE : View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions", e);
        }
    }
}
