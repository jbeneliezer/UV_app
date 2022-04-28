package com.example.uv;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.kizitonwose.calendarview.CalendarView;
import com.kizitonwose.calendarview.model.CalendarDay;
import com.kizitonwose.calendarview.ui.DayBinder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);

        calendarView = findViewById(R.id.calendarView);

        calendarView.setDayBinder(new DayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(DayViewContainer dayViewContainer, CalendarDay calendarDay) {
                LocalDate date = calendarDay.getDate();
                if (MainActivity.history.containsKey(date)) {
                    dayViewContainer.textView.setBackgroundColor(levels[getSeverity(MainActivity.history.get(date))]);
                    dayViewContainer.irr.setText(MainActivity.history.get(date));
                } else {
                    dayViewContainer.textView.setBackgroundColor(levels[0]);
                }
                dayViewContainer.textView.setText(calendarDay.getDate().getDayOfMonth());
            }
        });

        YearMonth currentMonth = YearMonth.now();
        YearMonth firstMonth = YearMonth.of(2022, 1);
        YearMonth lastMonth = currentMonth.plusMonths(1);
        DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
        calendarView.setup(firstMonth, lastMonth, firstDayOfWeek);
        calendarView.scrollToMonth(currentMonth);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());

    }

    private int getSeverity(int irr) {
        if (irr <= 0) {
            return 0;
        } else if (irr <= 1000) {
            return 1;
        } else if (irr <= 2000) {
            return 2;
        } else if (irr <= 3000) {
            return 3;
        } else if (irr <= 4000) {
            return 4;
        } else if (irr <= 5000){
            return 5;
        } else {
            return 6;
        }
    }

    public CalendarView calendarView;

    private int[] levels = {Color.GRAY,
            Color.parseColor("#4aedff"),
            Color.parseColor("#4affb7"),
            Color.parseColor("#78ff8c"),
            Color.parseColor("#f1ff78"),
            Color.parseColor("#ffe478"),
            Color.parseColor("#ffb078")};

}