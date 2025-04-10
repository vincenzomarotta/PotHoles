package com.example.potholes.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.potholes.R;
import com.example.potholes.activities.IntroActivity;
import com.example.potholes.utils.ViewPagerAdapterIntro;

import java.util.ArrayList;

import me.relex.circleindicator.CircleIndicator;

public class IntroFragment extends Fragment {
    private ViewPager viewPager;
    private ViewPagerAdapterIntro adapter;
    private Button next;
    private Button back;
    private int currentPage = 0;
    private IntroActivity introActivity = new IntroActivity();
    private ArrayList<Fragment> fragmentArrayList = new ArrayList<>();

    public IntroFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro, container, false);
        setFragmentIntro(view);
        return view;
    }

    /**
     * Sets the fragment using listeners.
     * The buttons will change name in every fragment screen.
     * The next button in the last fragment will open HomeActivity.
     *
     * @param view view of the fragment.
     */
    public void setFragmentIntro(View view){
        next = view.findViewById(R.id.nextIntroButton);
        back = view.findViewById(R.id.backIntroButton);
        next.setOnClickListener(v -> {
            Log.i("UI_INTERACTION","Cliccato pulsante Next.");
            if(currentPage == (fragmentArrayList.size() - 1))
                ((IntroActivity) requireActivity()).openHome();
            else
                viewPager.setCurrentItem(currentPage + 1);
        });

        back.setOnClickListener(v -> {
            Log.i("UI_INTERACTION", "Cliccato pulsante Back.");
            viewPager.setCurrentItem(currentPage - 1);
        });

        setFragmentsArrayList();

        adapter = new ViewPagerAdapterIntro(
                requireActivity().getSupportFragmentManager(),
                fragmentArrayList
        );

        viewPager = view.findViewById(R.id.viewPagerIntro);
        CircleIndicator circleIndicator = view.findViewById(R.id.circleIndicator);
        viewPager.setAdapter(adapter);

        ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentPage = position;

                if(position == 0){
                    next.setEnabled(true);
                    back.setEnabled(false);
                    back.setVisibility(View.INVISIBLE);

                    next.setText("Next");

                } else if(position == (fragmentArrayList.size() - 1)){
                    next.setEnabled(true);
                    back.setEnabled(true);
                    back.setVisibility(View.VISIBLE);

                    next.setText("Finish");
                    back.setText("Back");
                } else {
                    next.setEnabled(true);
                    back.setEnabled(true);
                    back.setVisibility(View.VISIBLE);

                    next.setText("Next");
                    back.setText("Back");
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };


        viewPager.addOnPageChangeListener(viewListener);
        circleIndicator.setViewPager(viewPager);
        adapter.registerDataSetObserver(circleIndicator.getDataSetObserver());
    }

    /**
     * Sets fragment ArrayList.
     */
    public void setFragmentsArrayList(){
        fragmentArrayList.add(new FirstIntroScreen());
        fragmentArrayList.add(new SecondIntroScreen());
        fragmentArrayList.add(new ThirdIntroScreen());
    }


}