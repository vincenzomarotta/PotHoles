package com.example.potholes.entities;

/**
 * The ECMServerConncetionInfo POJO class contains the data useful for connecting to the server.
 * Depending on the needs, it can contain either only the server's ip and port, or it can contain
 * all the information about the server's ip and port, its user ID and password.
 */
public class ECMServerConnectionInfo {
    private String ip;
    private int port;
    private String user;
    private String password;

    /**
     * Builder of the class.
     *
     * @param ip
     * @param port
     */
    public ECMServerConnectionInfo(String ip, int port) {
        if (ip == null)
            throw new NullPointerException("ip can't be null.");
        if (!ip.matches(
                "^(?:(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])\\.){3}(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])$"))
            throw new IllegalArgumentException("ip not valid.");
        if ((port < 0) || (port > 65535))
            throw new IllegalArgumentException("port not valid.");
        this.ip = ip;
        this.port = port;
    }

    /**
     * Builder of the class.
     *
     * @param ip
     * @param port
     * @param user
     * @param password
     */
    public ECMServerConnectionInfo(String ip, int port, String user, String password) {
        this(ip, port);

        if (user == null)
            throw new NullPointerException("user can't be null.");
        if (password == null)
            throw new NullPointerException("password can't be null.");
        this.ip = ip; // = "a.b.c.d/x"; IP DEL SERVER
        this.port = port; // = y; PORTA DEL SERVER
        this.user = user;
        this.password = password;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
