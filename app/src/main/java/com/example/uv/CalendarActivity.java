package com.example.uv;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity implements CalendarView.OnDateChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);

        calendarView = findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener(this);

        lineChart = findViewById(R.id.calendar_line_chart);
        configureLineChart();

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());

    }

    @Override
    protected void onResume() {
        getData(LocalDate.now() + ".txt");
        super.onResume();

        LineData lineData = new LineData(valueSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    @Override
    public void onSelectedDayChange(CalendarView calendarView, int year, int month, int dayOfMonth) {
        String selectedDate = String.format("%04d-%02d-%02d.txt", year, (month + 1), dayOfMonth);
        getData(selectedDate);

        LineData lineData = new LineData(valueSet);
        lineChart.setData(lineData);
        lineChart.invalidate();

        TextView tv = findViewById(R.id.irradiation);
        tv.setText(String.format("Total radiation: %.02f Joules", irradiance));
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
        startTime = LocalTime.of(0, 00, 00);
        endTime = LocalTime.of(23, 59, 59);
        xAxis.mAxisMinimum = MainActivity.getTotalTime(startTime);
        xAxis.mAxisMaximum = MainActivity.getTotalTime(endTime);
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

    private void getData(String filename) {
        String path = getApplicationContext().getFilesDir().toString() + "/data";
        File folder = new File(path);
        File[] fileList = folder.listFiles();
        valueSet.clear();
        if (fileList != null) {
            irradiance = 0;
            for (File file : fileList) {
                if (file.getName().equals(filename)) {
                    BufferedReader br;
                    try {
                        br = new BufferedReader(new FileReader(file));
                        String line;
                        int i = 0;
                        while ((line = br.readLine()) != null) {
                            double val = Double.parseDouble(line);
                            valueSet.addEntry(new Entry(i * 60, (float) val));
                            irradiance += (val * 0.025) / MainActivity.PROTECTION[MainActivity.spf];
                            ++i;
                        }
                        br.close();
                        return;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public CalendarView calendarView;

    static XAxis xAxis;
    private LineChart lineChart;
    private static LineDataSet valueSet;
    private LocalTime startTime;
    private LocalTime endTime;
    private double irradiance;
}
