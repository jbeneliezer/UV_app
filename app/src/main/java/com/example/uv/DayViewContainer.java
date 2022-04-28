package com.example.uv;

import android.view.View;
import android.widget.TextView;

import com.kizitonwose.calendarview.ui.ViewContainer;

public class DayViewContainer extends ViewContainer {

    public DayViewContainer(View view) {
        super(view);
        textView = view.findViewById(R.id.calendarDayText);
        irr = view.findViewById(R.id.irr);
    }

    public TextView textView;
    public TextView irr;
}
