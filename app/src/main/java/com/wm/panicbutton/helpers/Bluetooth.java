package com.wm.panicbutton.helpers;


import java.util.List;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.wm.panicbutton.interfaces.Callback;
import com.wm.panicbutton.interfaces.Processor;

import static android.content.ContentValues.TAG;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Bluetooth extends Service {

    private static final int ACTIVITY_REQUEST_CODE = 0;
    private static final int SERVICE_FOREGROUND_ID = 1;
    private static final String BLUETOOTH_NAME = "cmpe272";
    public static final int BLUETOOTH_SERVICE = 3;
    public static final int BLUETOOTH_CHARACTERISTIC = 0;
    public static final int BLUETOOTH_DESCRIPTOR = 0;
    public static final int BLUETOOTH_STATE_INACTIVE = 0;
    public static final int BLUETOOTH_STATE_SCANNING = 1;
    public static final int BLUETOOTH_STATE_ACTIVE = 2;

    private Context context;
    private Callback callback;
    private Processor processor;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoothLeScanner mLEScanner;
    private BluetoothGatt mGatt;
    private final IBinder mBinder = new BluetoothBinder();

    // for service binding
    public class BluetoothBinder extends Binder {
        public Bluetooth getService() {
            return Bluetooth.this;
        }
    }

    public Bluetooth() {

    }

    public void initialize(Context context, Callback callback, Processor processor) {
        this.context = context;
        this.processor = processor;

        final BluetoothManager bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (Build.VERSION.SDK_INT >= 21) {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }

        mHandler = new Handler(this.context.getMainLooper());

        this.callback = callback;
    }

    public void activate() {
        scanLeDevice(true);
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void deactivate() {
        callback.notifyState(BLUETOOTH_STATE_INACTIVE);
        disconnect();
        scanLeDevice(false);
        stopForeground(SERVICE_FOREGROUND_ID);
    }

    private void disconnect() {
        if (mGatt != null) {
            mGatt.close();
            mGatt = null;
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            disconnect();
            callback.notifyState(BLUETOOTH_STATE_SCANNING);
            startScan();
        } else {
            stopScan();
        }
    }

    private void startScan() {
        if (Build.VERSION.SDK_INT < 21) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mLEScanner.startScan(mScanCallback);
        }
    }

    private void stopScan() {
        if (Build.VERSION.SDK_INT < 21) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            if(btDevice != null) {
                if (btDevice.getName() != null && btDevice.getName().equals(BLUETOOTH_NAME)) {
                    connectToDevice(btDevice);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("scanResult - results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("scan failed", "error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(context, false, gattCallback);
            scanLeDevice(false);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    scanLeDevice(true);
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    final BluetoothGattCharacteristic receiveCharacteristic =
                            gatt.getServices().get(BLUETOOTH_SERVICE).getCharacteristics().get(BLUETOOTH_CHARACTERISTIC);
                    BluetoothGattDescriptor receiveConfigDescriptor =
                            receiveCharacteristic.getDescriptors().get(BLUETOOTH_DESCRIPTOR);
                    if (receiveConfigDescriptor != null) {
                        Log.i(TAG, "successfully connected!");
                        gatt.setCharacteristicNotification(receiveCharacteristic, true);
                        receiveConfigDescriptor.setValue(
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(receiveConfigDescriptor);
                        callback.notifyState(BLUETOOTH_STATE_ACTIVE);
                    } else {
                        Log.e(TAG, "receive config descriptor not found!");
                    }
                } catch(Exception e) {
                    Log.e(TAG, "receive characteristic not found!");
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        // triggered if got some signal from the connected device
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String signal = Integer.toString(characteristic.getValue()[0]);
            Log.i("signal", signal);
            processor.process(signal);
        }
    };

    private void runOnUiThread(Runnable r) {
        mHandler.post(r);
    }

    // show notification to user
    // this function is needed in order to push service in foreground
    public Notification getNotification() {
        Intent intent = new Intent(this, Bluetooth.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,ACTIVITY_REQUEST_CODE,intent,0);

        NotificationCompat.Builder foregroundNotification = new NotificationCompat.Builder(this);
        foregroundNotification.setOngoing(true);

        foregroundNotification.setContentTitle("Bluetooth service activated")
                .setContentText("Listening for signals..")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent);

        return foregroundNotification.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // triggered when starting service
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(SERVICE_FOREGROUND_ID, getNotification());
        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDestroy() {
        super.onDestroy();
        deactivate();
    }
}
