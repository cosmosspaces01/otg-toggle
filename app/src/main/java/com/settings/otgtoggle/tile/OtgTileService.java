package com.settings.otgtoggle.tile;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

/**
 * Quick Settings Tile for toggling USB OTG.
 *
 * On ROOTED devices: directly writes to Settings.Global / Settings.System.
 * On NON-ROOTED devices: opens the Settings panel so user can toggle manually.
 */
public class OtgTileService extends TileService {

    private static final String TAG = "OtgTileService";

    // Known OTG setting keys (varies by manufacturer)
    private static final String[] OTG_KEYS = {
        "usb_otg_enabled",          // Stock Android / AOSP
        "otg_storage_enabled",      // Samsung
        "usb_host_enabled",         // Some Qualcomm devices
        "persist.sys.usb.otg",      // Xiaomi/MIUI via system prop
    };

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean currentState = getOtgState();
        boolean newState = !currentState;

        if (trySetOtgState(newState)) {
            updateTileState();
        } else {
            // Fallback: guide user to open USB settings
            openUsbSettings();
        }
    }

    // ─── State Reading ───────────────────────────────────────────────────────

    public boolean getOtgState() {
        for (String key : OTG_KEYS) {
            try {
                int val = Settings.Global.getInt(getContentResolver(), key, -1);
                if (val != -1) return val == 1;
            } catch (Exception ignored) {}

            try {
                int val = Settings.System.getInt(getContentResolver(), key, -1);
                if (val != -1) return val == 1;
            } catch (Exception ignored) {}
        }
        // Default: assume OTG follows USB connection state
        return OtgStateHelper.isOtgEnabled(this);
    }

    // ─── State Writing ───────────────────────────────────────────────────────

    private boolean trySetOtgState(boolean enable) {
        int val = enable ? 1 : 0;
        for (String key : OTG_KEYS) {
            try {
                Settings.Global.putInt(getContentResolver(), key, val);
                Log.d(TAG, "Set " + key + " = " + val + " via Settings.Global");
                return true;
            } catch (SecurityException e) {
                Log.w(TAG, "No permission for Global key: " + key);
            }

            try {
                Settings.System.putInt(getContentResolver(), key, val);
                Log.d(TAG, "Set " + key + " = " + val + " via Settings.System");
                return true;
            } catch (SecurityException e) {
                Log.w(TAG, "No permission for System key: " + key);
            }
        }
        return false; // Could not write — needs root / system privileges
    }

    // ─── UI Update ───────────────────────────────────────────────────────────

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean isOn = getOtgState();
        tile.setState(isOn ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(isOn ? "OTG On" : "OTG Off");
        tile.updateTile();
    }

    private void openUsbSettings() {
        try {
            android.content.Intent intent = new android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            );
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open USB settings", e);
        }
    }
}
