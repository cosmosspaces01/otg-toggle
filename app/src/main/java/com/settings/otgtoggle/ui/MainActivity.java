package com.settings.otgtoggle.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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
            isOtgOn = !isOtgOn;
            if (tryApplySetting(isOtgOn)) {
                saveState(isOtgOn);
                applyState(true);
            } else {
                // Non-root fallback
                showPermissionDialog();
            }
        });

        findViewById(R.id.btn_grant_permission).setOnClickListener(v -> {
            // Open WRITE_SETTINGS permission screen
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        findViewById(R.id.btn_open_dev_options).setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        });
    }

    // ─── OTG Setting ─────────────────────────────────────────────────────────

    private boolean tryApplySetting(boolean enable) {
        int val = enable ? 1 : 0;
        String[] keys = {"usb_otg_enabled", "otg_storage_enabled", "usb_host_enabled"};
        for (String key : keys) {
            try {
                Settings.Global.putInt(getContentResolver(), key, val);
                return true;
            } catch (SecurityException ignored) {}
            try {
                Settings.System.putInt(getContentResolver(), key, val);
                return true;
            } catch (SecurityException ignored) {}
        }
        return false;
    }

    private void saveState(boolean on) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit().putBoolean(KEY_STATE, on).apply();
    }

    // ─── Animated State Application ──────────────────────────────────────────

    private void applyState(boolean animate) {
        float targetX = isOtgOn
            ? getResources().getDimension(R.dimen.toggle_thumb_on_x)
            : getResources().getDimension(R.dimen.toggle_thumb_off_x);

        if (animate) {
            // Slide thumb
            ObjectAnimator thumbAnim = ObjectAnimator.ofFloat(
                toggleThumb, "translationX",
                toggleThumb.getTranslationX(), targetX);
            thumbAnim.setDuration(300);
            thumbAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            thumbAnim.start();

            // Pulse dot
            ObjectAnimator pulse = ObjectAnimator.ofFloat(statusDot, "scaleX", 1f, 1.4f, 1f);
            pulse.setDuration(400);
            pulse.start();
            ObjectAnimator pulseY = ObjectAnimator.ofFloat(statusDot, "scaleY", 1f, 1.4f, 1f);
            pulseY.setDuration(400);
            pulseY.start();
        } else {
            toggleThumb.setTranslationX(targetX);
        }

        // Colors
        int trackColor = isOtgOn
            ? ContextCompat.getColor(this, R.color.toggle_on)
            : ContextCompat.getColor(this, R.color.toggle_off);
        toggleTrack.setCardBackgroundColor(trackColor);

        int dotColor = isOtgOn
            ? ContextCompat.getColor(this, R.color.status_on)
            : ContextCompat.getColor(this, R.color.status_off);
        statusDot.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(dotColor));

        statusLabel.setText(isOtgOn ? "OTG Active" : "OTG Inactive");
        statusDetail.setText(isOtgOn
            ? "USB On-The-Go is enabled. Connect USB devices."
            : "USB On-The-Go is disabled. Tap the toggle to enable.");
    }

    // ─── Permission Helpers ───────────────────────────────────────────────────

    private void checkPermissions() {
        View permissionCard = findViewById(R.id.card_permission_warning);
        boolean hasWriteSettings = Settings.System.canWrite(this);
        permissionCard.setVisibility(hasWriteSettings ? View.GONE : View.VISIBLE);
    }

    private void showPermissionDialog() {
        Toast.makeText(this,
            "Root or WRITE_SECURE_SETTINGS permission required. Tap 'Grant Permission'.",
            Toast.LENGTH_LONG).show();
        isOtgOn = !isOtgOn; // revert
    }
}
