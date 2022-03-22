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
import android.content.Intent;
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
        setContentView(R.layout.activity_main);
        initDashboard();

        Intent bluetoothScan = new Intent(this, Bluetooth.class);
        startActivity(bluetoothScan);
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
                if (Bluetooth.isConnected()) {
                    getUVData();
                    setLineChartData();
                }
                handler.postDelayed(runnable, delay);
            }
        }, delay);
    }

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

     public static void updateUVBufferSize(int size, int sensors) {
         sizeOfUVBuffer = size;
         numOfUVSensors = sensors;
         uvData = new int[sizeOfUVBuffer][numOfUVSensors];
     }

     public static void storeUVData(byte[] data) {
         //This function is called automatically every sizeOfUVBuffer seconds
         //uvData[0][x] is most recent data, uvData[x][0] correlates to sensor 0
         //uvData[sizeOfUVBuffer-1][x] is latest data, uvData[x][numOfUVSensors-1] correlates to last sensor
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

    private void setLineChartData() {
        TextView tv = findViewById(R.id.currentUV);
        tv.setText(String.format("%.02f", uvIndex));
        Log.e("values", (String.join(",", valueSet.toString())));

        LineData lineData = new LineData(valueSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

//    @RequiresApi(api = Build.VERSION_CODES.O)
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
    private static int uvData[][];
    private static int numOfUVSensors;
    private static int sizeOfUVBuffer;
}