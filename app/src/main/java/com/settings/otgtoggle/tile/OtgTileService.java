package com.settings.otgtoggle.tile;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

/**
 * Quick Settings Tile for toggling USB OTG.
 * Uses OtgSettingsHelper for manufacturer-agnostic OTG control.
 */
public class OtgTileService extends TileService {

    private static final String TAG = "OtgTileService";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        try {
            boolean currentState = OtgSettingsHelper.isOtgEnabled(this);
            boolean newState = !currentState;

            if (OtgSettingsHelper.setOtgEnabled(this, newState)) {
                Log.d(TAG, "OTG toggled to: " + newState);
            } else {
                Log.w(TAG, "Could not write OTG setting — opening settings");
                openSettings();
            }
            updateTileState();
        } catch (Exception e) {
            Log.e(TAG, "Error toggling OTG", e);
        }
    }

    private void updateTileState() {
        try {
            Tile tile = getQsTile();
            if (tile == null) return;

            boolean isOn = OtgSettingsHelper.isOtgEnabled(this);
            tile.setState(isOn ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setLabel(isOn ? "OTG On" : "OTG Off");
            tile.updateTile();
        } catch (Exception e) {
            Log.e(TAG, "Error updating tile", e);
        }
    }

    private void openSettings() {
        try {
            android.content.Intent intent = new android.content.Intent(
                android.provider.Settings.ACTION_SETTINGS
            );
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open settings", e);
        }
    }
}
