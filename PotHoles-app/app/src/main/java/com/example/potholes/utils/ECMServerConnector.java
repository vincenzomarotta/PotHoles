package com.example.potholes.utils;

import android.util.Log;

import com.example.potholes.callback_inferfaces.ECMServerConnectorReadyCallback;
import com.example.potholes.callback_inferfaces.ECMServerConnectorReceiverCallback;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class deals with sending / receiving data from the server and recognizing the packets of the
 * ECM protocol.
 */
public class ECMServerConnector {
    private Socket socket;
    private List<ECMServerConnectorReceiverCallback> callbackList;
    private Thread receiverThread;
    private Thread senderThread;
    private DataInputStream input;
    private DataOutputStream output;

    public ECMServerConnector(String ip, int port, ECMServerConnectorReadyCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(ip, port);
                    input = new DataInputStream(socket.getInputStream());
                    output = new DataOutputStream(socket.getOutputStream());
                    while (!socket.isConnected())
                        ;
                    startReceiver();
                    callback.onConnectionReady();
                } catch (ConnectException e) {
                    callback.onConnectionRefused();
                } catch (IOException e2) { // UnknownHostException viene catturato qui.
                    callback.onConnectionError();
                }
            }
        }).start();
    }

    /**
     * Register callbacks for handling data reception.
     *
     * @param callback
     */
    public synchronized void registerECMServerConnectorReceiverCallback(ECMServerConnectorReceiverCallback callback) {
        if (callback == null)
            throw new NullPointerException("callback can't be null.");

        if (callbackList == null)
            callbackList = Collections.synchronizedList(new LinkedList<>());
        synchronized (callbackList) {
            callbackList.add(callback);
        }
    }

    /**
     * Notify all registered callback interfaces of receipt of new data.
     *
     * @param payload
     * @param payloadSize
     */
    private void notifyAllReceiverCallback(byte[] payload, int payloadSize) {
        if (callbackList != null) {
            synchronized (callbackList) {
                for (ECMServerConnectorReceiverCallback callback : callbackList)
                    callback.onReceived(payload, payloadSize);
            }
        }
        Log.d("ECM_CONNECTOR", "Ho notificato le callback. [" + Integer.toString(callbackList.size()) + "]");
    }

    /**
     * Deletes the specified callback interface.
     *
     * @param callback
     */
    public synchronized void unregisterCallback(ECMServerConnectorReceiverCallback callback) {
        if (callbackList.contains(callback))
            callbackList.remove(callback);
    }

    /**
     * Start the thread dedicated to receiving messages.
     */
    private void startReceiver() {
        receiverThread = new Thread(() -> {
            while ((socket.isConnected()) && (!socket.isClosed()) && (!Thread.currentThread().isInterrupted())) {
                try {
                    if (input.available() > 0) {
                        //Modificato perché readNBytes non è supportato in android.
                        //byte[] protocol = input.readNBytes(11);
                        byte[] protocol = new byte[11];
                        input.read(protocol, 0, 11);
                        System.out.println("Protocol -> " + new String(protocol));
                        if (new String(protocol).equals("ECM_PROT_V1")) {
                            byte[] temp = new byte[4];
                            input.read(temp, 0, 4); //off:11
                            /*ByteBuffer packetSizeBuffer = ByteBuffer.wrap(input.readNBytes(4))
                                    .order(ByteOrder.LITTLE_ENDIAN);*/
                            ByteBuffer packetSizeBuffer = ByteBuffer.wrap(temp)
                                    .order(ByteOrder.LITTLE_ENDIAN);
                            int payloadSize = packetSizeBuffer.getInt();
                            Log.d("ECM_CONNECTOR", "Packet len -> " + Integer.toString(payloadSize));
                            //byte[] payload = input.readNBytes(payloadSize);
                            byte[] payload = new byte[payloadSize];
                            input.read(payload, 0, payloadSize);//off:15
                            Log.d("ECM_CONNECTOR", "Payload -> " + new String(payload));
                            notifyAllReceiverCallback(payload, payloadSize);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        receiverThread.start();
    }

    /**
     * Sends data on the specified socket.
     *
     * @param dataToSend
     */
    public void send(byte[] dataToSend) {
        senderThread = new Thread(() -> {
            try {
                synchronized (output) {
                    output.write(dataToSend);
                    output.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        senderThread.start();
    }

    /**
     * Closes the socket.
     */
    public void closeConnection() {
        try {
            input.close();
            socket.close();
            receiverThread.interrupt();
            if (senderThread.isAlive())
                senderThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}