package com.example.potholes.dao;

import android.util.Log;

import com.example.potholes.callback_inferfaces.ECMAccelerometerThresholdGetterCallback;
import com.example.potholes.callback_inferfaces.ECMReceivedEventCallback;
import com.example.potholes.callback_inferfaces.ECMServerConnectorReadyCallback;
import com.example.potholes.entities.ECMHoleEvent;
import com.example.potholes.entities.ECMServerConnectionInfo;
import com.example.potholes.utils.ECMServerConnector;

import java.util.List;

/**
 * This class is one of the wrappers of the more generic ECMServerDAO class.
 *
 * The class takes care of saving the recovery of the accelerometer threshold value.
 */
public class ECMAccelerometerThresholdGetter {
    private ECMServerConnectionInfo connectionInfo;
    private ECMServerConnector connector;
    private ECMAccelerometerThresholdGetterCallback callback;
    private ECMServerEventDAO dao;

    /**
     * ECMHoleEventSaver class constructor.
     * Receipt of the server status is done via callback.
     * The connectionInfo object must contain the IP address of the server, the port of the server, the identifier of the user and his password.
     *
     * @param connectionInfo
     * @param callback
     */
    public ECMAccelerometerThresholdGetter(ECMServerConnectionInfo connectionInfo, ECMAccelerometerThresholdGetterCallback callback) {
        if (connectionInfo == null)
            throw new NullPointerException("connectionInfo can't be null.");
        if (callback == null)
            throw new NullPointerException("callback can't be null.");

        this.connectionInfo = connectionInfo;
        this.callback = callback;
    }

    /**
     * Retrieves the accelerometer threshold value from the server.
     */
    public void getAccelerometerThreshold() {
        createConnection();
    }

    /**
     * It creates and establishes the connection with the server.
     * Call the setDaoAndExecute method when the connection is established.
     */
    private void createConnection() {
        connector = new ECMServerConnector(connectionInfo.getIp(), connectionInfo.getPort(), new ECMServerConnectorReadyCallback() {

            @Override
            public void onConnectionReady() {
                setDaoAndExecute();
            }

            @Override
            public void onConnectionRefused() {
                callback.onConnectionError();
            }

            @Override
            public void onConnectionError() {
                callback.onConnectionError();
            }

        });
    }

    /**
     * Set the dao by registering at the connector.
     * Start the sequence of communications with the server starting from the check with the latter.
     */
    private void setDaoAndExecute() {
        dao = new ECMServerEventDAO(connector, new ECMReceivedEventCallback() {

            @Override
            public void onCheckOk() {
                dao.sendLoginRequest(connectionInfo.getUser(), connectionInfo.getPassword());
            }

            @Override
            public void onCheckFail() {
                callback.onError();
                dao.sendCloseConnectionAnnouncement();
            }

            @Override
            public void onUserRegistrationOk() {
            }

            @Override
            public void onUserRegistrationFail() {
            }

            @Override
            public void onLoginOk() {
                dao.sendAccelerometerThresholdRequest();
            }

            @Override
            public void onLoginFail() {
                callback.onError();
                dao.sendCloseConnectionAnnouncement();
            }

            @Override
            public void onUserNotLogged() {
                callback.onUserNotLogged();
                dao.sendCloseConnectionAnnouncement();
            }

            @Override
            public void onNearEventReply(List<ECMHoleEvent> eventList) {
            }

            @Override
            public void onNearEventNotFound() {
            }

            @Override
            public void onAccelerometerThresholdReceived(double accelerometerThreshold) {
                Log.wtf("DAO ACC", String.valueOf(accelerometerThreshold));
                callback.onAccelerometerThresholdReceived(accelerometerThreshold);
                dao.sendCloseConnectionAnnouncement();
            }

            @Override
            public void onSaveCoordsOk() {
            }

            @Override
            public void onSaveCoordsFail() {
            }

            @Override
            public void onConnectionCloseConfirmed() {
                endSession();
            }

            @Override
            public void onEchoRequest() {
            }

            @Override
            public void onEchoReply() {
            }

            @Override
            public void onNotValidCommand() {
                callback.onError();
                dao.sendCloseConnectionAnnouncement();
            }

            @Override
            public void onConnectionLost() {
                callback.onConnectionLost();
            }

        });
        dao.sendCheckRequest();
    }

    /**
     * Closes the session by eliminating the callback, finalizing the dao and closing the connection to the server.
     */
    private void endSession() {
        try {
            dao.deleteCallback();
            dao.finalize();
            connector.closeConnection();
            connector = null;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
