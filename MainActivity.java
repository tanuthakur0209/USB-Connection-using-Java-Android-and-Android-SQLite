package com.example.usbdb;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "USB_SQLite_DB";
    private static final String ACTION_USB_PERMISSION = "com.example.usbdb.USB_PERMISSION";
    private ListView deviceListView;
    private ArrayAdapter<String> deviceListAdapter;
    private List<String> deviceList = new ArrayList<>();
    private UsbManager usbManager;
    private PendingIntent permissionIntent;
    private USBDevicesDatabaseHelper usbDevicesDatabaseHelper;
    private String device_type = null;
    private boolean isAttachedDevice = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.activity_main);

        //obtaining instance of UsbManager, that allows us to interact with USB devices connected to Android device.
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbDevicesDatabaseHelper = new USBDevicesDatabaseHelper(this);

        deviceListView = findViewById(R.id.deviceListView);
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        deviceListView.setAdapter(deviceListAdapter);

        //interface definition for callback to be invoked when an item in AdapterView has been clicked
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String productName = deviceList.get(position);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Do you want to remove " + deviceList.get(position) + " from list?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "Device removed from list");

                                UsbDevice selectedDevice = null;
                                List<UsbDevice> usbDevices = usbDevicesDatabaseHelper.getAllUSBDevices(MainActivity.this);

                                for (UsbDevice device : usbDevices) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        if (device.getProductName().equals(productName)) {
                                            selectedDevice = device;
                                            break;
                                        }
                                    }
                                }

                                if (selectedDevice != null) {
                                    usbDevicesDatabaseHelper.deleteUSBDeviceFromDB(MainActivity.this, selectedDevice);
                                    updateDeviceList();
                                    Log.d(TAG, "Device removed from db and list");
                                }
                                else {
                                    Log.e(TAG, "Selected device not found.");
                                }

                            }
                        })

                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
            }
        });

        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        //This filter is used to listen for permission grant result
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        registerReceiver(usbReceiver, filter);
        Log.i(TAG, "usbReceiver registered");
    }


    @Override
    protected void onResume() {
        super.onResume();
        updateDeviceList();
    }


    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //If action 'ACTION_USB_DEVICE_ATTACHED' - means USB device has attached to device.
             if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.i(TAG, "Device Attached");

                if (isAttachedDevice) {
                    Log.i(TAG, "Duplicate device attachment ignored");
                    return;
                }

                isAttachedDevice = true;

                //USB device attached, update the device list
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (device != null) {
                    checkdeviceinDB(device);
                }
            }

            //If action 'ACTION_USB_DEVICE_DETACHED' - means USB device has detached from device.
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                 Log.i(TAG, "Device Detached");

                 //USB device detached, update the device list
                 UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                 if (device != null) {
                     isAttachedDevice = false;
                 }
             }

            //If action 'ACTION_USB_PERMISSION' - means USB device permission request has been granted or denied.
            else if (ACTION_USB_PERMISSION.equals(action)) {
                     synchronized (this) {

                         UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                         if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                             if (device != null) {
                                 UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(device);
                                 device_type = UsbUtils.checkDeviceType(MainActivity.this, usbDeviceConnection, device);
                                 Log.i(TAG, "Checking Device Type in Database");

                                 usbDevicesDatabaseHelper.UpdateUSBDeviceType(MainActivity.this, device, device_type);

                                 updateDeviceList();
                                 Log.i(TAG, "Updating Device Type in Database");
                             }
                         }
                         else {
                             Log.d(TAG, "Permission denied for USB device");
                         }
                     }
                 }

            else {
                Log.e(TAG, "Unexpected value: " + action);
            }
        }
    };


    private void checkdeviceinDB(UsbDevice device) {

        // Check if the device is already in the database
        if (usbDevicesDatabaseHelper.checkUSBDeviceinDB(MainActivity.this, device)) {
            Log.i(TAG, "USB Device already in Database");
            updateDeviceList();
        }

        else {
            boolean inserted = usbDevicesDatabaseHelper.insertUSBDevicestoDB(MainActivity.this, device);

            if (inserted) {
                updateDeviceList();
                Log.i(TAG, "USB Device added to Database");

                // Request permission for the device
                usbManager.requestPermission(device, permissionIntent);
            }
            else {
                Log.e(TAG, "Failed to insert USB Device to Database");
            }
        }

    }


    private void updateDeviceList() {
        deviceList.clear();
        List<UsbDevice> usbDevices = usbDevicesDatabaseHelper.getAllUSBDevices(MainActivity.this);

        for (UsbDevice device : usbDevices) {
            String productName = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                productName = device.getProductName();
            }
            deviceList.add(productName);
        }

        deviceListAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        //to prevent memory leaks and ensure that receiver is not active when component is destroyed.
        unregisterReceiver(usbReceiver);
        Log.i(TAG, "usbReceiver unregistered");

        usbManager = null;
        usbDevicesDatabaseHelper = null;
    }
}