package com.example.potholes.callback_inferfaces;

public interface ECMServerConnectorReadyCallback {
    void onConnectionReady();
    void onConnectionRefused();
    void onConnectionError();
}
