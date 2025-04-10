package com.example.potholes.callback_inferfaces;

import com.example.potholes.entities.ECMHoleEvent;

public interface ECMHoleEventSaverCallback {
    void onSaveCoordsOk(ECMHoleEvent event);
    void onSaveCoordsFail(ECMHoleEvent event);
    void onUserNotLogged();
    void onConnectionError();
    void onError();
    void onConnectionLost();
}
