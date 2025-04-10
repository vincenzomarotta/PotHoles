package com.example.potholes.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.potholes.R;
import com.example.potholes.callback_inferfaces.ECMAccelerometerThresholdGetterCallback;
import com.example.potholes.callback_inferfaces.ECMHoleEventSaverCallback;
import com.example.potholes.callback_inferfaces.ECMNearHoleEventFinderCallback;
import com.example.potholes.callback_inferfaces.HomeMapsFragmentReadyCallback;
import com.example.potholes.dao.ECMAccelerometerThresholdGetter;
import com.example.potholes.dao.ECMHoleEventSaver;
import com.example.potholes.dao.ECMNearHoleEventFinder;
import com.example.potholes.entities.ECMHoleEvent;
import com.example.potholes.entities.ECMServerConnectionInfo;
import com.example.potholes.fragments.HomeMapsFragment;
import com.example.potholes.fragments.ViewHolesFragment;
import com.example.potholes.presenters.AccelerometerPresenter;
import com.example.potholes.utils.NetworkAvailable;
import com.example.potholes.utils.UserDataPreferences;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is the main controller of the application.
 */
public class HomeActivity extends AppCompatActivity implements SensorEventListener {
    private static final String DEFAULT_SERVER_ADDRESS = "13.94.142.3"; //Azure
    private static final int DEFAULT_SERVER_PORT = 5200;

    private static final String TAG = "HomeActivityNetwork";

    private HomeMapsFragment homeMapsFragment;
    private ViewHolesFragment viewHolesFragment;
    private AccelerometerPresenter accelerometerPresenter;

    private boolean mapsFragmentIsReady = false;

    private static final int REFRESH_LOCATION_INTERVAL = 15;
    private static final int REFRESH_LOCATION_FASTEST_INTERVAL = 10;
    private ActivityResultLauncher<String[]> locationPermissionRequest;//Vanno creati prima in onCreate
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private SensorManager sensorManager;
    private float[] accelerometerReads;
    private float[] magneticReads;

    private static final int DEFAULT_CIRCLE_RANGE = 10; //Km
    private static final int DEFAULT_CIRCLE_APPROX = 1; //Km
    private Location lastNearEventReferencePosition;
    private Location currentLocation;

    private ECMServerConnectionInfo connectionInfo;
    private List<ECMHoleEvent> holeEventList = new ArrayList<>(); //Lista di tutte le buche caricate e trovate.
    private ECMHoleEventSaver eventSaver;
    private BlockingQueue<ECMHoleEvent> holeEventSaveQueue = new LinkedBlockingQueue<>(); //Lista delle buche da salvare.

    private FloatingActionButton sensorButton;
    private boolean sensorStatus = false;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    NetworkAvailable networkAvailable = new NetworkAvailable(HomeActivity.this);

    private ImageView locationIcon;

    private BottomSheetBehavior bottomSheetBehavior;
    private FrameLayout frameLayout;

    private AnimationDrawable animationDrawable;
    private Animation clockwise, anticlockwise;


    private UserDataPreferences userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        setConnectionInfo();

        setNetworkListener();

        accelerometerPresenter = new AccelerometerPresenter(this);

        setButtonClickListener();

        setMyLocationIcon();

        /******/
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        /******/

        homeMapsFragment = new HomeMapsFragment();
        homeMapsFragment.registerHomeMapsFragmentReadyCallback(new HomeMapsFragmentReadyCallback() {
            @Override
            public void onFragmentReady() {
                mapsFragmentIsReady = true;
            }
        });
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.activity_fragment_map_container, homeMapsFragment, null)
                .setReorderingAllowed(true)
                .commit();
        setActivityResultLauncherForLocationPermission();
        setBottomSheet();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    @Override
    protected void onDestroy() {
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        if ((eventSaver != null) && (eventSaver.getSavingRun()))
            eventSaver.stop();
        super.onDestroy();
    }

    /**
     * Set the information for connecting to the remote server.
     */
    private void setConnectionInfo() {
        userPreferences = new UserDataPreferences(this);
        connectionInfo = new ECMServerConnectionInfo(DEFAULT_SERVER_ADDRESS, DEFAULT_SERVER_PORT, userPreferences.getUserUsername(), userPreferences.getUserPassword());
    }

    /**
     * Request for localization permits.
     */
    private void setActivityResultLauncherForLocationPermission() {
        locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_COARSE_LOCATION, false);
                            if ((fineLocationGranted != null && fineLocationGranted) || (coarseLocationGranted != null && coarseLocationGranted)) {
                                Log.d("LOCATION", "Permessi concessi.");
                                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                                getAndShowLastPositionOnMap();
                                setLocationRequest();
                                setLocationCallback();
                            } else {
                                Log.d("LOCATION", "Permessi negati.");
                                locationServiceInfoAlert();
                                sensorButton.hide();
                                locationIcon.setVisibility(View.GONE);
                            }
                        }
                );
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    /**
     * Alert for location permissions not granted.
     */
    private void locationServiceInfoAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.home_activity_alert_info_location_service_title)
                .setMessage(R.string.home_activity_alert_info_location_service_message)
                .setPositiveButton("Ok", (dialog, which) -> {
                })
                .create().show();
    }

    /**
     * Retrieve the last known position from the system and view on the map.
     */
    private void getAndShowLastPositionOnMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("LOCATION", "Sono nell'if del controllo dei permessi");
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null)
                        Log.d("LOCATION", "Last location -> " + location.getLatitude() + "-" + location.getLongitude());
                    lastNearEventReferencePosition = location;
                    new Thread(() -> {
                        while (!mapsFragmentIsReady) ;
                        if (location != null)
                            runOnUiThread(() -> {
                                homeMapsFragment.setCurrentLocation(location);
                                lastNearEventReferencePosition = location;
                                getNearHoleEventFromServer(location);
                            });
                    }).start();
                }
            });
        }
    }

    /**
     * Set the localization request to the system.
     */
    private void setLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(REFRESH_LOCATION_INTERVAL);
        locationRequest.setFastestInterval(REFRESH_LOCATION_FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Set up the callback interface to handle updating of positions.
     */
    private void setLocationCallback() {
        Log.d("LOCATION", "Sono in setLocationCallback");
        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (mapsFragmentIsReady) {
                    Location newLocation = locationResult.getLastLocation();
                    currentLocation = newLocation;
                    runOnUiThread(() -> homeMapsFragment.setCurrentLocation(newLocation));
                    if (lastNearEventReferencePosition == null)
                        lastNearEventReferencePosition = newLocation;
                    if (lastNearEventReferencePosition.distanceTo(newLocation) > (DEFAULT_CIRCLE_RANGE - DEFAULT_CIRCLE_APPROX)) {
                        lastNearEventReferencePosition = newLocation;
                        getNearHoleEventFromServer(newLocation);
                    }
                    Log.d("LOCATION", "New location -> " + newLocation.getLatitude() + "-" + newLocation.getLongitude());
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            accelerometerReads = sensorEvent.values;
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            magneticReads = sensorEvent.values;
        if ((accelerometerReads != null) && (magneticReads != null)) {
            float[] r = new float[9];
            float[] i = new float[9];
            if (SensorManager.getRotationMatrix(r, i, accelerometerReads, magneticReads)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(r, orientation);
                int azimut = (int) Math.round((Math.toDegrees(orientation[0]) + 360) % 360);
                homeMapsFragment.setCurrentPositionArrowBearing(Math.round((Math.toDegrees(orientation[0]) + 360) % 360));
                //Log.wtf("DEGREE", Integer.toString(azimut));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    /**
     * Return to your current location.
     *
     * @return location
     */
    public Location getCurrentLocation() {
        return currentLocation;
    }

    /**
     * Adds the details of the hole to be saved to the save queue.
     *
     * @param event
     */
    public void addHoleEventToSaveQueue(ECMHoleEvent event) {
        holeEventSaveQueue.add(event);
    }

    /**
     * View the hole on the map.
     *
     * @param event
     */
    public void showHoleEventOnMap(ECMHoleEvent event) {
        homeMapsFragment.addHoleMarker(event);
    }

    /**
     * Retrieve from the server the holes around the position indicated by parameter.
     *
     * @param location
     */
    public void getNearHoleEventFromServer(Location location) {
        Log.d("GET_NEAR_HOLE", "Sono in near hole event");
        ECMNearHoleEventFinder finder = new ECMNearHoleEventFinder(connectionInfo, new ECMNearHoleEventFinderCallback() {
            @Override
            public void onNearEventReply(List<ECMHoleEvent> eventList) {
                for (ECMHoleEvent ee : eventList)
                    Log.d("EVENT", ee.toString());
                new Thread(() -> {
                    synchronized (holeEventList) {
                        for (ECMHoleEvent event : eventList) {
                            if (!holeEventList.contains(event)) {
                                holeEventList.add(event);
                                runOnUiThread(() -> {
                                    showHoleEventOnMap(event);
                                    accelerometerPresenter.showListHoles(event);
                                });
                                //TODO: far vedere a Mattia come aggiungere le buche caricate dal server nella lista a schermo. //DA CONTROLLARE SE FUNZIONA
                            }
                        }
                    }
                }).start();
            }

            @Override
            public void onNearEventNotFound() {
                runOnUiThread(() -> Toast.makeText(HomeActivity.this, getResources().getString(R.string.home_activity_toast_event_not_found), Toast.LENGTH_LONG).show());
                Log.d("GET_NEAR_HOLE", "Event not found.");
            }

            @Override
            public void onUserNotLogged() {
                //TODO vedere come gestire.
                Log.d("GET_NEAR_HOLE", "User not logged.");
            }

            @Override
            public void onConnectionError() {
                showConnectionErrorAlert();
            }

            @Override
            public void onError() {
                showGenericErrorAlert();
            }

            @Override
            public void onConnectionLost() {
                //TODO: vedere come gestire.
            }
        });
        finder.getNearHoleEvents(new ECMHoleEvent(0d, location.getLatitude(), location.getLongitude()), DEFAULT_CIRCLE_RANGE);
    }

    /**
     * Set the click listener for the point recording function.
     */
    private void setButtonClickListener() {
        sensorButton = (FloatingActionButton) findViewById(R.id.floatingButtonSensor);

        clockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.floatingbutton_clockwise);
        anticlockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.floatingbutton_anticlockwise);

        sensorButton.setImageResource(R.drawable.play_icon);
        sensorButton.setClickable(true);
        sensorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorStatus = !sensorStatus;

                if (!sensorStatus) {
                    sensorButton.startAnimation(anticlockwise);
                    sensorButton.setImageResource(R.drawable.play_icon);
                    accelerometerPresenter.printList();
                    stopHoleEventSaver();

                } else {
                    sensorButton.startAnimation(clockwise);
                    sensorButton.setImageResource(R.drawable.pause_icon);
                    getAccelerometerThreshold();
                }
                accelerometerPresenter.onButtonSensorClicked(sensorStatus);
            }
        });
    }

    /**
     * Retrieves the threshold value for the accelerometer from the server.
     */
    private void getAccelerometerThreshold() {
        ECMAccelerometerThresholdGetter getter = new ECMAccelerometerThresholdGetter(connectionInfo, new ECMAccelerometerThresholdGetterCallback() {
            @Override
            public void onAccelerometerThresholdReceived(double accelerometerThreshold) {
                Log.wtf("HOME ACC", String.valueOf(accelerometerThreshold));

                accelerometerPresenter.setThreshold(accelerometerThreshold);
                startHoleEventSaver();
                //TODO: controllare se funziona
            }

            @Override
            public void onUserNotLogged() {
                //TODO vedere come gestire.
            }

            @Override
            public void onConnectionError() {
                runOnUiThread(() -> showConnectionErrorAlert());
            }

            @Override
            public void onError() {
                runOnUiThread(() -> showGenericErrorAlert());
            }

            @Override
            public void onConnectionLost() {
                //TODO: vedere come gestire.
            }
        });
        getter.getAccelerometerThreshold();
    }

    /**
     * Start the thread that takes care of saving the new holes on the server.
     */
    private void startHoleEventSaver() {
        eventSaver = new ECMHoleEventSaver(connectionInfo, holeEventSaveQueue, new ECMHoleEventSaverCallback() {
            @Override
            public void onSaveCoordsOk(ECMHoleEvent event) {
                //TODO vedere come gestire.
            }

            @Override
            public void onSaveCoordsFail(ECMHoleEvent event) {
                //TODO vedere come gestire.
            }

            @Override
            public void onUserNotLogged() {
                //TODO vedere come gestire.
            }

            @Override
            public void onConnectionError() {
                runOnUiThread(() -> showConnectionErrorAlert());
            }

            @Override
            public void onError() {
                runOnUiThread(() -> showGenericErrorAlert());
            }

            @Override
            public void onConnectionLost() {
                //TODO: vedere come gestire.
            }
        });
        new Thread(eventSaver).start();
    }

    /**
     * Stop the thread that takes care of saving new holes on the server.
     */
    private void stopHoleEventSaver() {
        if (eventSaver != null) {
            eventSaver.stop();
            eventSaver = null;
        }
    }

    /**
     * Set my location button
     */
    private void setMyLocationIcon() {
        locationIcon = findViewById(R.id.my_location_icon);

        locationIcon.setOnClickListener(view -> runOnUiThread(() -> homeMapsFragment.anchorCurrentPosition(true, currentLocation)));
    }

    /**
     * Set the bottom sheet.
     */
    private void setBottomSheet() {
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.activity_fragment_container);

        bottomSheetBehavior = BottomSheetBehavior.from(frameLayout);
        bottomSheetBehavior.setPeekHeight(100);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        sensorButton.show();
                        accelerometerPresenter.stateCollapsed();
                        locationIcon.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        sensorButton.hide();
                        accelerometerPresenter.stateExpanded();
                        locationIcon.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
        viewHolesFragment = new ViewHolesFragment(accelerometerPresenter, this, accelerometerPresenter.getListHoles());
        accelerometerPresenter.setViewHolesFragment(viewHolesFragment);
        accelerometerPresenter.openFragment(viewHolesFragment);
    }

    /**
     * Displays the dialog for the network error.
     */
    private void showConnectionErrorAlert() {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            builder.setTitle(R.string.home_activity_alert_warning_title)
                    .setMessage(R.string.home_activity_alert_connection_error)
                    .setPositiveButton("Ok", (dialog, which) -> {
                    })
                    .create();
        });
    }

    /**
     * Displays the dialog for generic errors.
     */
    private void showGenericErrorAlert() {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.home_activity_alert_error_title)
                    .setMessage(R.string.home_activity_alert_generic_error)
                    .setPositiveButton("Ok", (dialog, which) -> {
                    })
                    .create();
        });
    }

    /**
     * Log out of the user.
     *
     * @param item
     */
    public void logout(MenuItem item) {
        new UserAccessActivity().userLogout(HomeActivity.this);
        startActivity(new Intent(this, UserAccessActivity.class));
        finish();
    }

    /**
     * Starts the application help activity.
     *
     * @param item
     */
    public void help(MenuItem item) {
        startActivity(new Intent(HomeActivity.this, IntroActivity.class));
    }


    public void setNetworkListener() {
        /**
         * Observer for internet connectivity status live-data
         */
        connectivityManager = (ConnectivityManager) HomeActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
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


        if (!networkAvailable.isNetworkAvailable()) {
            Log.e(TAG, "No internet connection");
            networkAvailable.createAlertNoInternet();
        }
        /**
         * Observer for internet connectivity status live-data
         */

        connectivityManager.registerDefaultNetworkCallback(networkCallback);


    }
}