package com.settings.otgtoggle.tile;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Centralized helper to read/write OTG settings across all manufacturers.
 *
 * Supports: Stock Android, Samsung, Vivo/iQOO, Xiaomi, OPPO/Realme, OnePlus.
 *
 * OTG keys vary by manufacturer. This helper tries ALL known keys
 * across Settings.Global, Settings.Secure, and Settings.System.
 */
public class OtgSettingsHelper {

    private static final String TAG = "OtgSettingsHelper";

    // ─── ALL known OTG setting keys across manufacturers ──────────────────────
    //
    //  Different manufacturers store the OTG toggle under different keys:
    //   - Stock/AOSP/Pixel:  "usb_otg_enabled" (Global)
    //   - Samsung OneUI:     "otg_storage_enabled" (Global)
    //   - Vivo/iQOO:         "usb_otg_switch" / "persist.usb.otg" (Global/Secure)
    //   - Xiaomi/MIUI:       "usb_otg_enabled" / "otg_state" (Global)
    //   - OPPO/Realme:       "usb_otg_enabled" (Global)
    //   - OnePlus:           "usb_otg_enabled" (Global)
    //
    private static final String[] OTG_KEYS = {
        "usb_otg_switch",           // Vivo / iQOO (most common for your device)
        "usb_otg_enabled",          // Stock Android / AOSP / Pixel / OnePlus
        "otg_storage_enabled",      // Samsung OneUI
        "usb_host_enabled",         // Some Qualcomm devices
        "otg_state",                // Xiaomi / MIUI
        "otg_enabled",              // Some custom ROMs
        "persist.usb.otg",          // Vivo alternate key
    };

    // ─── Check if we have WRITE_SECURE_SETTINGS ──────────────────────────────

    public static boolean hasWriteSecureSettings(Context context) {
        return context.checkCallingOrSelfPermission(
            "android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
    }

    // ─── Read current OTG state ──────────────────────────────────────────────

    public static boolean isOtgEnabled(Context context) {
        for (String key : OTG_KEYS) {
            // Try Settings.Global
            try {
                int val = Settings.Global.getInt(context.getContentResolver(), key, -1);
                if (val != -1) {
                    Log.d(TAG, "Read OTG state from Global/" + key + " = " + val);
                    return val == 1;
                }
            } catch (Exception ignored) {}

            // Try Settings.Secure
            try {
                int val = Settings.Secure.getInt(context.getContentResolver(), key, -1);
                if (val != -1) {
                    Log.d(TAG, "Read OTG state from Secure/" + key + " = " + val);
                    return val == 1;
                }
            } catch (Exception ignored) {}

            // Try Settings.System
            try {
                int val = Settings.System.getInt(context.getContentResolver(), key, -1);
                if (val != -1) {
                    Log.d(TAG, "Read OTG state from System/" + key + " = " + val);
                    return val == 1;
                }
            } catch (Exception ignored) {}
        }

        // Fallback: check USB host mode via UsbManager
        return OtgStateHelper.isOtgEnabled(context);
    }

    // ─── Write OTG state ─────────────────────────────────────────────────────
    //
    //  Tries every combination of key × namespace until one succeeds.
    //  Returns true if at least one write succeeded.
    //

    public static boolean setOtgEnabled(Context context, boolean enable) {
        int val = enable ? 1 : 0;
        boolean anySuccess = false;

        for (String key : OTG_KEYS) {
            // Try Settings.Global (requires WRITE_SECURE_SETTINGS)
            try {
                boolean result = Settings.Global.putInt(context.getContentResolver(), key, val);
                if (result) {
                    Log.d(TAG, "✓ Wrote Global/" + key + " = " + val);
                    anySuccess = true;
                }
            } catch (Exception e) {
                Log.w(TAG, "✗ Global/" + key + ": " + e.getMessage());
            }

            // Try Settings.Secure (requires WRITE_SECURE_SETTINGS)
            try {
                boolean result = Settings.Secure.putInt(context.getContentResolver(), key, val);
                if (result) {
                    Log.d(TAG, "✓ Wrote Secure/" + key + " = " + val);
                    anySuccess = true;
                }
            } catch (Exception e) {
                Log.w(TAG, "✗ Secure/" + key + ": " + e.getMessage());
            }

            // Try Settings.System (requires WRITE_SETTINGS)
            try {
                boolean result = Settings.System.putInt(context.getContentResolver(), key, val);
                if (result) {
                    Log.d(TAG, "✓ Wrote System/" + key + " = " + val);
                    anySuccess = true;
                }
            } catch (Exception e) {
                Log.w(TAG, "✗ System/" + key + ": " + e.getMessage());
            }

            // If we had any success with this key, stop trying more keys
            if (anySuccess) break;
        }

        return anySuccess;
    }
}
