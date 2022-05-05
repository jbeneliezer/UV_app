package com.example.uv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "UV tech";
    public static final int[] MED = {200, 200, 250, 300, 450, 600, 1000};
    public static final int[] PROTECTION = {1, 1, 15, 30, 50};
    public static int[][] uvData;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        today = LocalDate.now();
        dayData = new double[1440];
        configureStorage();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // linechart
        lineChart = findViewById(R.id.activity_main_linechart);

        initDashboard();

        skin_type = 0;
        spf = 0;
        irradiance = 0;
        graphRange = 60;

//        Intent bluetoothScan = new Intent(this, Bluetooth.class);
//        startActivity(bluetoothScan);

    }

    @Override
    protected void onPause() {
        try {
            currentData = new File(getApplicationContext().getFilesDir() + "/data/" + today.toString() + ".txt");
            if (currentData.exists()) {
                currentData.delete();
            }
            currentData.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(currentData));
            try {
                for (double i: dayData) {
                    bw.write(i + "\n");
                }
                bw.flush();
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        valueSet = getDayData(graphRange);
        configureLineChart();
        setLineChartData();
        super.onResume();
    }

    private void initDashboard() {

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

        // set up calendar button
        calendarButton = findViewById(R.id.calendarButton);
        calendarButton.setOnClickListener(view -> switchToCalendarActivity());

        // set up settings button
        settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(view -> switchToSettingsActivity());

        Button hourButton = findViewById(R.id.hour);
        hourButton.setOnClickListener(view -> setGraphRange(60));

        Button halfButton = findViewById(R.id.half);
        halfButton.setOnClickListener(view -> setGraphRange(30));

        Button _15Button= findViewById(R.id.fifteen);
        _15Button.setOnClickListener(view -> setGraphRange(15));

        // set up data read
        updateUVBufferSize(3, 4);
        minuteData = new double[60];
        minutePtr= 0;

        // start handler
        handler = new Handler();

        delay = 1000;

        handler.postDelayed( runnable = () -> {
            if (Bluetooth.isConnected()) {
                setLineChartData();
            }
            // testing
            else {
                localTime = LocalTime.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(5)));
                xAxis.setAxisMaximum(getTotalTime(localTime));
                uvIndex = (float) Math.random() * 10;

//                processUVData(uvIndex);
                Random rd = new Random();
                byte[] arr = new byte[24];
                byte[] arr1 = new byte[24];
                rd.nextBytes(arr);
                for (int i = 0; i < 24; ++i) {
                    arr1[i] = (byte) (Math.abs(arr[i] % 10));
                }
                getUvData(arr1);

                setLineChartData();

            }
            // end testing
            handler.postDelayed(runnable, delay);
        }, delay);
    }

    private static boolean processUVData(double uv, int timeOffset) {
        irradiance += (uv * 0.025) / PROTECTION[spf];
        irradianceLimit = MED[skin_type];
        irradianceLeft = irradianceLimit - irradiance;
        LocalTime localTime = LocalTime.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(5)));
        xAxis.setAxisMaximum(getTotalTime(localTime));
        float x = (float) (xAxis.mAxisMaximum - timeOffset);
        Entry e = new Entry(x, (float) uv);
        valueSet.addEntry(e);

        float avgIrr = 0;
        for (Entry entry : valueSet.getValues()) {
            avgIrr += entry.getY();
        }

        avgIrr = avgIrr / valueSet.getValues().size();
        avgIrr = (float) ((avgIrr * 0.025) / PROTECTION[spf]);
        int t = (int) (irradianceLeft / avgIrr);
        timeLeft = toTime(t);

        return (t > 0);

    }

     public static void updateUVBufferSize(int size, int sensors) {
         sizeOfUVBuffer = size;
         numOfUVSensors = sensors;
         uvData = new int[sizeOfUVBuffer][numOfUVSensors];
     }

     public static boolean getUvData(byte[] data) {
         boolean ret = true;

         //This function is called automatically every sizeOfUVBuffer seconds
         //uvData[0][x] is most recent data, uvData[x][0] correlates to sensor 0
         //uvData[sizeOfUVBuffer-1][x] is latest data, uvData[x][numOfUVSensors-1] correlates to last sensor
         for (int i = 0; i < sizeOfUVBuffer; i++) {
             for (int j = 0; j < numOfUVSensors; j++) {
                 uvData[sizeOfUVBuffer-i-1][j] = (
                         (
                                 (data[(i*numOfUVSensors*2) + (j*2+1)] & 0xFF) << 8 |
                                         (data[(i*numOfUVSensors*2) + (j*2)] & 0xFF) << 0
                         ) & 0xFFFF
                 );
             }
         }
         for (int i = sizeOfUVBuffer - 1; i >= 0; --i) {
             double avg = 0;
             int divisor = numOfUVSensors;
             for (int j: uvData[i]) {
                 if (j != 0xFFFF) {
                     avg += j;
                 } else {
                     --divisor;
                 }
             }
             if (minutePtr >= 60) {
                writeMinuteBuffer();
                minuteData = new double[60];
             }
             avg = avg/(divisor * 100);
             ret = processUVData(avg, i);
             minuteData[minutePtr++] = avg;
         }
         return ret;
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
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0);
        yAxis.setAxisMaximum(11);

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

//        ArrayList<Entry> start = new ArrayList<>();
//        start.add(new Entry(xAxis.mAxisMinimum, 0));
//        valueSet = new LineDataSet(start, "UV Index");
        valueSet.setDrawCircles(true);
        valueSet.setDrawValues(false);
        valueSet.setLineWidth(3);
        valueSet.setColor(Color.GREEN);
        valueSet.setCircleColor(Color.GREEN);
        valueSet.setFillAlpha(100);
        valueSet.setDrawFilled(true);
        valueSet.setFillColor(Color.BLUE);
        valueSet.setMode(LineDataSet.Mode.LINEAR);
    }

    private void setLineChartData() {
        TextView tv = findViewById(R.id.currentUV);
        tv.setText(String.format("Current UV Index: %.02f", uvIndex));
        ProgressBar pb = findViewById(R.id.progressBar);
        pb.setProgress((int) ((irradiance/irradianceLimit) * 100));
        tv = findViewById(R.id.timeLeft);
        tv.setText("Time Left: " + timeLeft);

        LineData lineData = new LineData(valueSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    private void configureStorage() {
        try {
            File folder = new File(getApplicationContext().getFilesDir(), "/data");
            if (!folder.exists()) {
                folder.mkdir();
            } else {
                File[] fileList = folder.listFiles();
                if (fileList != null) {
                    for (File file : fileList) {
                        if (file.getName().split("\\.")[0].equals(today.toString())) {
                            BufferedReader br = new BufferedReader(new FileReader(file));
                            String line;
                            int i = 0;
                            while ((line = br.readLine()) != null) {
                                dayData[i++] = Double.parseDouble(line);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeMinuteBuffer() {
        double avg = 0;
        for (double i: minuteData) {
            avg += i;
        }
        dayData[(int) (System.currentTimeMillis() / (1000 * 60)) % 1440] = avg / 60;
        minutePtr = 0;
    }

    protected static int getTotalTime(@NonNull LocalTime lt) {
        return lt.getHour() * 3600 + lt.getMinute() * 60 + lt.getSecond();
    }

    private static String toTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds %= 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private LineDataSet getDayData(int r) {
        List<Entry> values = new ArrayList<>();
        int end = getTotalTime(LocalTime.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(5)))) / 60;
        int start = end - r;
        while (dayData[start] == 0 && start < end) ++start;
        double[] subset = Arrays.copyOfRange(dayData, start, end);
        for (int i = 0; i < subset.length; ++i) {
            for (int j = 0; j < 60; ++j) {
                values.add(new Entry(start * 60 + i * 60 + j, (float) subset[i]));
            }
        }
        return new LineDataSet(values, "UV Index");
    }

    private void switchToCalendarActivity() {
        Intent switchToCalendar= new Intent(this, CalendarActivity.class);
        startActivity(switchToCalendar);
    }
    private void switchToSettingsActivity() {
        Intent switchToSettings = new Intent(this, SettingsActivity.class);
        startActivity(switchToSettings);
    }

    private void setGraphRange(int num) {
        graphRange = num;
        valueSet = getDayData(num);
        configureLineChart();
        setLineChartData();
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

    static XAxis xAxis;
    private LineChart lineChart;
    private static LineDataSet valueSet;
    private Handler handler;
    private Runnable runnable;
    private LocalTime startTime;
    private LocalTime localTime;
    private static String timeLeft;
    private int delay;

    private double uvIndex;
    private static double irradiance;
    private static double irradianceLimit;
    private static double irradianceLeft;

    private ImageButton calendarButton;
    private ImageButton settingsButton;

    private static int sizeOfUVBuffer;
    private static int numOfUVSensors;

    private static double[] minuteData;
    private static int minutePtr;
    private static double[] dayData;
    private File currentData;
    private LocalDate today;
    private int graphRange;

    public static int skin_type;
    public static int spf;

    private NotificationCompat.Builder builder;

}