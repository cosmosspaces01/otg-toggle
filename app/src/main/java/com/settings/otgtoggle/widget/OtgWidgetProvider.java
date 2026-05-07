package com.settings.otgtoggle.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

import com.settings.otgtoggle.R;
import com.settings.otgtoggle.tile.OtgSettingsHelper;

/**
 * Home Screen Widget for OTG Toggle.
 * Tapping the widget toggles OTG on/off.
 */
public class OtgWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "OtgWidget";
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

    private void toggleOtg(Context context) {
        try {
            boolean current = OtgSettingsHelper.isOtgEnabled(context);
            boolean newState = !current;

            // Try to write the system setting
            boolean written = OtgSettingsHelper.setOtgEnabled(context, newState);

            if (written) {
                // Save locally for widget display
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_OTG_STATE, newState).apply();
            }

            // Refresh all widgets
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, OtgWidgetProvider.class));
            for (int id : ids) {
                updateWidget(context, manager, id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling OTG from widget", e);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        try {
            boolean isOn = OtgSettingsHelper.isOtgEnabled(context);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_otg);

            // Update icon and text based on state
            views.setImageViewResource(R.id.widget_toggle_icon,
                isOn ? R.drawable.ic_toggle_on : R.drawable.ic_toggle_off);
            views.setTextViewText(R.id.widget_status_text, isOn ? "OTG ON" : "OTG OFF");
            views.setImageViewResource(R.id.widget_status_indicator,
                isOn ? R.drawable.bg_status_on : R.drawable.bg_status_off);

            // Click to toggle
            Intent toggleIntent = new Intent(context, OtgWidgetProvider.class);
            toggleIntent.setAction(ACTION_TOGGLE);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pi);

            manager.updateAppWidget(widgetId, views);
        } catch (Exception e) {
            Log.e(TAG, "Error updating widget", e);
        }
    }
}
