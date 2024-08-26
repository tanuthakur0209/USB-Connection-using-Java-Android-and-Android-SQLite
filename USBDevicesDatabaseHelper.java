package com.example.usbdb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class USBDevicesDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "USB_SQLite_DB";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "usb_devices.db";
    private static final String TABLE_NAME = "usb_devices";
    private static final String COLUMN_SR_NO = "sr_no";
    private static final String COLUMN_DEVICE_NAME = "device_name";
    private static final String COLUMN_VENDOR_ID = "vendor_id";
    private static final String COLUMN_PRODUCT_ID = "product_id";
    private static final String COLUMN_DEVICE_SERIAL_NUMBER = "serial_number";
    private static final String COLUMN_MANUFACTURER = "manufacturer";
    private static final String COLUMN_PRODUCT_NAME = "product_name";
    private static final String COLUMN_DEVICE_TYPE = "device_type";
    private static final String COLUMN_DEVICE_ATTACH_DATETIME = "date_time";
    private static USBDevicesDatabaseHelper usbDevicesDatabaseHelper;

    public USBDevicesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //create table
    @Override
    public void onCreate(SQLiteDatabase db) {

        String createUSBDeviceTable = "CREATE TABLE " +
                TABLE_NAME + " (" +
                COLUMN_SR_NO + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DEVICE_NAME + " TEXT, " +
                COLUMN_VENDOR_ID + " INTEGER, " +
                COLUMN_PRODUCT_ID + " INTEGER, " +
                COLUMN_DEVICE_SERIAL_NUMBER + " TEXT, " +
                COLUMN_MANUFACTURER + " TEXT, " +
                COLUMN_PRODUCT_NAME + " TEXT," +
                COLUMN_DEVICE_TYPE + " TEXT," +
                COLUMN_DEVICE_ATTACH_DATETIME + " TEXT" +
                ") ";

        //to execute sql query
        db.execSQL(createUSBDeviceTable);
    }


    //when database need to be upgraded
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

        // Create tables again
        onCreate(db);
    }


    //insert usb device information into database.
    // It gets writable db instance, creates 'ContentValues' object with device information and inserts it into table
    public static boolean insertUSBDevicestoDB(Context context, UsbDevice device) {

        usbDevicesDatabaseHelper = new USBDevicesDatabaseHelper(context);

        //get data repository in write mode
        SQLiteDatabase db = usbDevicesDatabaseHelper.getWritableDatabase();

        //creating map of values, where column names are keys
        ContentValues values = new ContentValues();

        //passing values with its key-value pair
        values.put(COLUMN_DEVICE_NAME, device.getDeviceName());
        values.put(COLUMN_VENDOR_ID, device.getVendorId());
        values.put(COLUMN_PRODUCT_ID, device.getProductId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            values.put(COLUMN_DEVICE_SERIAL_NUMBER, device.getSerialNumber());
            values.put(COLUMN_MANUFACTURER, device.getManufacturerName());
            values.put(COLUMN_PRODUCT_NAME, device.getProductName());
        }
        values.put(COLUMN_DEVICE_TYPE, "NA");
        values.put(COLUMN_DEVICE_ATTACH_DATETIME, getCurrentTimestamp());

        //insert record in the table with values that are passed. Returns row ID
        long newRowId = db.insert(TABLE_NAME, null, values);

        if (newRowId != -1) {
            Log.i(TAG, "Device added to the database successfully");
            db.close();
            return true;
        }
        else {
            Log.e(TAG, "Error inserting USB device information into database");
            db.close();
            return false;
        }
    }

    //to check if usb device already exists in database.
    //It queries db for matching device name or serial no.
    public static boolean checkUSBDeviceinDB(Context context, UsbDevice device) {

        usbDevicesDatabaseHelper = new USBDevicesDatabaseHelper(context);
        SQLiteDatabase db = usbDevicesDatabaseHelper.getReadableDatabase();

        String deviceName = device.getDeviceName();
        String serialNumber = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            serialNumber = device.getSerialNumber();
        }

        String selection = COLUMN_DEVICE_SERIAL_NUMBER + "=? OR " + COLUMN_DEVICE_NAME + "=?";
        String[] selectionArgs = {serialNumber, deviceName};


        Cursor cursor = db.query(
                TABLE_NAME,
                new String[]{COLUMN_DEVICE_NAME, COLUMN_DEVICE_SERIAL_NUMBER, COLUMN_VENDOR_ID, COLUMN_PRODUCT_ID, COLUMN_PRODUCT_NAME, COLUMN_MANUFACTURER},
                selection,
                selectionArgs,
                null,
                null,
                null,
                null
        );

        if (cursor.getCount() > 0) {
            //usb device is already known
            cursor.moveToFirst();

//            String productNameFromDB = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_NAME));

            // Update the attachment time in the database
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_DEVICE_ATTACH_DATETIME, getCurrentTimestamp());

            //The “?” in whereClause parameter serves as a placeholder for statement.
            int rowsAffected = db.update(
                    TABLE_NAME,
                    contentValues,
                    selection,
                    selectionArgs
            );

            if (rowsAffected > 0) {
                Log.i(TAG, "Attachment time updated successfully: " + device.getDeviceName());
            }
            else {
                Log.e(TAG, "Error updating attachment time: " + device.getDeviceName());
            }

            cursor.close();
            return true;
        }
        else {
            cursor.close();
            return false;
        }
    }

    // Updating Device Type column in database.
    // It takes device and new device type as parameters, retrieves writable database instance and updates corresponding row in table
    public static void UpdateUSBDeviceType(Context context, UsbDevice device, String deviceType) {

        usbDevicesDatabaseHelper = new USBDevicesDatabaseHelper(context);
        SQLiteDatabase db = usbDevicesDatabaseHelper.getWritableDatabase();

        ContentValues contentValues1 = new ContentValues();
        contentValues1.put(COLUMN_DEVICE_TYPE, deviceType);

        String serialNumber = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            serialNumber = device.getSerialNumber();
        }
        String deviceName = device.getDeviceName();

        int rowsAffected = db.update(TABLE_NAME, contentValues1, COLUMN_DEVICE_SERIAL_NUMBER + "=? OR " + COLUMN_DEVICE_NAME + "=?", new String[]{serialNumber, deviceName});

        if (rowsAffected > 0) {
            Log.i(TAG, "Device type updated successfully: " + device.getDeviceName());
        }
        else {
            Log.e(TAG, "Error updating device type: " + device.getDeviceName());
        }

        db.close();
    }

    // Get current timestamp in "yyyy-MM-dd HH:mm:ss" format
    private static String getCurrentTimestamp() {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    public static void deleteUSBDeviceFromDB(Context context, UsbDevice device) {
        usbDevicesDatabaseHelper = new USBDevicesDatabaseHelper(context);
        SQLiteDatabase db = usbDevicesDatabaseHelper.getWritableDatabase();

        String serialNumber = null;
        String deviceName = device.getDeviceName();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            serialNumber = device.getSerialNumber();
        }

        if (serialNumber == null) {
            Log.e(TAG, "Serial number is null for device: " + device.getDeviceName());
            return;
        }

        int deletedRows = db.delete(TABLE_NAME,
                COLUMN_DEVICE_NAME + "=? OR " + COLUMN_DEVICE_SERIAL_NUMBER + "=?",
                new String[] {deviceName, serialNumber});

        if (deletedRows > 0) {
            Log.i(TAG, "Device deleted successfully: " + device.getDeviceName());
        }
        else {
            Log.e(TAG, "Error deleting device: " + device.getDeviceName());
        }

        db.close();
    }

    public List<UsbDevice> getAllUSBDevices(Context context) {
        List<UsbDevice> usbDevices = new ArrayList<>();

        usbDevicesDatabaseHelper = new USBDevicesDatabaseHelper(context);
        SQLiteDatabase db = usbDevicesDatabaseHelper.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String productName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEVICE_NAME));
                UsbDevice usbDevice = getUsbDeviceFromProductName(productName, context);

                // Add the UsbDevice object to the list
                if (usbDevice != null) {
                    usbDevices.add(usbDevice);
                }
            }
            while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        db.close();

        return usbDevices;
    }

    private UsbDevice getUsbDeviceFromProductName(String productName, Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (device.getProductName().equals(productName)) {
                    return device;
                }
            }
        }

        return null;
    }


    public String getAttachmentTimestamp(Context context, UsbDevice device) {

        usbDevicesDatabaseHelper = new USBDevicesDatabaseHelper(context);
        SQLiteDatabase db = usbDevicesDatabaseHelper.getReadableDatabase();

        String[] projection = {COLUMN_DEVICE_ATTACH_DATETIME};
        String selection = COLUMN_DEVICE_NAME + " = ? AND " + COLUMN_VENDOR_ID + " = ? AND " + COLUMN_PRODUCT_ID + " = ?";
        String[] selectionArgs = {device.getDeviceName(), String.valueOf(device.getVendorId()), String.valueOf(device.getProductId())};

        Cursor cursor = db.query(TABLE_NAME, projection, selection, selectionArgs, null, null, null);

        String timestamp = null;
        if (cursor.moveToFirst()) {
            timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEVICE_ATTACH_DATETIME));
        }

        cursor.close();
        db.close();

        return timestamp;
    }

}