package com.example.potholes.callback_inferfaces;

import com.example.potholes.entities.ECMHoleEvent;

import java.util.List;

public interface ECMReceivedEventCallback {
    void onCheckOk();
    void onCheckFail();
    void onUserRegistrationOk();
    void onUserRegistrationFail();
    void onLoginOk();
    void onLoginFail();
    void onUserNotLogged();
    void onNearEventReply(List<ECMHoleEvent> eventList);
    void onNearEventNotFound();
    void onAccelerometerThresholdReceived(double accelerometerThreshold);
    void onSaveCoordsOk();
    void onSaveCoordsFail();
    void onConnectionCloseConfirmed();
    void onEchoRequest();
    void onEchoReply();
    void onNotValidCommand();
    void onConnectionLost();
}
