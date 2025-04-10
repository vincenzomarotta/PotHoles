package com.example.potholes.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.example.potholes.R;
import com.example.potholes.activities.HomeActivity;
import com.example.potholes.entities.ECMHoleEvent;
import com.example.potholes.presenters.AccelerometerPresenter;
import com.example.potholes.utils.HoleAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ViewHolesFragment extends Fragment {
    //private AccelerometerActivity accelerometerActivity;
    private final HomeActivity homeActivity;
    private AccelerometerPresenter accelerometerPresenter;

    private ListView list;
    private HoleAdapter adapter;

    private ArrayList<ECMHoleEvent> listHoles;

    private TextView emptyList;

    private ConstraintLayout constraintLayout;
    private AnimationDrawable animationDrawable;

    public ViewHolesFragment(AccelerometerPresenter accelerometerPresenter, /*AccelerometerActivity accelerometerActivity*/HomeActivity homeActivity,ArrayList<ECMHoleEvent> listHoles) {
        this.accelerometerPresenter = accelerometerPresenter;
        this.homeActivity = homeActivity;
        this.listHoles = listHoles;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_view_holes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        constraintLayout = (ConstraintLayout) view.findViewById(R.id.constrainLayout_viewHolesFragment);
        animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(4500);
        animationDrawable.setExitFadeDuration(4500);
        animationDrawable.start();


        //Assegnamento ListView
        list = view.findViewById(R.id.listHoles);
        adapter = new HoleAdapter(getContext(), listHoles);

        emptyList = view.findViewById(R.id.empty_holes_list);
        emptyList.setText("No holes found.");
        list.setEmptyView(emptyList);

        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view1, position, id) -> itemClicked(view1, position));
        list.setVisibility(View.INVISIBLE);
        //Assegna Listener
        listenToClickEvents();
    }

    private void listenToClickEvents() {

    }

    public void itemClicked(View view, int position){
        accelerometerPresenter.itemClicked(listHoles.get(position));
    }

    public void resetAdapter(ArrayList<ECMHoleEvent> listHoles) {
        adapter.newValues(listHoles);
    }


    public void showViews(){
        list.setAlpha(0f);
        list.setVisibility(View.VISIBLE);
        list.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null);
    }

    public void hideViews(){
        list.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        list.setVisibility(View.INVISIBLE);
                    }
                });
    }

    public void newValues(ArrayList<ECMHoleEvent>newList){
        adapter.newValues(newList);
    }

    public void noHoleFound(){

    }
}