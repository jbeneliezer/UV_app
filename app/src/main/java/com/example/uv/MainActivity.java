package com.example.uv;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

        RecyclerView rvDevices = (RecyclerView) findViewById(R.id.recycler_view);

        // Create adapter passing in the sample user data
        rvAdapter = new DevicesAdapter(leDevicesFound);
        // Attach the adapter to the recyclerview to populate items
        rvDevices.setAdapter(rvAdapter);
        // Set layout manager to position the items
        rvDevices.setLayoutManager(new LinearLayoutManager(this));

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
        } else {
            //stop scanning if the device is scanning
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    @SuppressLint("MissingPermission")
    void updateLeDeviceList() {
        rvAdapter.notifyDataSetChanged();
    }

    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST) {
                        leDevicesFound.remove(result.getDevice());
                    } else {
                        if (!leDevicesFound.contains(result.getDevice())) {
                            leDevicesFound.add(result.getDevice());
                        }
                    }
                    updateLeDeviceList();
                }
            };

    //length of time to keep scanning: 30 seconds
    private static final long SCAN_TIME = 60000;

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
    private BluetoothDevice leDeviceConnected;
    DevicesAdapter rvAdapter;
}