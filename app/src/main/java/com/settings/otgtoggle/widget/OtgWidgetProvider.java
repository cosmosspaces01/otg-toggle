package com.settings.otgtoggle.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.settings.otgtoggle.R;
import com.settings.otgtoggle.tile.OtgStateHelper;
import com.settings.otgtoggle.tile.OtgTileService;

/**
 * Home Screen Widget for OTG Toggle.
 * Displays a beautiful animated toggle card on the home screen.
 */
public class OtgWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TOGGLE = "com.settings.otgtoggle.TOGGLE_OTG";
    private static final String PREFS_NAME = "OtgWidgetPrefs";
    private static final String KEY_OTG_STATE = "otg_state";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TOGGLE.equals(intent.getAction())) {
            toggleOtg(context);
        }
    }

    // ─── Toggle Logic ────────────────────────────────────────────────────────

    private void toggleOtg(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean current = prefs.getBoolean(KEY_OTG_STATE, OtgStateHelper.isOtgEnabled(context));
        boolean newState = !current;

        // Try to apply the setting
        OtgTileService svc = new OtgTileService();
        // Write via Settings API if possible
        tryWriteSetting(context, newState);

        // Save locally for widget display
        prefs.edit().putBoolean(KEY_OTG_STATE, newState).apply();

        // Refresh all widgets
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, OtgWidgetProvider.class));
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    private void tryWriteSetting(Context context, boolean enable) {
        int val = enable ? 1 : 0;
        String[] keys = {"usb_otg_enabled", "otg_storage_enabled", "usb_host_enabled"};
        for (String key : keys) {
            try {
                android.provider.Settings.Global.putInt(
                    context.getContentResolver(), key, val);
                return;
            } catch (SecurityException ignored) {}
            try {
                android.provider.Settings.System.putInt(
                    context.getContentResolver(), key, val);
                return;
            } catch (SecurityException ignored) {}
        }
    }

    // ─── Widget UI Update ────────────────────────────────────────────────────

    public static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isOn = prefs.getBoolean(KEY_OTG_STATE, OtgStateHelper.isOtgEnabled(context));

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_otg);

        // Update toggle drawable based on state
        views.setImageViewResource(R.id.widget_toggle_icon,
            isOn ? R.drawable.ic_toggle_on : R.drawable.ic_toggle_off);
        views.setTextViewText(R.id.widget_status_text, isOn ? "OTG Enabled" : "OTG Disabled");
        views.setInt(R.id.widget_status_indicator, "setBackgroundResource",
            isOn ? R.drawable.bg_status_on : R.drawable.bg_status_off);

        // Click to toggle
        Intent toggleIntent = new Intent(context, OtgWidgetProvider.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pi);

        manager.updateAppWidget(widgetId, views);
    }
}
