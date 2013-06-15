package com.wmp.calcpad.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wmp.calcpad.R;

public class AboutFragment extends Fragment {

	public static final int PAGE_ID = 3;
	
	public static final String TAG = "About";
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState){
    	super.onCreateView(inflater, container, savedInstanceState);
    	
        // Inflate your layout
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        
        TextView tv = (TextView)view.findViewById(R.id.wmp_footer_note);
        
        if( tv != null ){
        	Typeface tf = Typeface.createFromAsset(getActivity().getAssets(),"fonts/opensans_regular.ttf"); 
        	tv.setTypeface(tf);
        }

        return view;
    }

}
