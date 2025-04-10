package com.example.potholes.entities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * POJO class for user information.
 */
public class ECMUser {
    private String user;
    private String password;
    private String email;
    private String name;
    private String surname;

    public ECMUser() {
    }

    public ECMUser(String user, String password, String email, String name, String surname) {
        if (user == null)
            throw new NullPointerException("user can't be null.");
        if (password == null)
            throw new NullPointerException("password can't be null.");
        if (email == null)
            throw new NullPointerException("email can't be null.");
        if (name == null)
            throw new NullPointerException("name can't be null.");
        if (surname == null)
            throw new NullPointerException("surname can't be null.");

        if (user.length() == 0)
            throw new IllegalArgumentException("user length can't be 0.");
        if (password.length() == 0)
            throw new IllegalArgumentException("password length can't be 0.");
        if (email.length() == 0)
            throw new IllegalArgumentException("email length can't be 0.");
        if (name.length() == 0)
            throw new IllegalArgumentException("name length can't be 0.");
        if (surname.length() == 0)
            throw new IllegalArgumentException("surname length can't be 0.");

        this.user = user + "\n";
        this.password = password + "\n";
        this.email = email + "\n";
        this.name = name + "\n";
        this.surname = surname + "\n";
    }

    @Override
    public String toString() {
        return new String(user + password + email + name + surname);
    }

    public int getTotalSize() {
        return user.length() + password.length() + email.length() + name.length() + surname.length();
    }

    public byte[] getAllAttributesByteArray() {
        int arraySize = getTotalSize();
        ByteBuffer buffer = ByteBuffer.allocate(arraySize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(user.getBytes());
        buffer.put(password.getBytes());
        buffer.put(email.getBytes());
        buffer.put(name.getBytes());
        buffer.put(surname.getBytes());
        return buffer.array();
    }

    public String getUser() {
        return user.substring(0, user.length() - 1);
    }

    public void setUser(String user) {
        if (user == null)
            throw new NullPointerException("user can't be null.");
        if (user.length() == 0)
            throw new IllegalArgumentException("user length can't be 0.");
        this.user = user + "\n";
    }

    public String getPassword() {
        return password.substring(0, password.length() - 1);
    }

    public void setPassword(String password) {
        if (password == null)
            throw new NullPointerException("password can't be null.");
        if (password.length() == 0)
            throw new IllegalArgumentException("password length can't be 0.");
        this.password = password + "\n";
    }

    public String getEmail() {
        return email.substring(0, email.length() - 1);
    }

    public void setEmail(String email) {
        if (email == null)
            throw new NullPointerException("email can't be null.");
        if (email.length() == 0)
            throw new IllegalArgumentException("email length can't be 0.");
        this.email = email + "\n";
    }

    public String getName() {
        return name.substring(0, name.length() - 1);
    }

    public void setName(String name) {
        if (name == null)
            throw new NullPointerException("name can't be null.");
        if (name.length() == 0)
            throw new IllegalArgumentException("name length can't be 0.");
        this.name = name + "\n";
    }

    public String getSurname() {
        return surname.substring(0, surname.length() - 1);
    }

    public void setSurname(String surname) {
        if (surname == null)
            throw new NullPointerException("surname can't be null.");
        if (surname.length() == 0)
            throw new IllegalArgumentException("surname length can't be 0.");
        this.surname = surname + "\n";
    }

}
