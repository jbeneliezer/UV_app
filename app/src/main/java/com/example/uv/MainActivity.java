package com.example.uv;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        initBluetooth();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
//        handler.removeCallbacks(runnable);
        super.onPause();
    }

    private void initDashboard() {
        lineChart = findViewById(R.id.activity_main_linechart);
        configureLineChart();

        handler = new Handler();
        delay = 1000;

        handler.postDelayed( runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                getUVData();
                setLineChartData();
                handler.postDelayed(runnable, delay);
            }
        }, delay);
    }

    private void displayDashboard() {
        setContentView(R.layout.activity_main);
        initDashboard();
    }

    @SuppressLint("DefaultLocale")
    private void getUVData() {
        localTime = LocalTime.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(5)));
        xAxis.mAxisMaximum = getTotalTime(localTime);
        float x = xAxis.mAxisMaximum;
        uvIndex = (float) Math.random() * 10;
        Entry e = new Entry(x, uvIndex);
        valueSet.addEntry(e);

    }

    private void processUVData() {
        irradiance += uvIndex/40;
        localTime = LocalTime.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(5)));
        int timeDiff = getTotalTime(localTime) - getTotalTime(startTime);
    }

    private void configureLineChart() {
        lineChart.getLegend().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getAxisRight().setDrawGridLines(false);
        lineChart.getXAxis().setTextColor(Color.WHITE);
        lineChart.getAxisLeft().setTextColor(Color.WHITE);

        xAxis = lineChart.getXAxis();
        startTime = LocalTime.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(5)));
        localTime = startTime;
        xAxis.mAxisMinimum = getTotalTime(localTime);
        xAxis.mAxisMaximum = 1 + xAxis.mAxisMaximum;
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH) ;

            @Override
            public String getFormattedValue(float value) {
                long millis = (long) value * 1000L;
                return mFormat.format(new Date(millis));
            }

        });

        ArrayList<Entry> start = new ArrayList<>();
        start.add(new Entry(xAxis.mAxisMinimum, 0));
        valueSet = new LineDataSet(start, "UV Index");
        valueSet.setDrawCircles(false);
        valueSet.setDrawValues(false);
        valueSet.setLineWidth(3);
        valueSet.setColor(Color.GREEN);
        valueSet.setCircleColor(Color.GREEN);
        valueSet.setFillAlpha(100);
        valueSet.setDrawFilled(true);
        valueSet.setFillColor(Color.BLUE);
        valueSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

    }

    @SuppressLint("DefaultLocale")
    private void setLineChartData() {
        TextView tv = findViewById(R.id.currentUV);
        tv.setText(String.format("%.02f", uvIndex));
        Log.e("values", (String.join(",", valueSet.toString())));

        LineData lineData = new LineData(valueSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private int getTotalTime(@NonNull LocalTime lt) {
        return lt.getHour() * 3600 + lt.getMinute() * 60 + lt.getSecond();
    }

    XAxis xAxis;
    private LineChart lineChart;
    private LineDataSet valueSet;
    private Handler handler;
    private Runnable runnable;
    private LocalTime startTime;
    private LocalTime localTime;
    private int delay;
    private float uvIndex;
    private float irradiance;
    private float irradianceLimit;
    private int uvData[][];
    private int numOfUVSensors;
    private int sizeOfUVBuffer;

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
    void updateLeDeviceList() {
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
            }
        };

    private DevicesAdapter.AdapterListener lePairListener =
            new DevicesAdapter.AdapterListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void buttonClick(View view, int position) {
                    //attempt to pair with device on button click
                    leDevicesFound.get(position).connectGatt(MainActivity.this, false, leGattCallback);
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

            numOfUVSensors = 4;
            sizeOfUVBuffer = 3;
            uvData = new int[sizeOfUVBuffer][numOfUVSensors];

            //change activity to dashboard
            displayDashboard();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (characteristic.equals(leUVDataCharacteristic)) {
                byte[] data = characteristic.getValue();

                for (int i = 0; i < sizeOfUVBuffer; i++) {
                    for (int o = 0; o < numOfUVSensors; o++) {
                        uvData[sizeOfUVBuffer-i-1][o] = (
                                (
                                        (data[(i*numOfUVSensors*2) + (o*2+1)] & 0xFF) << 8 |
                                        (data[(i*numOfUVSensors*2) + (o*2)] & 0xFF) << 0
                                ) & 0xFFFF
                        );
                    }
                }

                //CALL FUNCTIONS TO UPDATE GRAPHS HERE
                //uvData[0][x] is most recent data, uvData[x][0] correlates to sensor 0
                //uvData[sizeOfUVBuffer-1][x] is latest data, uvData[x][numOfUVSensors-1] correlates to last sensor
            }
        }
    };

    //length of time to keep scanning: 30 seconds
    private static final long SCAN_TIME = 60000;
    protected static final UUID UV_SENSE_SERVICE_UUID = UUID.fromString("23e490ae-dbed-41a6-8fda-b4a13d4cc679");
    protected static final UUID UV_SENSE_DATA_CHARACTERISTIC_UUID = UUID.fromString("7f47dcfb-b44b-4703-9298-4e0e981bfcab");
    //CCCD for notify/indicate characteristic
    protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private List<ScanFilter> leScanFilter = Arrays.asList(
            new ScanFilter.Builder()
                    .setDeviceName("UV Sense")
                    //.setServiceUuid(ParcelUuid.fromString("23e490ae-dbed-41a6-8fda-b4a13d4cc679"))
                    .build()
    );
    private ScanSettings leScanSetting = new ScanSettings.Builder()
            .setLegacy(false)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setNumOfMatches(ScanSettings.MATCH_NUM_FEW_ADVERTISEMENT)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build();

    private List<BluetoothDevice> leDevicesFound = new ArrayList<BluetoothDevice>();
    private BluetoothGatt leDeviceConnected = null;
    private boolean connected = false;
    private SwipeRefreshLayout swipeContainer;
    private DevicesAdapter rvAdapter;

    private BluetoothGattService leUVService;
    private BluetoothGattCharacteristic leUVDataCharacteristic;
}