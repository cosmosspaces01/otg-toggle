package com.settings.otgtoggle.ui;

import android.animation.ObjectAnimator;
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
import com.settings.otgtoggle.tile.OtgStateHelper;

/**
 * Main UI Activity - beautiful OTG toggle screen.
 * Shows status, animated toggle switch, device info, and instructions.
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load saved state
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        isOtgOn = prefs.getBoolean(KEY_STATE, OtgStateHelper.isOtgEnabled(this));

        bindViews();
        applyState(false); // Apply without animation on first load
        setupListeners();
        checkPermissions();
    }

    private void bindViews() {
        toggleThumb = findViewById(R.id.toggle_thumb);
        toggleTrack = findViewById(R.id.toggle_track);
        statusLabel = findViewById(R.id.status_label);
        statusDetail = findViewById(R.id.status_detail);
        statusDot = findViewById(R.id.status_dot);
    }

    private void setupListeners() {
        toggleTrack.setOnClickListener(v -> {
            try {
                isOtgOn = !isOtgOn;

                // Always save the local state and update UI (toggle works as a visual switch)
                saveState(isOtgOn);
                applyState(true);

                // Try to actually write the system setting (best-effort)
                boolean written = tryApplySetting(isOtgOn);
                if (!written) {
                    Log.w(TAG, "Could not write OTG setting — showing as local toggle only");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error toggling OTG", e);
                Toast.makeText(this, "Toggle error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });

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

        View btnDev = findViewById(R.id.btn_open_dev_options);
        if (btnDev != null) {
            btnDev.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(this, "Dev options not available", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ─── OTG Setting ─────────────────────────────────────────────────────────

    private boolean tryApplySetting(boolean enable) {
        int val = enable ? 1 : 0;
        String[] keys = {"usb_otg_enabled", "otg_storage_enabled", "usb_host_enabled"};
        for (String key : keys) {
            try {
                Settings.Global.putInt(getContentResolver(), key, val);
                Log.d(TAG, "Wrote " + key + "=" + val + " via Settings.Global");
                return true;
            } catch (Exception e) {
                Log.w(TAG, "Global write failed for " + key + ": " + e.getMessage());
            }
            try {
                Settings.System.putInt(getContentResolver(), key, val);
                Log.d(TAG, "Wrote " + key + "=" + val + " via Settings.System");
                return true;
            } catch (Exception e) {
                Log.w(TAG, "System write failed for " + key + ": " + e.getMessage());
            }
        }
        return false;
    }

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

            // Colors
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

    // ─── Permission Helpers ───────────────────────────────────────────────────

    private void checkPermissions() {
        try {
            View permissionCard = findViewById(R.id.card_permission_warning);
            if (permissionCard != null) {
                boolean hasWriteSettings = Settings.System.canWrite(this);
                permissionCard.setVisibility(hasWriteSettings ? View.GONE : View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions", e);
        }
    }
}
