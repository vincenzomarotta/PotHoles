package com.example.potholes.callback_inferfaces;

public interface ECMServerConnectorReceiverCallback {
    void onReceived(byte[] payload, int payloadSize);
}
