package com.example.potholes.fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.potholes.R;
import com.example.potholes.activities.HomeActivity;
import com.example.potholes.callback_inferfaces.HomeMapsFragmentReadyCallback;
import com.example.potholes.entities.ECMHoleEvent;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment management class containing the interactive Google page.
 */
public class HomeMapsFragment extends Fragment {
    private boolean fragmentIsReady = false;
    private HomeMapsFragmentReadyCallback fragmentReadyCallback;

    private GoogleMap gMap;
    private Marker currentPositionMarker;

    private List<Marker> holeMarkerList = new ArrayList<>();

    private boolean anchorCurrentPosition = true;

    private Location lastLocation;
    private float lastRotationRead = 0;

    private OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            gMap = googleMap;
            gMap.getUiSettings().setCompassEnabled(true);
            gMap.getUiSettings().setMapToolbarEnabled(true);
            setMapCallbacks();
            fragmentIsReady = true;
            notifyHomeMapsFragmentIsReady();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    /**
     * Set the map callback interfaces.
     */
    private void setMapCallbacks() {
        gMap.setOnCameraMoveListener(() -> anchorCurrentPosition = false);
        gMap.setOnMapLongClickListener(latLng -> {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= 29)
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.EFFECT_HEAVY_CLICK));
            else
                vibrator.vibrate(200);
            Location clickedLocation = new Location("");
            clickedLocation.setLatitude(latLng.latitude);
            clickedLocation.setLongitude(latLng.longitude);
            ((HomeActivity) requireActivity()).getNearHoleEventFromServer(clickedLocation);
        });
    }

    /**
     * Register the callback to notify that the fragment is ready.
     *
     * @param callback
     */
    public void registerHomeMapsFragmentReadyCallback(HomeMapsFragmentReadyCallback callback) {
        fragmentReadyCallback = callback;
        if (fragmentIsReady)
            fragmentReadyCallback.onFragmentReady();
    }

    /**
     * Notify that the fragment is ready and the map is loaded.
     */
    private void notifyHomeMapsFragmentIsReady() {
        if (fragmentReadyCallback != null)
            fragmentReadyCallback.onFragmentReady();
    }

    /**
     * Converts a Drawable object to MarkerIcon.
     *
     * @param drawable
     * @return
     */
    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * Displays the user's current location on the map.
     *
     * @param location
     */
    public void setCurrentLocation(Location location) {
        if (currentPositionMarker == null) {
            Drawable arrowDrawable = getResources().getDrawable(R.drawable.current_position_icon);
            BitmapDescriptor arrowIcon = getMarkerIconFromDrawable(arrowDrawable);

            currentPositionMarker = gMap.addMarker(new MarkerOptions()
                    .position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .draggable(false)
                    //.rotation(location.getBearing())
                    .icon(arrowIcon)
                    .zIndex(Float.MAX_VALUE)
                    .flat(true));
            //.icon(BitmapDescriptorFactory.fromResource(R.drawable.navigation_arrow)));
        } else {

            currentPositionMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
            currentPositionMarker.setFlat(true);
            //currentPositionMarker.setRotation(location.getBearing());
        }
        lastLocation = location;

        if (anchorCurrentPosition)
            gMap.moveCamera(CameraUpdateFactory
                    .newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
    }

    /**
     * Adds the marker on the map to indicate a hole.
     *
     * @param event
     */
    public void addHoleMarker(ECMHoleEvent event) {
        if (event == null)
            throw new NullPointerException("event can't be null.");

        Drawable potholeDrawable = getResources().getDrawable(R.drawable.potholes_icon);
        BitmapDescriptor potholeIcon = getMarkerIconFromDrawable(potholeDrawable);

        Marker marker = gMap.addMarker(new MarkerOptions()
                .position(new LatLng(event.getLatitude(), event.getLongitude()))
                .title("Acc. value -> " + Double.toString(event.getAccelerometerValue()))
                .icon(potholeIcon)
                .draggable(false));
        holeMarkerList.add(marker);
    }

    /**
     * Add markers on the map from a list.
     *
     * @param eventList
     */
    public void addHoleMarker(List<ECMHoleEvent> eventList) {
        if (eventList == null)
            throw new NullPointerException("eventList can't be null.");
        if (eventList.isEmpty())
            return;

        for (ECMHoleEvent event : eventList) {
            addHoleMarker(event);
        }
    }

    /**
     * Lock the current location.
     *
     * @param value
     */
    public void anchorCurrentPosition(boolean value, Location location) {
        anchorCurrentPosition = value;

        if((anchorCurrentPosition) && (location != null)) {
            gMap.moveCamera(CameraUpdateFactory
                    .newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
        }
    }

    /**
     * Set the position arrow bearing.
     *
     * @param value
     */
    public void setCurrentPositionArrowBearing(float value) {
        if ((value < lastRotationRead - 10f) || (value > lastRotationRead + 10f)) {
            lastRotationRead = value;
            if (currentPositionMarker != null)
                currentPositionMarker.setRotation(value);
        }

    }
}