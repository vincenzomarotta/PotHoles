package com.example.potholes.utils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

public class ViewPagerAdapterIntro extends FragmentStatePagerAdapter {

    private ArrayList<Fragment> fragmentArrayList = new ArrayList<>();


    public ViewPagerAdapterIntro(@NonNull FragmentManager fragmentManager, ArrayList<Fragment> fragmentArrayList) {
        super(fragmentManager);
        this.fragmentArrayList.addAll(fragmentArrayList);
    }

    /**
     * Return the correct fragment of the IntroFragment.
     * @param position position of the fragment.
     * @return fragment.
     */
    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragmentArrayList.get(position);
    }

    @Override
    public int getCount() {
        return fragmentArrayList.size();
    }
}

