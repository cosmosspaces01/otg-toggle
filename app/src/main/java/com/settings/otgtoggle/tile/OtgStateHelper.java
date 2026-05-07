package com.settings.otgtoggle.tile;

import android.content.Context;
import android.hardware.usb.UsbManager;

/**
 * Helper to detect OTG / USB-host state without requiring root.
 * Uses UsbManager to check for connected USB accessories/devices.
 */
public class OtgStateHelper {

    public static boolean isOtgEnabled(Context context) {
        try {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            if (usbManager == null) return false;
            // If there are any connected USB devices in host mode, OTG is likely on
            return !usbManager.getDeviceList().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
