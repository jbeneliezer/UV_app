package com.example.uv;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.O)
public class Bluetooth extends AppCompatActivity {

    private static final String CHANNEL_ID = "UV tech";

    //length of time to keep scanning: 30 seconds
    private static final long SCAN_TIME = 60000;
    protected static final UUID UV_SENSE_SERVICE_UUID = UUID.fromString("23e490ae-dbed-41a6-8fda-b4a13d4cc679");
    protected static final UUID UV_SENSE_DATA_CHARACTERISTIC_UUID = UUID.fromString("7f47dcfb-b44b-4703-9298-4e0e981bfcab");
    //CCCD for notify/indicate characteristic
    protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static Handler handler;
    private static BluetoothLeScanner bluetoothLeScanner;
    private static boolean scanning = false;
    private static List<ScanFilter> leScanFilter = Arrays.asList(
            new ScanFilter.Builder()
                    .setDeviceName("UV Sense")
                    //.setServiceUuid(ParcelUuid.fromString("23e490ae-dbed-41a6-8fda-b4a13d4cc679"))
                    .build()
    );
    private static ScanSettings leScanSetting = new ScanSettings.Builder()
            .setLegacy(false)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setNumOfMatches(ScanSettings.MATCH_NUM_FEW_ADVERTISEMENT)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build();



    private static List<BluetoothDevice> leDevicesFound = new ArrayList<BluetoothDevice>();
    private static BluetoothGatt leDeviceConnected = null;
    private static boolean connected = false;
    private static SwipeRefreshLayout swipeContainer;
    private static DevicesAdapter rvAdapter;

    private static BluetoothGattService leUVService;
    private static BluetoothGattCharacteristic leUVDataCharacteristic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        initBluetooth();


        // set up alarm
        createNotificationChannel();

        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo))
                .setSmallIcon(IconCompat.createWithBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.logo)))
                .setContentTitle("Warning!")
                .setContentText("UV exposure limit exceeded.")
                .setColor(0xff9026ed)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    private void initBluetooth() {
        //request permission to use location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //entered if location permission was not given
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        //get bluetooth peripheral for the phone
        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            //bluetoothLeScanner is null if bluetooth is disabled
            return;
        }

        handler = new Handler();

        // Lookup the swipe container view
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(leRefreshListener);

        swipeContainer.setColorSchemeResources(R.color.pink,
                R.color.purple,
                R.color.blue);

        RecyclerView rvDevices = (RecyclerView) findViewById(R.id.recycler_view);

        // Create adapter passing in the sample user data
        rvAdapter = new DevicesAdapter(leDevicesFound, lePairListener);
        // Attach the adapter to the recyclerview to populate items
        rvDevices.setAdapter(rvAdapter);
        // Set layout manager to position the items
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        rvDevices.addItemDecoration(itemDecoration);

        //start scanning
        scanLeDevice();
    }

    public static boolean isScanning() {
        return scanning;
    }

    public static boolean isConnected() {
        return connected;
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice() {
        if (!scanning) {
            //create a "timer" that will execute run() after SCAN_TIME is reached
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_TIME);

            //start scanning if the device is not scanning
            scanning = true;
            bluetoothLeScanner.startScan(leScanFilter, leScanSetting, leScanCallback);
            //bluetoothLeScanner.startScan(leScanCallback);
        } else {
            //if the device is scanning, stop and restart scanning
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
            scanLeDevice();
        }
    }

    @SuppressLint("MissingPermission")
    private void updateLeDeviceList() {
        rvAdapter.notifyDataSetChanged();
    }

    private void enableUVNotifications() {
        setCharacteristicNotification(leUVDataCharacteristic, true);
    }

    private void disableUVNotifications() {
        setCharacteristicNotification(leUVDataCharacteristic, false);
    }

    @SuppressLint("MissingPermission")
    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                                      boolean enabled) {
        //enable notification for the characteristic locally
        leDeviceConnected.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //enable notification for the characteristic for the peripheral server
        leDeviceConnected.writeDescriptor(descriptor);
    }

    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (!leDevicesFound.contains(result.getDevice())) {
                        leDevicesFound.add(result.getDevice());             //add device to list
                    }
                    updateLeDeviceList();                                   //update the list on display
                }
            };

    private SwipeRefreshLayout.OnRefreshListener leRefreshListener =
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    //clear list of discovered devices
                    leDevicesFound.clear();
                    swipeContainer.setRefreshing(false);
                    rvAdapter.notifyDataSetChanged();
                    scanLeDevice();
                }
            };

    private DevicesAdapter.AdapterListener lePairListener =
            new DevicesAdapter.AdapterListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void buttonClick(View view, int position) {
                    //attempt to pair with device on button click
                    leDevicesFound.get(position).connectGatt(Bluetooth.this, false, leGattCallback);
                    leDevicesFound.clear();
                    rvAdapter.notifyDataSetChanged();
                }
            };

    private BluetoothGattCallback leGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //successfully connected to the GATT Server
                leDeviceConnected = gatt;
                connected = true;
                //identify services from the device
                leDeviceConnected.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //disconnected from the GATT Server
                leDeviceConnected = null;
                connected = false;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status != BluetoothGatt.GATT_SUCCESS){
                //if unable to get services from gatt
                return;
            }
            //get UV Sense service and UV Sense Sensor Data characteristic
            leUVService = gatt.getService(UV_SENSE_SERVICE_UUID);
            leUVDataCharacteristic = leUVService.getCharacteristic(UV_SENSE_DATA_CHARACTERISTIC_UUID);
            //turn on notifications for Sensor Data characteristic
            enableUVNotifications();

            MainActivity.updateUVBufferSize(3, 4);

            //end this activity and go back to the dashboard
            finish();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (characteristic.equals(leUVDataCharacteristic)) {
                byte[] data = characteristic.getValue();

                if (!MainActivity.getUvData(data)) {
                    sendNotification();
                }

                //CALL FUNCTIONS TO UPDATE GRAPHS HERE
            }
        }
    };

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_MAX;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, builder.build());
    }

    private NotificationCompat.Builder builder;
}