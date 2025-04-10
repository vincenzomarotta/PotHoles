package com.example.potholes.callback_inferfaces;

public interface ECMUserRegisterCallback {
    void onUserRegistrationOk();
    void onUserRegistrationFail();
    void onConnectionError();
    void onError();
    void onConnectionLost();
}
