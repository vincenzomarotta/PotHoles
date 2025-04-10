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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.example.potholes.R;
import com.example.potholes.activities.UserAccessActivity;
import com.example.potholes.entities.User;
import com.example.potholes.utils.MotionToast;
import com.example.potholes.utils.MotionToastType;
import com.google.android.material.textfield.TextInputLayout;


public class LoginFragment extends Fragment {

    private EditText username;
    private EditText password;
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private Button login;
    private CheckBox rememberMe;
    private Button signUp;



    public LoginFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        usernameLayout = view.findViewById(R.id.usernameLoginEditText);
        passwordLayout = view.findViewById(R.id.passwordLoginEditText);
        setLayouts();
        login = view.findViewById(R.id.loginButton);
        login.setOnClickListener(view12 -> ((UserAccessActivity) requireActivity()).login());
        rememberMe = view.findViewById(R.id.rememberMeCheckBox);
        rememberMe.setOnClickListener(v -> {
            if(rememberMe.isChecked())
                ((UserAccessActivity) requireActivity()).rememberMe(true);
            else if(!rememberMe.isChecked())
                ((UserAccessActivity) requireActivity()).rememberMe(false);
        });
        signUp = view.findViewById(R.id.newAccountButton);
        signUp.setOnClickListener(view1 -> ((UserAccessActivity) requireActivity()).openRegistrationFragment());
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Gets email from EditText.
     * @return email string or null.
     */
    public String getUsername() {
        if(String.valueOf(username.getText()).equals("") || String.valueOf(username.getText()) == null)
            return null;
        return String.valueOf(username.getText());
    }

    /**
     * Gets password from EditText.
     * @return email string or null.
     */
    public String getPassword() {
        if(String.valueOf(password.getText()).equals("") || String.valueOf(password.getText()) == null){
            return null;
        }
        return String.valueOf(password.getText());
    }

    /**
     * Sets error in emailLayout.
     */
    public void setEmptyUsernameError() {
        usernameLayout.setErrorEnabled(true);
        usernameLayout.setError(getString(R.string.user_access_empty_username_error));
    }

    /**
     * Sets error in passwordLayout.
     */
    public void setEmptyPasswordError() {
        passwordLayout.setErrorEnabled(true);
        passwordLayout.setError(getString(R.string.user_access_empty_password_error));
    }

    /**
     * Sets the layout, adding listeners.
     */
    public void setLayouts(){
        username = usernameLayout.getEditText();
        password = passwordLayout.getEditText();
        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                usernameLayout.setError(null);
                usernameLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

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
    }

    /**
     * Clears the EditTexts.
     */
    public void clear() {
        username.setText("");
        password.setText("");
        rememberMe.setSelected(false);
    }

    /**
     * Create an error alert.
     * @param context context of the application.
     * @param message error message.
     */
    public void createErrorAlert(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.user_access_try_again))
                .setMessage(message)
                .setIcon(context.getDrawable(R.drawable.error_icon))
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

    /**
     * Enables login button.
     */
    public void setLoginEnabled() {
        login.setEnabled(true);
    }

    /**
     * Disables login button.
     */
    public void setLoginDisabled() {
        login.setEnabled(false);
    }

}