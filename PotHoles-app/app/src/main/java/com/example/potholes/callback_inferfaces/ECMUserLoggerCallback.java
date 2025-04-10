package com.example.potholes.callback_inferfaces;

public interface ECMUserLoggerCallback {
    void onLoginOk();
    void onLoginFail();
    void onConnectionError();
    void onError();
    void onConnectionLost();
}
