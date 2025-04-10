package com.example.potholes.callback_inferfaces;

import com.example.potholes.entities.ECMHoleEvent;

import java.util.List;

public interface ECMNearHoleEventFinderCallback {
    void onNearEventReply(List<ECMHoleEvent> eventList);
    void onNearEventNotFound();
    void onUserNotLogged();
    void onConnectionError();
    void onError();
    void onConnectionLost();
}
