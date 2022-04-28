package com.example.uv;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "UV tech";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initDashboard();

//        Intent bluetoothScan = new Intent(this, Bluetooth.class);
//        startActivity(bluetoothScan);

    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        LocalDate d = LocalDate.now();
        tmpHistory = history;
        if (tmpHistory.containsKey(d)) {
            tmpHistory.put(d, tmpHistory.get(d) + (int) irradiance);
        } else {
            tmpHistory.put(d, (int) irradiance);
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(savedData));
            tmpHistory.forEach((date, value) -> {
                try {
                    bw.write(formatter.format(date) + " " + value + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void initDashboard() {
        // configure storage
        configureStorage();

        // set up alarm
        createNotificationChannel();
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo))
                .setContentTitle("Warning!")
                .setContentText("Exposure limit exceeded. Reapply sunscreen.")
                .setPriority(NotificationCompat.PRIORITY_MAX);

        lineChart = findViewById(R.id.activity_main_linechart);
        configureLineChart();

        calendarButton = findViewById(R.id.calendarButton);
        calendarButton.setOnClickListener(view -> switchToCalendarActivity());

        settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(view -> switchToSettingsActivity());

        handler = new Handler();

        delay = 1000;

        handler.postDelayed( runnable = () -> {
            if (Bluetooth.isConnected()) {
                getUVData();
                processUVData();
                setLineChartData();
            }
            // testing
            else {
                localTime = LocalTime.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(5)));
                xAxis.setAxisMaximum(getTotalTime(localTime));
                float x = xAxis.mAxisMaximum;
                uvIndex = (float) Math.random() * 10;
                Entry e = new Entry(x, uvIndex);
                valueSet.addEntry(e);
                irradiance = 0;
                for (Entry i :
                        valueSet.getValues()) {
                    irradiance += i.getY();
                }
                processUVData();
                setLineChartData();
            }
            // end testing
            handler.postDelayed(runnable, delay);
        }, delay);
    }

    private void getUVData() {
        localTime = LocalTime.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(5)));
        xAxis.mAxisMaximum = getTotalTime(localTime);
        float x = xAxis.mAxisMaximum;
        // average sensor value:
//        for (int[] i: uvData) {
//            for (int j: i) {
//                uvIndex += j;
//            }
//        }
//        uvIndex /= (uvData.length * uvData[0].length);
        //
        Entry e = new Entry(x, uvIndex);
        valueSet.addEntry(e);
    }

    private void processUVData() {
        irradiance += uvIndex;
        localTime = LocalTime.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(5)));
        int timeDiff = getTotalTime(localTime) - getTotalTime(startTime);
        float meanIr = irradiance/timeDiff;
        irradianceLeft = irradianceLimit - irradiance;
        timeLeft = irradianceLeft/meanIr;
        if (timeLeft < 0) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            notificationManager.notify(0, builder.build());
        }
        stringTimeLeft = toTime((int) timeLeft);
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
        tv.setText(String.format("Current UV Index: %.02f", uvIndex));
        ProgressBar pb = findViewById(R.id.progressBar);
        pb.setProgress((int) ((irradiance/irradianceLimit) * 100));
        tv = findViewById(R.id.timeLeft);
        tv.setText("Time Left: " + stringTimeLeft);
        Log.e("values", (String.join(",", valueSet.toString())));

        LineData lineData = new LineData(valueSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    private void configureStorage() {
        history = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("data.txt"));
            String line = br.readLine();
            Log.e("read", line);
            while (line != null) {
                String[] spl = line.split(" ");
                LocalDate date = LocalDate.from(formatter.parse(spl[0]));
                int value = Integer.parseInt(spl[1]);
                history.put(date, value);
            }
        } catch (FileNotFoundException e) {
            savedData = new File(getApplicationContext().getFilesDir(), "data.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getTotalTime(@NonNull LocalTime lt) {
        return lt.getHour() * 3600 + lt.getMinute() * 60 + lt.getSecond();
    }

    private String toTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds %= 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void switchToCalendarActivity() {
        Intent switchToCalendar= new Intent(this, CalendarActivity.class);
        startActivity(switchToCalendar);
    }
    private void switchToSettingsActivity() {
        Intent switchToSettings = new Intent(this, SettingsActivity.class);
        startActivity(switchToSettings);
    }

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


//    public void startHandlerThread() {
//        handlerThread = new HandlerThread("HandlerThread");
//        handlerThread.start();
//        handler = new Handler(handlerThread.getLooper());
//    }

    XAxis xAxis;
    private LineChart lineChart;
    private LineDataSet valueSet;
    private Handler handler;
//    private HandlerThread handlerThread;
    private Runnable runnable;
    private LocalTime startTime;
    private LocalTime localTime;
    private String stringTimeLeft;
    private float timeLeft;
    private int delay;
    private float uvIndex;
    private float irradiance = 0;
    private final float irradianceLimit = 100;
    private float irradianceLeft = irradianceLimit;
    private ImageButton calendarButton;
    private ImageButton settingsButton;

    private static int sizeOfUVBuffer;
    private static int numOfUVSensors;
    private static int[][] uvData;

    public static Map<LocalDate, Integer> history;
    public Map<LocalDate, Integer> tmpHistory;
    private String filename;
    private File savedData;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private NotificationCompat.Builder builder;
}