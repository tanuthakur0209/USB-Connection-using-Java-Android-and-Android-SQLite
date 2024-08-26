package com.example.usbdb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

public class AOAP {

    private static final String TAG = "USB_SQLite_DB";

    //vendor ID and product ID for Android
    private static final int ANDROID_VENDOR_ID = 0x18D1;
    private static final int ANDROID_PRODUCT_ID_ACCESSORY = 0x2D00;
    private static final int ANDROID_PRODUCT_ID_ACCESSORY_ADB = 0x2D01;
    private static final int ANDROID_PRODUCT_ID_ACCESSORY_AUDIO = 0x2D04;
    private static final int ANDROID_PRODUCT_ID_ACCESSORY_AUDIO_ADB = 0x2D05;

    //control request
    private static final int ACCESSORY_GET_PROTOCOL = 51;
    private static final int ACCESSORY_SEND_STRING = 52;
    private static final int ACCESSORY_START = 53;
    private static final int AOAP_TIMEOUT_MS = 2000;

    //method to determine whether USB device is in AOAP mode.
    public static boolean isDeviceInAOAPMode(UsbDevice device) {

        int vid = device.getVendorId();
        int pid = device.getProductId();
        return vid == ANDROID_VENDOR_ID &&
                (pid == ANDROID_PRODUCT_ID_ACCESSORY ||
                        pid == ANDROID_PRODUCT_ID_ACCESSORY_ADB ||
                        pid == ANDROID_PRODUCT_ID_ACCESSORY_AUDIO ||
                        pid == ANDROID_PRODUCT_ID_ACCESSORY_AUDIO_ADB);
    }

    //checks if protocol version is greater than or equal to 1
    public static boolean isSupported(UsbDeviceConnection conn) {
        return getProtocol(conn) >= 1;
    }

    //sends a control request 51 to USB device to retrieve protocol version.
    public static int getProtocol(UsbDeviceConnection conn) {

        Log.i(TAG, "Send Control Request 51 Get Protocol");

        if (conn == null) {
            Log.e(TAG, "AOAPInterface.getProtocol(): Invalid handle");
            return 0;
        }

        //receive a response of 2 bytes, which are stored in the buffer array
        byte[] buffer = new byte[2];

        int len = conn.controlTransfer(UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_VENDOR, ACCESSORY_GET_PROTOCOL, 0, 0, buffer, 2, AOAP_TIMEOUT_MS);

        if (len != 2) {
            Log.e(TAG, "Error occur during control transfer");
        }

        //combines two bytes in buffer to create a 16-bit value representing the protocol version.
        //shifting second byte of buffer 8 bits to left
        return (buffer[1] << 8) | buffer[0];
    }


    //method to send a string to a USB device using control transfer
    public static void sendString(UsbDeviceConnection conn, int index, String string) {

        //string is converted to byte array and appended with null terminator (\0).
        byte[] buffer = (string + "\0").getBytes();

        //send control request to USB device
        int len = conn.controlTransfer(UsbConstants.USB_DIR_OUT | UsbConstants.USB_TYPE_VENDOR, ACCESSORY_SEND_STRING, 0, index, buffer, buffer.length, AOAP_TIMEOUT_MS);

        if (len != buffer.length) {
            Log.e(TAG, "Failed to send string");
        }
        else {
            Log.i(TAG, "String send");
        }
    }


    public static void sendAOAPStart(UsbDeviceConnection conn) {
        int len = conn.controlTransfer(UsbConstants.USB_DIR_OUT | UsbConstants.USB_TYPE_VENDOR, ACCESSORY_START, 0, 0, null, 0, AOAP_TIMEOUT_MS);

        if (len < 0) {
            Log.e(TAG, "Control transfer for accessory start failed: " + len);
        }
    }
}
