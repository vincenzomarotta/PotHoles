package com.example.potholes.presenters;

import static android.content.Context.SENSOR_SERVICE;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Vibrator;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.potholes.R;
import com.example.potholes.activities.HomeActivity;
import com.example.potholes.entities.ECMHoleEvent;
import com.example.potholes.fragments.ViewHolesFragment;
import com.example.potholes.utils.MotionToast;
import com.example.potholes.utils.MotionToastType;

import java.util.ArrayList;

public class AccelerometerPresenter implements SensorEventListener {
    private double threshold = 0;

    private ArrayList<ECMHoleEvent> listHoles;

    private final HomeActivity homeActivity;
    private Sensor sensor;
    private SensorManager sensorManager;

    private ViewHolesFragment viewHolesFragment;

    private int Index=0;

    private Vibrator vibe;
    private boolean sensorStatus = false;

    public AccelerometerPresenter(HomeActivity homeActivity) {
        this.homeActivity = homeActivity;
        listHoles = new ArrayList<ECMHoleEvent>();
        //Creazione del sensor manager
    /*
        for(int i=0;i<30;i++){
            listHoles.add(new ECMHoleEvent(i,(double) i, (double) i));
        }
*/
        sensorManager = (SensorManager)this.homeActivity.getSystemService(SENSOR_SERVICE);
        //Sensore Accelerometro
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        setVibratorService();
    }

    public void startSensorListener(){
        sensorManager.registerListener(this,sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopSensorListener(){
        sensorManager.unregisterListener(this, sensor);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //0 = x, 1 = y & 2 = z
        if(threshold <= 0)
            return;

        if (event.values[1] > threshold) {
                Log.wtf("ACCELEROMETER_PRESENTER_VALORE SINGOLO", String.valueOf(threshold));
                Log.wtf("ACCELEROMETER_PRESENTER_VALORE SINGOLO","Accelerometer value - X: "+event.values[0]+", Y: "+event.values[1]+", Z: "+event.values[2]+";" );

                Location location = homeActivity.getCurrentLocation();
                Location lastLocation = new Location("A");
                if(!(listHoles.isEmpty())){
                    for (int i = 0; i < listHoles.size(); i++) {
                        lastLocation.setLatitude(listHoles.get(i).getLatitude());
                        lastLocation.setLongitude(listHoles.get(i).getLongitude());
                        if (lastLocation.distanceTo(location) < 3f){
                            return; }
                    }
                }

                MotionToast.display(
                        homeActivity,
                        R.string.toast_found_hole,
                        MotionToastType.FOUND_HOLE_TOAST
                );

                ECMHoleEvent hole = new ECMHoleEvent(event.values[1], location.getLatitude(), location.getLongitude());

                if(!listHoles.contains(hole)) {
                    listHoles.add(hole);
                    //CHIAMATA PER AGGIUNGERLA ALLA LISTA DI BUCHE
                    viewHolesFragment.newValues(listHoles);
                    //CHIAMATA PER SETTARE BUCA SU MAPPA
                    homeActivity.showHoleEventOnMap(hole);
                    //CHIAMATA PER SALVARE BUCA SUL SERVER
                    homeActivity.addHoleEventToSaveQueue(hole);
                }
            }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //NOT USED
    }


    public void openFragment(Fragment fragment) {
        FragmentManager fragmentManager = homeActivity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.activity_fragment_container, fragment).commit();
    }

    private void closeFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentManager fragmentManager = homeActivity.getSupportFragmentManager();
            fragmentManager.beginTransaction().remove(fragment).commit();
        }
    }

    public void itemClicked(ECMHoleEvent hole){
        Log.d("Accelerometer Presenter","Hole - AccelerometerValue: "+hole.getAccelerometerValue()+" Latitude:"+hole.getLatitude()+" Longitude:"+hole.getLongitude());
    }


    /**
     * Set the click listener for the point recording function.
     */
    public void onButtonSensorClicked(boolean sensorStatus) {
        this.sensorStatus = sensorStatus;
        if(sensorStatus){
            startSensorListener();
        }else{
            stopSensorListener();
        }
        vibe.vibrate(100);
    }


    private void setVibratorService() {
        vibe = (Vibrator) homeActivity.getSystemService(Context.VIBRATOR_SERVICE);
    }


    public ArrayList<ECMHoleEvent> getListHoles() {
        return listHoles;
    }

    public void setListHoles(ArrayList<ECMHoleEvent> listHoles) {
        this.listHoles = listHoles;
    }

    public void stateCollapsed(){
        viewHolesFragment.hideViews();
    }

    public void stateExpanded(){
        if(!listHoles.isEmpty()) {
            viewHolesFragment.showViews();
        }else{
            MotionToast.display(
                    homeActivity,
                    R.string.no_hole_found,
                    MotionToastType.INFO_MOTION_TOAST
            );
            viewHolesFragment.noHoleFound();
        }
    }

    public void stateDragging(){
        viewHolesFragment.showViews();
    }

    public void setViewHolesFragment(ViewHolesFragment viewHolesFragment) {
        this.viewHolesFragment = viewHolesFragment;
    }


    public void showListHoles(ECMHoleEvent eventFound){
        listHoles.add(eventFound);
        viewHolesFragment.newValues(listHoles);
    }

    public void printList(){
        for (ECMHoleEvent holes : listHoles) {
            Log.wtf("ACCELEROMETER_PRESENTER_LISTA_BUCHE", "Accelerometer value - Y: " + holes.getAccelerometerValue() + ", Lat: " + holes.getLatitude() + ", Long: " + holes.getLongitude() + ";");
        }
    }

    public void setThreshold(double accelerometerThreshold) {
        this.threshold = accelerometerThreshold;
    }
}
