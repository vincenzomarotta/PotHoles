package com.example.potholes.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.util.Log;

import com.example.potholes.R;
import com.example.potholes.callback_inferfaces.ECMUserLoggerCallback;
import com.example.potholes.callback_inferfaces.ECMUserRegisterCallback;
import com.example.potholes.dao.ECMUserLogger;
import com.example.potholes.dao.ECMUserRegister;
import com.example.potholes.entities.ECMServerConnectionInfo;
import com.example.potholes.entities.ECMUser;
import com.example.potholes.entities.User;
import com.example.potholes.fragments.LoginFragment;
import com.example.potholes.fragments.RegistrationFragment;
import com.example.potholes.utils.NetworkAvailable;
import com.example.potholes.utils.RememberMePreferences;
import com.example.potholes.utils.UserDataPreferences;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserAccessActivity extends AppCompatActivity {
    private static final String DEFAULT_SERVER_ADDRESS = "13.94.142.3"; //Azure
    private static final int DEFAULT_SERVER_PORT = 5200;
    private static final String TAG = "UserAccessActivityNetwork";
    private static final int PATTERN_ERROR = 0;
    private static final int EMPTY_ERROR = 1;
    private static final int NOT_SAME = -2;
    private static final int LONG_ERROR = -3;
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()._–[{}]:;',?/*~$^+=<>]).{8,36}$";
    private static final String NAME_SURNAME_PATTERN_ERROR = "[\\p{Punct}+[£°ç§^]+{0-9}]";
    private static final String USERNAME_PATTERN = "^(?=.{4,36}$)(?![.-])(?!.*[.]{2})[a-zA-Z0-9.-]+(?<![.])$";
    private static final int NAME_SURNAME_LENGTH = 64;
    private static final int EMAIL_LENGTH = 254;

    private ECMUser user;
    private User newUser;
    private RememberMePreferences rememberMePreferences;
    private UserDataPreferences userDataPreferences;

    private ECMServerConnectionInfo info;

    private LoginFragment loginFragment;
    private RegistrationFragment registrationFragment;


    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private NetworkAvailable networkAvailable;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_access);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setUserAccessActivity();
        openLoginFragment();
        setNetworkListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    public void openLoginFragment(){
        getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                .replace(R.id.fragmentContainerViewUserAccess, LoginFragment.class, null)
                .addToBackStack(null)
                .commit();
    }

    public void openRegistrationFragment(){
        getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                .replace(R.id.fragmentContainerViewUserAccess, RegistrationFragment.class, null)
                .addToBackStack(null)
                .commit();
    }

    public void setUserAccessActivity(){
        rememberMePreferences = new RememberMePreferences(UserAccessActivity.this);
        userDataPreferences = new UserDataPreferences(UserAccessActivity.this);
        networkAvailable = new NetworkAvailable(UserAccessActivity.this);
        user = new ECMUser();
        newUser = new User();
        loginFragment = new LoginFragment();
        registrationFragment = new RegistrationFragment();
    }

    public void rememberMe(boolean checked) {
        if(checked)
            rememberMePreferences.setRememberMeTrue();
        else
            rememberMePreferences.setRememberMeFalse();
    }

    public void login(Activity myActivity, String user, String password){
        ECMServerConnectionInfo infos = new ECMServerConnectionInfo(DEFAULT_SERVER_ADDRESS, DEFAULT_SERVER_PORT, user, password);

        ECMUserLogger logger = new ECMUserLogger(infos, new ECMUserLoggerCallback() {
            @Override
            public void onLoginOk() {
                runOnUiThread(() -> {
                    Log.d("UserAccessActivity", "Logged in from remember me");
                });

            }

            @Override
            public void onLoginFail() {
                runOnUiThread(() -> {
                    Log.d("UserAccessActivity", "N O T Logged in from remember me");
                    //loginFragment.createToastFail(myActivity);
                });

            }

            @Override
            public void onConnectionError() {
                runOnUiThread(()->{
                    Log.d("UserAccessActivity", "N O T Logged in from remember me");
                    //loginFragment.createToastNoConnection(myActivity);
                });
            }

            @Override
            public void onError() {
                runOnUiThread(() -> {
                    Log.d("UserAccessActivity", "N O T Logged in from remember me");
                    //loginFragment.createToastGeneralError(myActivity);
                });
            }

            @Override
            public void onConnectionLost() {
                //TODO: vedere come gestire.
            }
        });

        logger.login();
    }

    public void login(){
        loginFragment = (LoginFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerViewUserAccess);
        boolean ctrl = false;

        assert loginFragment != null;
        String username = loginFragment.getUsername();
        String password = loginFragment.getPassword();

        if(username == null) {
            loginFragment.setEmptyUsernameError();
            ctrl = true;
        }

        if(password == null){
            loginFragment.setEmptyPasswordError();
            ctrl = true;
        }

        if(ctrl)
            return;

        if(!networkAvailable.isNetworkAvailable()){
            networkAvailable.createAlertNoInternet();
            return;
        }

        user.setUser(username);
        user.setPassword(password);

        loginFragment.setLoginDisabled();

        info = new ECMServerConnectionInfo(DEFAULT_SERVER_ADDRESS, DEFAULT_SERVER_PORT, user.getUser(), user.getPassword());

        ECMUserLogger logger = new ECMUserLogger(info, new ECMUserLoggerCallback() {
            @Override
            public void onLoginOk() {
                runOnUiThread(() -> {
                    loginFragment.setLoginEnabled();
                    loginFragment.clear();
                    userDataPreferences.setUserData(user);
                });
                //TODO: Cambiare HomeActivity con Intro per il primo login
                Intent intent = new Intent(UserAccessActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onLoginFail() {
                runOnUiThread(() -> {
                    loginFragment.setLoginEnabled();
                    loginFragment.createToastFail(UserAccessActivity.this);
                });

            }

            @Override
            public void onConnectionError() {
                runOnUiThread(()->{
                    loginFragment.setLoginEnabled();
                    loginFragment.createToastNoConnection(UserAccessActivity.this);
                });
            }

            @Override
            public void onError() {
                runOnUiThread(() -> {
                    loginFragment.setLoginEnabled();
                    loginFragment.createToastGeneralError(UserAccessActivity.this);
                });
            }

            @Override
            public void onConnectionLost() {
                //TODO: vedere come gestire.
            }
        });

        logger.login();

    }

    public void register(){
        registrationFragment = (RegistrationFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerViewUserAccess);

        try{
            newUser = createUser();
        } catch(Exception e){
            Log.d("UserAccessActivity", "Eccezione -> " + e.getMessage());
            return;
        }

        if(!networkAvailable.isNetworkAvailable()){
            networkAvailable.createAlertNoInternet();
            return;
        }

        registrationFragment.setRegisterDisabled();

        info = new ECMServerConnectionInfo(DEFAULT_SERVER_ADDRESS, 5200);

        ECMUserRegister register = new ECMUserRegister(info, new ECMUserRegisterCallback() {
            @Override
            public void onUserRegistrationOk() {
                runOnUiThread(() -> {
                    registrationFragment.setRegisterEnabled();
                    new AlertDialog.Builder(UserAccessActivity.this)
                            .setTitle("Done!")
                            .setMessage("Registration done!\nLogin using your credentials.")
                            .setPositiveButton("OK", (dialogInterface, i) -> openLoginFragment())
                            .show();
                });
            }

            @Override
            public void onUserRegistrationFail() {
                runOnUiThread(() -> {
                    registrationFragment.setRegisterEnabled();
                    registrationFragment.createToastFail(UserAccessActivity.this);
                });
            }

            @Override
            public void onConnectionError() {
                runOnUiThread(() -> {
                    registrationFragment.setRegisterEnabled();
                    registrationFragment.createToastNoConnection(UserAccessActivity.this);
                });
            }

            @Override
            public void onError() {
                runOnUiThread(() -> {
                    registrationFragment.setRegisterEnabled();
                    registrationFragment.createToastGeneralError(UserAccessActivity.this);
                });

            }

            @Override
            public void onConnectionLost() {
                //TODO: vedere come gestire.
            }
        });

        user = new ECMUser(newUser.getUsername(), newUser.getPassword(), newUser.getEmail(),
                newUser.getName(), newUser.getSurname());

        register.registerUser(user);

    }

    public User createUser() throws Exception {
        User tmp = new User();
        boolean ctrl = false;

        final Pattern pattenNameSurname = Pattern.compile(NAME_SURNAME_PATTERN_ERROR);
        final Pattern patternPassword = Pattern.compile(PASSWORD_PATTERN);
        final Pattern patternUsername = Pattern.compile(USERNAME_PATTERN);

        tmp.setName(registrationFragment.getName());
        if(tmp.getName() == null){
            registrationFragment.setNameError(EMPTY_ERROR);
            ctrl = true;
        } else {
            if (tmp.getName().length() > NAME_SURNAME_LENGTH) {
                registrationFragment.setNameError(LONG_ERROR);
                ctrl = true;
            }

            Matcher matcherName = pattenNameSurname.matcher(tmp.getName());
            if (matcherName.find()) {
                registrationFragment.setNameError(PATTERN_ERROR);
                ctrl = true;
            }
        }

        tmp.setSurname(registrationFragment.getSurname());
        if(tmp.getSurname() == null){
            registrationFragment.setSurnameError(EMPTY_ERROR);
            ctrl = true;
        } else {
            if (tmp.getSurname().length() > NAME_SURNAME_LENGTH) {
                registrationFragment.setSurnameError(LONG_ERROR);
                ctrl = true;
            }

            Matcher matcherSurname = pattenNameSurname.matcher(tmp.getSurname());
            if (matcherSurname.find()) {
                registrationFragment.setSurnameError(PATTERN_ERROR);
                ctrl = true;
            }
        }

        tmp.setUsername(registrationFragment.getUsername());
        if(tmp.getUsername() == null){
            registrationFragment.setUsernameError(EMPTY_ERROR);
            ctrl = true;
        } else {
            Matcher matcherUsername = patternUsername.matcher(tmp.getUsername());
            if (!matcherUsername.find()) {
                registrationFragment.setUsernameError(PATTERN_ERROR);
                ctrl = true;
            }
        }

        tmp.setEmail(registrationFragment.getEmail());
        if(tmp.getEmail() == null){
            registrationFragment.setEmailError(EMPTY_ERROR);
            ctrl = true;
        } else {
            if (tmp.getEmail().length() > EMAIL_LENGTH) {
                registrationFragment.setEmailError(LONG_ERROR);
                ctrl = true;
            }
        }

        tmp.setPassword(registrationFragment.getPassword());
        if(tmp.getPassword() == null){
            registrationFragment.setPasswordError(EMPTY_ERROR);
            ctrl = true;
        } else {
            Matcher matcherPassword = patternPassword.matcher(tmp.getPassword());
            if (!matcherPassword.matches()) {
                registrationFragment.setPasswordError(PATTERN_ERROR);
                ctrl = true;
            }
        }

        String confirmPassword = registrationFragment.getConfirmPassword();
        if(confirmPassword == null){
            registrationFragment.setConfirmPasswordError(EMPTY_ERROR);
            ctrl = true;
        }

        if(!tmp.getPassword().equals(confirmPassword)){
            registrationFragment.setConfirmPasswordError(NOT_SAME);
            ctrl = true;
        }

        if(ctrl)
            throw new IllegalArgumentException();

        return tmp;
    }

    public void userLogout(Context context){
        Log.d("UserAccessActivity", "Log out");
        new RememberMePreferences(context).setRememberMeFalse();
        new UserDataPreferences(context).clearUserData();
    }

    ////////////////////////////////////////////////////

    public void setNetworkListener(){
        connectivityManager = (ConnectivityManager) UserAccessActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                /*runOnUiThread(() -> {
                    Log.e(TAG, "The default network is now: " + network);
                    networkAvailable.createAlertInternetReturned();
                });*/
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> {
                    Log.e(TAG, "The application no longer has a default network. The last default network was " + network);
                    networkAvailable.createAlertNoInternet();
                });
            }
        };


        if(!networkAvailable.isNetworkAvailable()){
            Log.e(TAG,"No internet connection");
            networkAvailable.createAlertNoInternet();
        }
        /**
         * Observer for internet connectivity status live-data
         */

        connectivityManager.registerDefaultNetworkCallback(networkCallback);

    }

}