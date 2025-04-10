package com.example.potholes.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.potholes.R;

public class IntroActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        getSupportActionBar().hide();
    }

    /**
     * Opens HomeActivity.
     */
    public void openHome() {
        Intent home = new Intent(IntroActivity.this, HomeActivity.class);
        startActivity(home);
        finish();
    }

}