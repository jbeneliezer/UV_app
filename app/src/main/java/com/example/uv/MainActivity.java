package com.example.uv;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
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
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

//    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
//        handler.removeCallbacks(runnable);
        super.onPause();
    }

    @SuppressLint("DefaultLocale")
//    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getUVData() {
        localTime = LocalTime.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(5)));
        xAxis.mAxisMaximum = getTotalTime(localTime);
        float x = xAxis.mAxisMaximum;
        uvIndex = (float) Math.random() * 10;
        Entry e = new Entry(x, uvIndex);
        valueSet.addEntry(e);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void processUVData() {
        irradiance += uvIndex/40;
        localTime = LocalTime.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(5)));
        int timeDiff = getTotalTime(localTime) - getTotalTime(startTime);
    }

//    @RequiresApi(api = Build.VERSION_CODES.O)
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
//    @RequiresApi(api = Build.VERSION_CODES.O)
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
}