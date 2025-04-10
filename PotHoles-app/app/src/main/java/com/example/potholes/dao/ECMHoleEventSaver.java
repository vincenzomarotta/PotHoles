package com.example.potholes.dao;

import com.example.potholes.callback_inferfaces.ECMHoleEventSaverCallback;
import com.example.potholes.callback_inferfaces.ECMReceivedEventCallback;
import com.example.potholes.callback_inferfaces.ECMServerConnectorReadyCallback;
import com.example.potholes.entities.ECMHoleEvent;
import com.example.potholes.entities.ECMServerConnectionInfo;
import com.example.potholes.utils.ECMServerConnector;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is one of the wrappers of the more generic ECMServerDAO class.
 * The class takes care of saving the saving of ECMHoleEvent events on the server, fetching the events from a BlockingQueue.
 * It is advisable to run the class on a specific thread; for this purpose this class extends Runanble.
 */
public class ECMHoleEventSaver implements Runnable {
    private ECMServerConnectionInfo connectionInfo;
    private ECMServerConnector connector;
    private ECMHoleEventSaverCallback callback;
    private ECMServerEventDAO dao;

    private boolean savingRun = false;

    private BlockingQueue<ECMHoleEvent> saveQueue;
    private ECMHoleEvent currentEvent;

    private AtomicBoolean connectionReady = new AtomicBoolean(false);
    private AtomicBoolean saving = new AtomicBoolean(false);
    private final Object waitingObject = new Object();

    /**
     * ECMHoleEventSaver class constructor.
     * Receipt of the server status is done via callback.
     * The connector must already be instantiated and the socket must already be connected.
     * The connectionInfo object must contain the IP address of the server, the port of the server, the identifier of the user and his password.
     *
     * @param connectionInfo
     * @param saveQueue
     * @param callback
     */
    public ECMHoleEventSaver(ECMServerConnectionInfo connectionInfo, BlockingQueue<ECMHoleEvent> saveQueue,
                             ECMHoleEventSaverCallback callback) {
        if (connectionInfo == null)
            throw new NullPointerException("connectionInfo can't be null.");
        if (saveQueue == null)
            throw new NullPointerException("saveQueue can't be null.");
        if (callback == null)
            throw new NullPointerException("callback can't be null.");

        this.connectionInfo = connectionInfo;
        this.saveQueue = saveQueue;
        this.callback = callback;
    }

    /**
     * Start connecting and saving events in the list.
     */
    @Override
    public void run() {
        createConnection();
        while (!connectionReady.get()) {
            try {
                synchronized (waitingObject) {
                    waitingObject.wait();
                }
            } catch (InterruptedException e) {
                //e.printStackTrace();
                break;
            }
        }

        savingRun = true;

        while (!Thread.currentThread().isInterrupted() && savingRun) {
            try {
                currentEvent = saveQueue.take();
            } catch (InterruptedException e) {
                //e.printStackTrace();
                break;
            }

            //Controllo del valore fake per interromepere il ciclo.
            if((currentEvent.getAccelerometerValue() == Double.MIN_VALUE) &&
                    (currentEvent.getLatitude() == Double.MIN_VALUE) &&
                    (currentEvent.getLongitude() == Double.MIN_VALUE)) {
                dao.sendCloseConnectionAnnouncement();
                break;
            }

            saving.set(true);

            dao.sendSaveRequest(currentEvent);

            while (saving.get()) {
                try {
                    synchronized (waitingObject) {
                        waitingObject.wait();
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    break;
                }
            }
        }
    }

    /**
     * It creates and establishes the connection with the server.
     * Call the setDaoAndExecute method when the connection is established.
     */
    private void createConnection() {
        connector = new ECMServerConnector(connectionInfo.getIp(), connectionInfo.getPort(),
                new ECMServerConnectorReadyCallback() {

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
                connectionReady.set(true);
                synchronized (waitingObject) {
                    waitingObject.notifyAll();
                }
            }

            @Override
            public void onLoginFail() {
                callback.onUserNotLogged();
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
            }

            @Override
            public void onSaveCoordsOk() {
                saving.set(false);
                callback.onSaveCoordsOk(currentEvent);

                synchronized (waitingObject) {
                    waitingObject.notifyAll();
                }
            }

            @Override
            public void onSaveCoordsFail() {
                saving.set(false);
                callback.onSaveCoordsFail(currentEvent);
                synchronized (waitingObject) {
                    waitingObject.notifyAll();
                }
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
     * Stop the save system and close the connection.
     */
    public void stop() {
        //Valore fake per interromepere il cliclo principale.
        saveQueue.add(new ECMHoleEvent(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE));
        //dao.sendCloseConnectionAnnouncement();
    }

    /**
     * Force stop saving and close the connection.
     */
    public void forceStop(){
        savingRun = false;
        saveQueue.clear();
        endSession();
    }

    /**
     * Closes the session by eliminating the callback, finalizing the dao and closing the connection to the server.
     */
    private void endSession() {
        try {
            savingRun = false;
            dao.deleteCallback();
            dao.finalize();
            connector.closeConnection();
            connector = null;
            //Valore fake per interromepere il cliclo principale.
            //saveQueue.add(new ECMHoleEvent(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the value of the state of saving in progress.
     *
     * @return
     */
    public boolean getSavingRun(){
        return savingRun;
    }
}
