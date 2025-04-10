package com.example.potholes.callback_inferfaces;

public interface ECMAccelerometerThresholdGetterCallback {
    void onAccelerometerThresholdReceived(double accelerometerThreshold);
    void onUserNotLogged();
    void onConnectionError();
    void onError();
    void onConnectionLost();
}
