package com.wm.panicbutton.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.skyfishjy.library.RippleBackground;
import com.wm.panicbutton.R;
import com.wm.panicbutton.helpers.Bluetooth;
import com.wm.panicbutton.helpers.LocationUpdater;
import com.wm.panicbutton.helpers.SignalProcessor;
import com.wm.panicbutton.interfaces.Callback;

import java.util.ArrayList;
import java.util.List;

import at.markushi.ui.CircleButton;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends AppCompatActivity implements Callback {

    private static final String TAG = "MAIN";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int DISCOVERABLE_REQUEST_CODE = 1;

    private CircleButton panicButton;
    private ImageButton toContactActivityButton;
    private RippleBackground rippleBackground;
    private Toast statusToast;
    private Handler stateHandler;

    private Intent bluetoothIntent;
    private Bluetooth bluetoothService;
    private boolean isActivated;
    private boolean isScanning;

    private LocationUpdater locationUpdater;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // close the application if bluetooth not supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // initialize UI components
        statusToast = new Toast(getApplicationContext());
        rippleBackground = findViewById(R.id.content);
        stateHandler = new Handler();

        // initialize panic button, set listener to control bluetooth
        panicButton = findViewById(R.id.panicButton);
        panicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(View v) {
                if (requestPermissions() && requestBluetoothDiscoverable()) {
                    if(isActivated) {
                        Log.i(TAG, "deactivate");
                        panicButton.setColor(ResourcesCompat.getColor(getResources(), R.color.colorDeactivated, null));
                        stopService(bluetoothIntent);
                        bluetoothService.deactivate();
                        locationUpdater.deactivate();
                        isScanning = false;
                    } else {
                        Log.i(TAG, "activate");
                        panicButton.setColor(ResourcesCompat.getColor(getResources(), R.color.colorActivated, null));
                        startService(bluetoothIntent);
                        bluetoothService.activate();
                        locationUpdater.activate();
                    }
                    isActivated = !isActivated;
                }

            }
        });


        // initialize contact button, set listener to navigate to contact activity
        toContactActivityButton = findViewById(R.id.toContactActivityButton);
        toContactActivityButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, ContactActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        // initialize bluetooth components
        bluetoothIntent = new Intent(this, Bluetooth.class);
        Intent intent = bluetoothIntent;
        bindService(intent, mConnection, BIND_AUTO_CREATE); // bind bluetooth service in order to access its function

        locationUpdater = new LocationUpdater(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bluetoothService != null) {
            bluetoothService.deactivate();
        }
        stopService(bluetoothIntent);
        locationUpdater.deactivate();
    }

    // callback for service binding
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // initialize and start bluetooth service
            Bluetooth.BluetoothBinder binder = (Bluetooth.BluetoothBinder) service;
            bluetoothService = binder.getService();
            bluetoothService.initialize(getApplicationContext(), MainActivity.this, new SignalProcessor(getApplicationContext()));
            Log.i(TAG, "bluetooth service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bluetoothService = null;
            Log.i(TAG, "bluetooth service disconnected");
        }
    };

    // request for all permissions required for the application
    private boolean requestPermissions() {
        boolean areAllGranted = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            List<String> permissions = new ArrayList<>();

            if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_DENIED) {

                permissions.add(Manifest.permission.SEND_SMS);

            }

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

            }

            if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {

                permissions.add(Manifest.permission.CALL_PHONE);

            }

            if (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {

                permissions.add(Manifest.permission.BLUETOOTH);

            }

            if (!permissions.isEmpty()) {
                areAllGranted = false;
                requestPermissions(permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            }
        }
        return areAllGranted;
    }

    // if bluetooth is not enabled, ask for it
    private boolean requestBluetoothDiscoverable() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(discoverableIntent, DISCOVERABLE_REQUEST_CODE);
            return false;
        }
        return true;
    }

    // show current status: Scanning, Activated, Deactivated
    private void showStatus(int status) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_status, (ViewGroup) findViewById(R.id.toast_layout_root));
        TextView text = layout.findViewById(R.id.status);
        text.setText(status);
        statusToast.setDuration(Toast.LENGTH_SHORT);
        statusToast.setView(layout);
        statusToast.show();
    }

    // start/stop ripple effect
    private void animateRipple(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!enable) {
                    rippleBackground.stopRippleAnimation();
                } else {
                    rippleBackground.startRippleAnimation();
                }
            }
        });
    }

    // to make Scanning status blink
    private Runnable r = new Runnable() {
        public void run() {
            if(isScanning) {
                animateRipple(false);
                showStatus(R.string.status_scanning);
                stateHandler.postDelayed(this, 2500);
            }
        }
    };

    // a callback called from bluetooth service to update UI
    public void notifyState(int state) {
        Log.i(TAG, "state: " + Integer.toString(state));
        switch (state) {
            case Bluetooth.BLUETOOTH_STATE_INACTIVE: {
                animateRipple(false);
                showStatus(R.string.status_deactivated);
                break;
            }
            case Bluetooth.BLUETOOTH_STATE_SCANNING: {
                isScanning = true;
                showStatus(R.string.status_scanning);
                stateHandler.post(r);   // start blinking Scanning status
                break;
            }
            case Bluetooth.BLUETOOTH_STATE_ACTIVE: {
                isScanning = false;     // stop blinking Scanning status
                if(!rippleBackground.isRippleAnimationRunning()) {
                    animateRipple(true);
                    showStatus(R.string.status_activated);
                }
                break;
            }
        }
    }
}