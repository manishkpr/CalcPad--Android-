package com.wmp.calcpad.activities;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.wmp.calcpad.R;
import com.wmp.calcpad.fragments.AboutFragment;
import com.wmp.calcpad.fragments.CalculatorFragment;
import com.wmp.calcpad.fragments.TestFragment;
import com.wmp.calcpad.fragments.TrainFragment;

public class CalcPadActivity extends FragmentActivity implements ActionBar.TabListener {
    
	/**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
     * keep every loaded fragment in memory. If this becomes too memory intensive, it may be best
     * to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter _sectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager _viewPager;		
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
                                  
        setContentView(R.layout.activity_calcpadtab);
        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        _sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up the ViewPager with the sections adapter.
        _viewPager = (ViewPager) findViewById(R.id.pager);
        _viewPager.setAdapter(_sectionsPagerAdapter);

        // When swiping between different sections, select the corresponding tab.
        // We can also use ActionBar.Tab#select() to do this if we have a reference to the
        // Tab.
        _viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < _sectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(_sectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }                                
    }
    
    @Override
    public void onDestroy(){    	    	
    	super.onDestroy();
    }
    
    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Hold on to this
    	_menu = menu;
        
        // Inflate the currently selected menu XML resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(MENU_RESOURCES[_viewPager.getCurrentItem()], menu);                
        
        return true;
    } */       
    
    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {        
        return false; 
    }*/
	
	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction arg1) {
		// When the given tab is selected, switch to the corresponding page in the ViewPager.
		_viewPager.setCurrentItem(tab.getPosition());
		
		invalidateOptionsMenu();
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub		
	}			
	
	@Override
	protected void onSaveInstanceState(Bundle outState){						
		super.onSaveInstanceState(outState);	
		
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		
		//_calculatorFragment = (CalculatorFragment)getSupportFragmentManager().getFragment(savedInstanceState, CalculatorFragment.TAG);
	}
	
	
	
	/**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public static class SectionsPagerAdapter extends FragmentPagerAdapter {
    	    	
    	
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);            
        }
        
        @Override
        public Fragment getItem(int i) {
        	switch( i){
        	case CalculatorFragment.PAGE_ID:        		        		
        		return new CalculatorFragment();
        	case TestFragment.PAGE_ID:
        		return new TestFragment();
        	case TrainFragment.PAGE_ID:
        		return new TrainFragment(); 
        	case AboutFragment.PAGE_ID:
        		return new AboutFragment(); 
        	}
        	
        	return null; 
        }
        
        @Override
        public int getCount() {
        	return 4;  
        }

        @Override
        public CharSequence getPageTitle(int position) {        	
        	switch( position ){
        	case CalculatorFragment.PAGE_ID:
        		return CalculatorFragment.TAG;
        	case TestFragment.PAGE_ID:
        		return TestFragment.TAG;
        	case TrainFragment.PAGE_ID:
        		return TrainFragment.TAG; 
        	case AboutFragment.PAGE_ID:
        		return AboutFragment.TAG;
        	}                 
        	
        	return "Unknown";
        }
    }    
}
