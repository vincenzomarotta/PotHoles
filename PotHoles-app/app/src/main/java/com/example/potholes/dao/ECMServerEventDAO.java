package com.example.potholes.dao;

import android.util.Log;

import com.example.potholes.callback_inferfaces.ECMReceivedEventCallback;
import com.example.potholes.callback_inferfaces.ECMServerConnectorReceiverCallback;
import com.example.potholes.entities.ECMHoleEvent;
import com.example.potholes.entities.ECMUser;
import com.example.potholes.utils.ECMServerConnector;

import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class deals with the handling of ECM protocol events.
 * The class manages the requests to the server and allows the management of the received data.
 */
public class ECMServerEventDAO {
    private static final String PROTOCOL_HEADING = "ECM_PROT_V1";
    private static final int PROTOCOL_HEADING_SIZE = 11;
    private static final int INTEGER_SIZE = 4;
    private static final int DOUBLE_SIZE = 8;

    private ECMReceivedEventCallback callback;
    private ECMServerConnector connector;
    private ECMServerConnectorReceiverCallback connectorCallback;

    private ScheduledExecutorService echoCheckExecutorService;
    private AtomicBoolean echoCheckReceived = new AtomicBoolean(true);

    public ECMServerEventDAO(ECMServerConnector connector, ECMReceivedEventCallback callback) {
        if (connector == null)
            throw new NullPointerException("connector can't be null.");
        if (callback == null)
            throw new NullPointerException("callback can't be null.");

        this.callback = callback;
        this.connector = connector;
        setServerConnector();
        setEchoCheck();
    }

    @Override
    protected void finalize() throws Throwable {
        echoCheckExecutorService.shutdown();
        connector.unregisterCallback(connectorCallback);
    }

    /**
     * Sets the callback interface of the ECMServerConnector instance passed in the constructor of
     * this class.
     */
    private void setServerConnector() {
        connectorCallback = new ECMServerConnectorReceiverCallback() {

            @Override
            public void onReceived(byte[] payload, int payloadSize) {
                ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
                int commandLen = 0;
                while (payload[commandLen] != '\n')
                    commandLen++;
                byte[] command = new byte[commandLen];
                buffer.get(command, 0, commandLen);
                String commandString = new String(command);
                Log.d("ECMServerEventDAO", "Command -> " + commandString + "|");
                buffer.get(); //Serve per togliere \n dopo aver rimosso il comando dal buffer.

                switch (commandString) {
                    case "CHECK_OK":
                        receivedCheckOk();
                        break;
                    case "CHECK_FAIL":
                        receivedCheckFail();
                        break;
                    case "USR_REG_OK":
                        receivedUserReagistrationOk();
                        break;
                    case "USR_REG_FAIL":
                        receivedUserRegistrationFail();
                        break;
                    case "LOGIN_OK":
                        receivedLoginOk();
                        break;
                    case "LOGIN_FAIL":
                        receivedLoginFail();
                        break;
                    case "USR_NOT_LOGGED":
                        receivedUserNotLogged();
                        break;
                    case "NEAR_EVENT_REPLY":
                        receivedNearEventReply(buffer);
                        break;
                    case "NEAR_EVENT_NOT_FOUND":
                        receivedNearEventNotFound();
                        break;
                    case "ACCELEROMETER_RESPONSE":
                        receivedAccelerometerThreshold(buffer);
                        break;
                    case "SAVE_COORDS_OK":
                        receivedSaveCoordsOk();
                        break;
                    case "SAVE_COORDS_FAIL":
                        receivedSaveCoordsFail();
                        break;
                    case "CONN_CLOSE_OK":
                        receivedCloseConnectionConfirmed();
                        break;
                    case "ECHO_REQUEST":
                        receivedEchoRequest();
                        break;
                    case "ECHO_REPLY":
                        receivedEchoReply();
                        break;
                    default:
                        callback.onNotValidCommand();
                        break;
                }
            }

        };
        connector.registerECMServerConnectorReceiverCallback(connectorCallback);
    }

    /**
     * Set the performer for periodic echo checks.
     */
    void setEchoCheck(){
        echoCheckExecutorService = Executors.newSingleThreadScheduledExecutor();
        echoCheckExecutorService.scheduleAtFixedRate(() -> {
            if(!echoCheckReceived.get()){
                callback.onConnectionLost();
                echoCheckExecutorService.shutdown();
                return;
            }
            echoCheckReceived.set(false);
            sendEcho();
        }, 2, 2, TimeUnit.SECONDS);
    }

    /**
     * Replaces the currently registered callback interface.
     * @param callback
     */
    public void replaceECMReceivedEventCallback(ECMReceivedEventCallback callback) {
        if(callback == null)
            throw new NullPointerException("callback can't be null.");
        this.callback = callback;
    }

    /**
     * Delete the currently registered callback interface.
     */
    public void deleteCallback(){
        callback = null;
    }

    /**
     * Notification of receipt of a positive server availability response.
     */
    private void receivedCheckOk() {
        callback.onCheckOk();
    }

    /**
     * Notifies the receipt of a negative server availability response.
     */
    private void receivedCheckFail() {
        callback.onCheckFail();
    }

    /**
     * Notification that the new user has been successfully registered.
     */
    private void receivedUserReagistrationOk() {
        callback.onUserRegistrationOk();
    }

    /**
     * Notification that the registration of the new user has failed.
     */
    private void receivedUserRegistrationFail() {
        callback.onUserRegistrationFail();
    }

    /**
     * Notifies that the user login was successful and returns the threshold value for the accelerometer.
     */
    private void receivedLoginOk() {
        callback.onLoginOk();
    }

    /**
     * Notification that the user's login has failed.
     */
    private void receivedLoginFail() {
        callback.onLoginFail();
    }

    /**
     * Notification that the user is not logged in.
     */
    private void receivedUserNotLogged() {
        callback.onUserNotLogged();
    }

    /**
     * It notifies the receipt of the position of the holes and returns the ECMHoleEvent list of the holes themselves.
     *
     * @param residualBuffer
     */
    private void receivedNearEventReply(ByteBuffer residualBuffer) {
        List<ECMHoleEvent> eventList = new LinkedList<>();
        int eventNumber = residualBuffer.getInt();
        double accelerometer;
        double latitude;
        double longitude;
        for (int i = 0; i < eventNumber; i++) {
            // Non cambiare la seguente disposizione di get.
            latitude = residualBuffer.getDouble();
            longitude = residualBuffer.getDouble();
            accelerometer = residualBuffer.getDouble();
            eventList.add(new ECMHoleEvent(accelerometer, latitude, longitude));
        }
        callback.onNearEventReply(eventList);
    }

    /**
     * Notification that there are no holes in the area.
     */
    private void receivedNearEventNotFound() {
        callback.onNearEventNotFound();
    }

    /**
     * Notifies the receipt of the accelerometer threshold value.
     * @param residualBuffer
     */
    private void receivedAccelerometerThreshold(ByteBuffer residualBuffer) {
        callback.onAccelerometerThresholdReceived(residualBuffer.getDouble());
    }

    /**
     * Notification that the position of the holes has been successfully saved.
     */
    private void receivedSaveCoordsOk() {
        callback.onSaveCoordsOk();
    }

    /**
     * Notification that saving the hole position has failed.
     */
    private void receivedSaveCoordsFail() {
        callback.onSaveCoordsFail();
    }

    /**
     * Notification that the server has received the connection closure notification.
     */
    private void receivedCloseConnectionConfirmed() {
        callback.onConnectionCloseConfirmed();
    }

    /**
     * Notify the receipt of an echo reply request.
     */
    private void receivedEchoRequest() {
        callback.onEchoRequest();
    }

    /**
     * Notify the receipt of a response to an echo request.
     */
    private void receivedEchoReply() {
        echoCheckReceived.set(true);
        callback.onEchoReply();
    }

    /**
     * Submit a server availability request.
     */
    public void sendCheckRequest() {
        String command = "CHECK\n";
        int payloadSize = command.length();
        // ByteBuffer buffer = ByteBuffer.allocate(21).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer buffer = ByteBuffer.allocate(PROTOCOL_HEADING_SIZE + INTEGER_SIZE + payloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        try {
            buffer.put(PROTOCOL_HEADING.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(payloadSize);
            buffer.put(command.getBytes(StandardCharsets.US_ASCII));
            connector.send(buffer.array());
        } catch (BufferUnderflowException | ReadOnlyBufferException e1) {
            System.out.println("Exception -> " + e1.getMessage());
            e1.printStackTrace();
        }
    }

    /**
     * Send a request to register a new user.
     *
     * @param user
     */
    public void sendUserReagistration(ECMUser user) {
        String command = "USR_REG_REQUEST\n";
        int payloadSize = command.length() + user.getTotalSize();
        ByteBuffer buffer = ByteBuffer.allocate(PROTOCOL_HEADING_SIZE + INTEGER_SIZE + payloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        try {
            buffer.put(PROTOCOL_HEADING.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(payloadSize);
            buffer.put(command.getBytes(StandardCharsets.US_ASCII));
            buffer.put(user.getAllAttributesByteArray());
            connector.send(buffer.array());
        } catch (ReadOnlyBufferException | BufferUnderflowException e) {
            System.out.println("Exception -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Submit a login request.
     *
     * @param user
     * @param password
     */
    public void sendLoginRequest(String user, String password) {
        if (user == null)
            throw new NullPointerException("user can't be null.");
        if (user.length() == 0)
            throw new IllegalArgumentException("user length can't be 0.");
        if (password == null)
            throw new NullPointerException("password can't be null.");
        if (password.length() == 0)
            throw new IllegalArgumentException("password length can't be 0.");

        String command = "LOGIN_REQUEST\n";
        int payloadSize = command.length() + user.length() + 1 + password.length() + 1; // I +1 sono i "\n".
        ByteBuffer buffer = ByteBuffer.allocate(PROTOCOL_HEADING_SIZE + INTEGER_SIZE + payloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        try {
            buffer.put(PROTOCOL_HEADING.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(payloadSize);
            buffer.put(command.getBytes(StandardCharsets.US_ASCII));
            buffer.put(user.concat("\n").getBytes(StandardCharsets.US_ASCII));
            buffer.put(password.concat("\n").getBytes(StandardCharsets.US_ASCII));
            connector.send(buffer.array());
        } catch (ReadOnlyBufferException | BufferUnderflowException e) {
            System.out.println("Exception -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send a request to obtain the list of holes with respect to a position indicated by an ECMHoleEvent object and in the radius indicated by maxRange (in Km).
     *
     * @param event
     * @param maxRange
     */
    public void sendNearEventRequest(ECMHoleEvent event, int maxRange) {
        if (event == null)
            throw new NullPointerException("event can't be null.");
        if (maxRange < 0)
            throw new IllegalArgumentException("maxRange can't be negative.");

        String command = "NEAR_EVENT_REQUEST\n";
        int payloadSize = command.length() + INTEGER_SIZE + (3 * DOUBLE_SIZE); // maxRange + object size.
        ByteBuffer buffer = ByteBuffer.allocate(PROTOCOL_HEADING_SIZE + INTEGER_SIZE + payloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        try {
            buffer.put(PROTOCOL_HEADING.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(payloadSize);
            buffer.put(command.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(maxRange);
            buffer.putDouble(event.getLatitude());
            buffer.putDouble(event.getLongitude());
            buffer.putDouble(event.getAccelerometerValue());
            connector.send(buffer.array());
        } catch (ReadOnlyBufferException | BufferUnderflowException e) {
            System.out.println("Exception -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send the request to get the accelerometer threshold value.
     */
    public void sendAccelerometerThresholdRequest() {
        String command = "ACCELEROMETER_REQUEST\n";
        int payloadSize = command.length();
        ByteBuffer buffer = ByteBuffer.allocate(PROTOCOL_HEADING_SIZE + Integer.BYTES + payloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        try {
            buffer.put(PROTOCOL_HEADING.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(payloadSize);
            buffer.put(command.getBytes(StandardCharsets.US_ASCII));
            connector.send(buffer.array());
        } catch (BufferUnderflowException | ReadOnlyBufferException e) {
            System.out.println("Exception -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send the request to save a new hole.
     * @param event
     */
    public void sendSaveRequest(ECMHoleEvent event) {
        if (event == null)
            throw new NullPointerException("event can't be null.");

        String command = "SAVE_COORDS_REQUEST\n";
        int payloadSize = command.length() + INTEGER_SIZE + (3 * DOUBLE_SIZE);
        ByteBuffer buffer = ByteBuffer.allocate(PROTOCOL_HEADING_SIZE + INTEGER_SIZE + payloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        try {
            buffer.put(PROTOCOL_HEADING.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(payloadSize);
            buffer.put(command.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(1);
            buffer.putDouble(event.getLatitude());
            buffer.putDouble(event.getLongitude());
            buffer.putDouble(event.getAccelerometerValue());
            connector.send(buffer.array());
        } catch (ReadOnlyBufferException | BufferUnderflowException e) {
            System.out.println("Exception -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send the save request and the list of holes to save.
     *
     * @param eventList
     */
    public void sendSaveRequest(List<ECMHoleEvent> eventList) {
        if (eventList == null)
            throw new NullPointerException("eventList can't be null.");
        if (eventList.size() == 0)
            throw new IllegalArgumentException("eventList must have at least one point.");

        String command = "SAVE_COORDS_REQUEST\n";
        int payloadSize = command.length() + INTEGER_SIZE + (eventList.size() * 3 * DOUBLE_SIZE);
        ByteBuffer buffer = ByteBuffer.allocate(PROTOCOL_HEADING_SIZE + INTEGER_SIZE + payloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        try {
            buffer.put(PROTOCOL_HEADING.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(payloadSize);
            buffer.put(command.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(eventList.size());
            for (ECMHoleEvent event : eventList) {
                buffer.putDouble(event.getLatitude());
                buffer.putDouble(event.getLongitude());
                buffer.putDouble(event.getAccelerometerValue());
            }
            connector.send(buffer.array());
        } catch (ReadOnlyBufferException | BufferUnderflowException e) {
            System.out.println("Exception -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send the connection closure announcement.
     */
    public void sendCloseConnectionAnnouncement() {
        String command = "CONN_CLOSE_ANNOUNCEMENT\n";
        int payloadSize = command.length();
        ByteBuffer buffer = ByteBuffer.allocate(PROTOCOL_HEADING_SIZE + INTEGER_SIZE + payloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        try {
            buffer.put(PROTOCOL_HEADING.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(payloadSize);
            buffer.put(command.getBytes(StandardCharsets.US_ASCII));
            connector.send(buffer.array());
        } catch (BufferUnderflowException | ReadOnlyBufferException e1) {
            System.out.println("Exception -> " + e1.getMessage());
            e1.printStackTrace();
        }
    }

    /**
     * Send an echo request.
     */
    public void sendEcho() {
        String command = "ECHO_REQUEST\n";
        int payloadSize = command.length();
        ByteBuffer buffer = ByteBuffer.allocate(PROTOCOL_HEADING_SIZE + INTEGER_SIZE + payloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        try {
            buffer.put(PROTOCOL_HEADING.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(payloadSize);
            buffer.put(command.getBytes(StandardCharsets.US_ASCII));
            connector.send(buffer.array());
        } catch (BufferUnderflowException | ReadOnlyBufferException e1) {
            System.out.println("Exception -> " + e1.getMessage());
            e1.printStackTrace();
        }
    }
}
