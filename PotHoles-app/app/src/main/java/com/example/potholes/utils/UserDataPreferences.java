package com.example.potholes.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.potholes.entities.ECMUser;
import com.example.potholes.entities.User;

public class UserDataPreferences {
    private Context context;
    SharedPreferences sharedPreferences;

    private final String NAME = "name";
    private final String SURNAME = "surname";
    private final String PASSWORD = "password";
    private final String EMAIL = "email";
    private final String USERNAME = "username";

    /**
     * Creates UserDataPreferences to store non-private user data.
     * @param context context of the application used to create SharedPreferences.
     */
    public UserDataPreferences(Context context){
        this.context = context;
        sharedPreferences = context.getSharedPreferences("user_data", context.MODE_PRIVATE);
    }

    /**
     * Sets user data.
     * @param user user datas gotten during registration or thanks to UserDAO method.
     */
    public void setUserData(User user){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(NAME, user.getName());
        editor.putString(SURNAME, user.getSurname());
        editor.putString(EMAIL, user.getEmail());
        editor.putString(USERNAME, user.getUsername());
        editor.putString(PASSWORD, user.getPassword());

        editor.apply();
    }

    public void setUserData(ECMUser user){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USERNAME, user.getUser());
        editor.putString(PASSWORD, user.getPassword());

        editor.apply();
    }

    /**
     * Gets current user name.
     * @return name string.
     */
    public String getUserName(){
        return sharedPreferences.getString(NAME, "");
    }

    /**
     * Gets current user surname.
     * @return surname string.
     */
    public String getUserSurname(){
        return sharedPreferences.getString(SURNAME, "");
    }

    /**
     * Gets current user email.
     * @return email string.
     */
    public String getUserEmail(){
        return sharedPreferences.getString(EMAIL, "");
    }

    public String getUserUsername(){
        return sharedPreferences.getString(USERNAME, "");
    }

    public String getUserPassword(){
        return sharedPreferences.getString(PASSWORD, "");
    }

    public void clearUserData(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(NAME, null);
        editor.putString(SURNAME, null);
        editor.putString(EMAIL, null);
        editor.putString(USERNAME, null);
        editor.putString(PASSWORD, null);

        editor.apply();
    }
}
