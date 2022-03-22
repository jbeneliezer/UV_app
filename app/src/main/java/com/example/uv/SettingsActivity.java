package com.example.uv;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

//import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
        Log.e("SettingsActivite", "started settings activity.");
    }
    private ImageButton backButton;


}