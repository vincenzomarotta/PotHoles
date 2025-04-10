package com.example.potholes.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.potholes.R;
import com.example.potholes.activities.UserAccessActivity;
import com.example.potholes.utils.MotionToast;
import com.example.potholes.utils.MotionToastType;
import com.google.android.material.textfield.TextInputLayout;

public class RegistrationFragment extends Fragment {

    private static final int PATTERN_ERROR = 0;
    private static final int EMPTY_ERROR = 1;
    private static final int NOT_SAME = -2;
    private static final int LONG_ERROR = -3;

    private EditText name, surname, username, email, password, confirmPassword;
    private TextInputLayout nameLayout, surnameLayout, usernameLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private Button register;


    public RegistrationFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nameLayout = view.findViewById(R.id.nameRegistrationEditText);
        surnameLayout = view.findViewById(R.id.surnameRegistrationEditText);
        usernameLayout = view.findViewById(R.id.usernameRegistrationEditText);
        emailLayout = view.findViewById(R.id.emailRegistrationEditText);
        passwordLayout = view.findViewById(R.id.passwordRegistrationEditText);
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordRegistrationEditText);
        setLayout();
        register = view.findViewById(R.id.registrationButton);
        register.setOnClickListener(view1 -> ((UserAccessActivity) requireActivity()).register());
    }

    /**
     * Gets name from EditText.
     * @return name string or null.
     */
    public String getName() {
        if(name.getText().toString() == null || name.getText().toString().equals(""))
            return null;
        else
            return name.getText().toString();
    }

    /**
     * Gets surname from EditText.
     * @return surname string or null.
     */
    public String getSurname() {
        if(surname.getText().toString() == null || surname.getText().toString().equals(""))
            return null;
        else
            return surname.getText().toString();
    }

    /**
     * Gets username from EditText.
     * @return username string or null.
     */
    public String getUsername() {
        if(username.getText().toString() == null || username.getText().toString().equals(""))
            return null;
        else
            return username.getText().toString();
    }

    /**
     * Gets email from EditText.
     * @return email string or null.
     */
    public String getEmail() {
        if(email.getText().toString() == null || email.getText().toString().equals(""))
            return null;
        else
            return email.getText().toString();
    }

    /**
     * Gets password from EditText.
     * @return password string or null.
     */
    public String getPassword() {
        if(password.getText().toString() == null || password.getText().toString().equals(""))
            return null;
        else
            return password.getText().toString();
    }

    /**
     * Gets confirm password from EditText.
     * @return confirm password string or null.
     */
    public String getConfirmPassword() {
        if(confirmPassword.getText().toString() == null || confirmPassword.getText().toString().equals(""))
            return null;
        else
            return confirmPassword.getText().toString();
    }

    /**
     * Sets Layout of the fragment adding listeners.
     */
    public void setLayout(){
        name = nameLayout.getEditText();
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                nameLayout.setError(null);
                nameLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        surname = surnameLayout.getEditText();
        surname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                surnameLayout.setError(null);
                surnameLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        username = usernameLayout.getEditText();
        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                usernameLayout.setError(null);
                usernameLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        email = emailLayout.getEditText();
        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailLayout.setError(null);
                emailLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        password = passwordLayout.getEditText();
        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordLayout.setError(null);
                passwordLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        confirmPassword = confirmPasswordLayout.getEditText();
        confirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirmPasswordLayout.setError(null);
                confirmPasswordLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * Enables register button.
     */
    public void setRegisterEnabled() {
        register.setEnabled(true);
    }

    /**
     * Disables register button.
     */
    public void setRegisterDisabled() {
        register.setEnabled(false);
    }

    /**
     * Sets error in nameLayout.
     * @param errorType type of the error.
     */
    public void setNameError(int errorType) {
        nameLayout.setErrorEnabled(true);
        switch(errorType){
            case PATTERN_ERROR:
                nameLayout.setError(getString(R.string.user_access_wrong_name_error));
                break;
            case EMPTY_ERROR:
                nameLayout.setError(getString(R.string.user_access_empty_name_error));
                break;
            case LONG_ERROR:
                nameLayout.setError(getString(R.string.user_access_long_name));
                break;
        }

    }

    /**
     * Sets error in surnameLayout.
     * @param errorType type of the error.
     */
    public void setSurnameError(int errorType) {
        surnameLayout.setErrorEnabled(true);
        switch(errorType){
            case PATTERN_ERROR:
                surnameLayout.setError(getString(R.string.user_access_wrong_surname_error));
                break;
            case EMPTY_ERROR:
                surnameLayout.setError(getString(R.string.user_access_empty_surname_error));
                break;
            case LONG_ERROR:
                nameLayout.setError(getString(R.string.user_access_long_surname));
                break;
        }

    }

    /**
     * Sets error in usernameLayout.
     */
    public void setUsernameError(int errorType) {
        usernameLayout.setErrorEnabled(true);
        switch(errorType){
            case PATTERN_ERROR:
                usernameLayout.setError(getString(R.string.user_access_wrong_username_error));
                break;
            case EMPTY_ERROR:
                usernameLayout.setError(getString(R.string.user_access_empty_username_error));
                break;
            case LONG_ERROR:
                nameLayout.setError(getString(R.string.user_access_long_username));
                break;
        }

    }

    /**
     * Sets error in emailLayout.
     */
    public void setEmailError(int errorType) {
        emailLayout.setErrorEnabled(true);
        switch (errorType){
            case EMPTY_ERROR:
                emailLayout.setError(getString(R.string.user_access_empty_email_error));
                break;
            case LONG_ERROR:
                emailLayout.setError(getString(R.string.user_access_long_email));
                break;
        }
    }

    /**
     * Sets error in passwordLayout.
     * @param errorType type of the error.
     */
    public void setPasswordError(int errorType) {
        passwordLayout.setErrorEnabled(true);
        switch(errorType){
            case PATTERN_ERROR:
                passwordLayout.setError(getString(R.string.user_access_wrong_password_error));
                break;
            case EMPTY_ERROR:
                passwordLayout.setError(getString(R.string.user_access_empty_password_error));
                break;
            case LONG_ERROR:
                nameLayout.setError(getString(R.string.user_access_long_password));
                break;
        }
    }

    /**
     * Sets error in confirmPasswordLayout.
     * @param errorType type of the error.
     */
    public void setConfirmPasswordError(int errorType) {
        confirmPasswordLayout.setErrorEnabled(true);
        switch(errorType){
            case NOT_SAME:
                confirmPasswordLayout.setError(getString(R.string.user_access_confirm_password_error));
                break;
            case EMPTY_ERROR:
                confirmPasswordLayout.setError(getString(R.string.user_access_empty_confirm_password_error));
                break;
            case LONG_ERROR:
                nameLayout.setError(getString(R.string.user_access_long_password));
                break;
        }
    }

    /**
     * Creates AlertDialog to show an error.
     * @param context context of the application.
     * @param message message of error.
     */
    public void createErrorAlert(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle("Try Again")
                .setIcon(context.getDrawable(R.drawable.error_icon))
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    public void createToastFail(Activity activity){
        MotionToast.display(activity, R.string.user_access_login_fail, MotionToastType.ERROR_MOTION_TOAST);
    }

    public void createToastNoConnection(Activity activity){
        MotionToast.display(activity, R.string.user_access_no_connection, MotionToastType.ERROR_MOTION_TOAST);
    }

    public void createToastGeneralError(Activity activity){
        MotionToast.display(activity, R.string.user_access_try_again, MotionToastType.ERROR_MOTION_TOAST);
    }




}