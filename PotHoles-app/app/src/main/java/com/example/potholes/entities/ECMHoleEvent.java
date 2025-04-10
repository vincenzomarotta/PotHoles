package com.example.potholes.entities;

import android.location.Location;

import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * POJO class for hole information.
 */
public class ECMHoleEvent implements Serializable {
    private double accelerometerValue;
    private double latitude;
    private double longitude;

    public ECMHoleEvent(){}

    public ECMHoleEvent(double accelerometerValue, double latitude, double longitude){
        this.accelerometerValue = accelerometerValue;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getAccelerometerValue() {
        return accelerometerValue;
    }
    public void setAccelerometerValue(double accelerometerValue) {
        this.accelerometerValue = accelerometerValue;
    }
    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(!(obj instanceof ECMHoleEvent)) return false;
        if(latitude == ((ECMHoleEvent) obj).getLatitude() && longitude == ((ECMHoleEvent) obj).getLongitude()) return true;
        return false;
    }

}
