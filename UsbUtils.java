package com.example.usbdb;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

public class UsbUtils {

    private static final String TAG = "USB_SQLite_DB";

    //vendor ID and product ID for iPhone
    private static final int VENDOR_ID_IPHONE = 1452;
    private static final int PRODUCT_ID_IPHONE = 4776;

    //strings sent by app
    private static final int MANUFACTURER_NAME = 0;
    private static final int MODEL_NAME = 1;
    private static final int DESCRIPTION = 2;
    private static final int VERSION = 3;
    private static final int URI = 4;
    private static final int SERIAL_NUMBER = 5;

    public static String checkDeviceType(Context context, UsbDeviceConnection usbDeviceConnection, UsbDevice device) {

        //checks if connected USB device is iPhone with vendor and product IDs.
        if (device.getVendorId() == VENDOR_ID_IPHONE && device.getProductId() == PRODUCT_ID_IPHONE) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                UsbUtils.showAlertDialog(context, device.getProductName() + " supporting CarPlay session.");
                return "iPhone";
            }
        }

        //if not iPhone, it checks for Android or other device
        else {

            if (AOAP.isDeviceInAOAPMode(device)) {
                Log.i(TAG, "Device in accessory mode");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    UsbUtils.showAlertDialog(context, device.getProductName() + " supporting Android Auto session");
                    return "Android";
                }
            }

            //If neither condition is met, it checks if the device supports AOAP by calling
            else if (AOAP.isSupported(usbDeviceConnection)) {

                //it sends identifying information CT 52 to the device
                String manufacturerName = "Manufacturer";
                String modelName = "Model";
                String description = "Description";
                String version = "1.0";
                String uri = "https://www.android.com/auto";
                String serial = "1234";

                AOAP.sendString(usbDeviceConnection, MANUFACTURER_NAME, manufacturerName);
                AOAP.sendString(usbDeviceConnection, MODEL_NAME, modelName);
                AOAP.sendString(usbDeviceConnection, DESCRIPTION, description);
                AOAP.sendString(usbDeviceConnection, VERSION, version);
                AOAP.sendString(usbDeviceConnection, URI, uri);
                AOAP.sendString(usbDeviceConnection, SERIAL_NUMBER, serial);

                //then initiates the AOAP mode using sendAOAPStart()
                AOAP.sendAOAPStart(usbDeviceConnection);
                Log.i(TAG, "Send Control Request 53 Start Accessory Mode");

                //Finally, it closes the USB device connection
                usbDeviceConnection.close();
            }
            else {
                Toast.makeText(context, "Device does not support accessory mode", Toast.LENGTH_SHORT).show();
                return "Android";
            }
        }
        return "Unknown";
    }

    public static void showAlertDialog(Context context, String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message + " Do you want to continue?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
