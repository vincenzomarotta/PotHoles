package com.example.potholes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.potholes.activities.HomeActivity;
import com.example.potholes.activities.IntroActivity;
import com.example.potholes.activities.UserAccessActivity;
import com.example.potholes.entities.User;
import com.example.potholes.utils.RememberMePreferences;
import com.example.potholes.utils.UserDataPreferences;

public class MainActivity extends AppCompatActivity {

    private RememberMePreferences rememberMePreferences;
    private UserDataPreferences userDataPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rememberMePreferences = new RememberMePreferences(this);

        if (rememberMePreferences.checkRememberMe()) {
            userDataPreferences = new UserDataPreferences(this);
            new UserAccessActivity().login(this, userDataPreferences.getUserUsername(), userDataPreferences.getUserPassword());
            startActivity(new Intent(this, HomeActivity.class));
        }
        else
            startActivity(new Intent(this, UserAccessActivity.class));

        finish();

    }


}