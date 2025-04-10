package com.example.potholes.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.potholes.entities.User;

public class RememberMePreferences {
    private Context context;
    SharedPreferences sharedPreferences;

    /**
     * Creates RememberMePreferences using SharedPreferences to check if the user wants to remain logged in.
     * @param context context of the application used to create SharedPreferences.
     */
    public RememberMePreferences(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("checkbox", context.MODE_PRIVATE);
    }

    /**
     * Sets the remember to false, so the user won't be immediately directed to his Homepage.
     * The application won't keep the user logged in.
     */
    public void setRememberMeFalse(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("remember", "false");
        editor.apply();
    }

    /**
     * Sets remember to true. It means that the user has chosen to be directed
     * to the homepage, skipping the login section.
     * The application will keep the user logged in.
     */
    public void setRememberMeTrue(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("remember", "true");
        editor.apply();
    }

    /**
     * Checks if remember preferences is set to true.
     * @return boolean
     */
    public boolean checkRememberMe() {
        String checkbox = sharedPreferences.getString("remember", "");
        if(checkbox.equals("true"))
            return true;
        else
            return false;
    }

}
