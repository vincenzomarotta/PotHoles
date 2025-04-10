package com.example.potholes.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.example.potholes.R;

import androidx.annotation.NonNull;

public class NetworkAvailable {
    Context context;

    /**
     * Creates NetworkAvailable instance, used to check if there is active connection.
     * @param context context of the application used to get SystemService.
     */
    public NetworkAvailable(@NonNull Context context){
        this.context = context;
    }

    /**
     * Checks if there is active connection.
     * @return true if there is connection, false if there is not.
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Creates error alert if there is no connection.
     */
    public void createAlertInternetReturned(){
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.internet_connection_back))
                .setMessage(context.getString(R.string.internet_connection_back_message))
                .setIcon(context.getDrawable(R.drawable.refresh_icon))
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
    public void createAlertNoInternet(){
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.internet_check_connection))
                .setMessage(context.getString(R.string.internet_no_connection_message))
                .setIcon(context.getDrawable(R.drawable.error_icon))
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
