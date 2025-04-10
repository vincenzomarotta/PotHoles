package com.example.potholes.utils;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.potholes.R;
import com.example.potholes.entities.ECMHoleEvent;

import java.util.ArrayList;
import java.util.List;

public class HoleAdapter extends ArrayAdapter<ECMHoleEvent> {
    ArrayList<ECMHoleEvent> listHoles;
    Context context;

    public HoleAdapter(Context context, @NonNull ArrayList<ECMHoleEvent> listHoles){
        super(context, 0, listHoles);
        this.context = context;
        this.listHoles = listHoles;

    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ECMHoleEvent hole = getItem(position);

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_holes, parent, false);
        }


        TextView longitudeView = convertView.findViewById(R.id.longitudeView);
        TextView latitudeView = convertView.findViewById(R.id.latitudeView);

        longitudeView.setText("Longitude: "+hole.getLongitude());
        latitudeView.setText("Latitude: "+hole.getLatitude());
        return convertView;
    }

    public void newValues(ArrayList<ECMHoleEvent> newListHoles){
        listHoles = newListHoles;
        notifyDataSetChanged();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    /**
     * Get the counts of the items into a list.
     * If the list is null, the count is 0
     * @return items number
     */

    @Override
    public int getCount(){
        if(listHoles == null)
            return 0;
        else
            return listHoles.size();
    }
}
