package com.example.uv;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

//import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        skin_type = findViewById(R.id.skin_type);
        skin_type.setSelection(MainActivity.skin_type);
        skin_type.setOnItemSelectedListener(this);

        spf = findViewById(R.id.spf);
        spf.setSelection(MainActivity.spf);
        spf.setOnItemSelectedListener(this);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
    }
    private ImageButton backButton;

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        skin_type = findViewById(R.id.skin_type);
        spf = findViewById(R.id.spf);
        if (adapterView.equals(skin_type)) {
            MainActivity.skin_type = i;
            skin_type.setSelection(i);
        } else if (adapterView.equals(spf)) {
            MainActivity.spf = i;
            spf.setSelection(i);
        } else {
            Log.e("Error", "spinner error");
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // do nothing
    }

    private Spinner skin_type;
    private Spinner spf;

}