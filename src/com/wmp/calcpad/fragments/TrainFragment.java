package com.wmp.calcpad.fragments;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ActionBar.LayoutParams;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.wmp.calcpad.CalcPadApplication;
import com.wmp.calcpad.CalcPadEventConstants;
import com.wmp.calcpad.R;
import com.wmp.calcpad.ocr.CalcPadLabelEnum;
import com.wmp.calcpad.ocr.CalcPadOCR;
import com.wmp.calcpad.ocr.CalcPadTrainingData;

public class TrainFragment extends CalcPadCanvasFragment {

	public static final int PAGE_ID = 2;
	
	public static final String TAG = "Train";	
	
	private static boolean InstructionsAlertShown = false; 
	
	private ArrayAdapter<CalcPadLabelEnum> _labelAdapter = null;
	
	private int _previouslySelectedIndex = 0; 
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
    }  
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){    	               
        inflater.inflate(R.menu.menu_train, menu);       
    } 
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.mi_add: {

			if (CalcPadOCR.getSharedInstance().isBusy()) {
				Toast.makeText(this.getActivity(),
						getString(R.string.ocr_is_busy), Toast.LENGTH_SHORT)
						.show();
				return true;
			}							

			Bitmap bitmap = getCanvasBitmap();
			if (bitmap != null) {
				
				// create dialog 
				trainCanvasBitmap(bitmap); 
								
			} else {
				Toast.makeText(this.getActivity(),
						getString(R.string.ocr_error_no_bitmap_available),
						Toast.LENGTH_SHORT).show();
			}

			return true;
		}
		case R.id.mi_flush:{
			if( !CalcPadOCR.getSharedInstance().isReady() ){
				Toast.makeText(this.getActivity(),
						getString(R.string.ocr_is_busy),
						Toast.LENGTH_SHORT).show();
				return true; 
			}
			
			showConfirmation(getResources().getString(R.string.confirm), 
					getResources().getString(R.string.confirm_flush_training_data), 
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							CalcPadOCR.getSharedInstance().FlushTrainingDataAndLabels(CalcPadApplication.getNextAsyncId());
							dialog.dismiss();
						}
					}, 
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});								
			break; 
		}
		case R.id.mi_reset: {
			reset();

			return true;
		}
		case R.id.mi_info:{
			showInstructions(); 
			return true; 
		}
		case R.id.mi_undo:
		{
			if( _sCanvas.undo() ){
				
			}
			return true; 
		}		
		case R.id.mi_redo:
		{
			if( _sCanvas.redo() ){
				
			}
			return true; 
		}
		}
		return false;
	}          
    
    private void trainCanvasBitmap(final Bitmap bitmap){
    	
    	if(_labelAdapter == null){
    		_labelAdapter = ((CalcPadApplication)getActivity().getApplication()).createLabelListAdapater();
    	}    		
    	
    	String title = getResources().getString(R.string.ocr_train_select_label_title); 
    	
    	final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity(), AlertDialog.THEME_HOLO_LIGHT);
		builder.setSingleChoiceItems(_labelAdapter, _previouslySelectedIndex,  null);
		//builder.setTitle(title);
		
		if( _opensansRegularTypeface == null ){
			_opensansRegularTypeface = Typeface.createFromAsset(getActivity().getAssets(),"fonts/opensans_regular.ttf"); 
		}
		
		TextView titleTV = new TextView(this.getActivity());
		titleTV.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		titleTV.setTypeface(_opensansRegularTypeface);
		titleTV.setTextSize(10 * getResources().getDisplayMetrics().density);
		titleTV.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
		titleTV.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
		int hTitleTVPadding = (int)(10 * getResources().getDisplayMetrics().density);  
		int vTitleTVPadding = (int)(5 * getResources().getDisplayMetrics().density);
		titleTV.setPadding(hTitleTVPadding, vTitleTVPadding, hTitleTVPadding, vTitleTVPadding);
		titleTV.setText(title);
		builder.setCustomTitle(titleTV);
		
		//builder.setMessage("Hello world?");		
		builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
				// lock canvas 
				setCanvasEnabled(false);				
				// train 
				CalcPadOCR.getSharedInstance().Train(CalcPadApplication.getNextAsyncId(), bitmap, ((CalcPadLabelEnum)_labelAdapter.getItem(selectedPosition)).label());
				// remember the last choice (assuming the user will work their way sequentially) 
				_previouslySelectedIndex = selectedPosition; 
			}
		});
				
		builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss(); 
			}
		});
		
		AlertDialog ad = builder.show();
		
		//TextView tv = (TextView) ad.findViewById(android.R.id.message);		
		//tv.setTextSize(8f * getResources().getDisplayMetrics().density);
		//tv.setTypeface(_opensansRegularTypeface);
    }

	@Override
	public boolean OnCalcPadEvent(long eventId, String event, Object caller) {
		if( event.equals(CalcPadEventConstants.EVT_TRAINING_DATA_FLUSHED)){
			Toast.makeText(this.getActivity(),
					getString(R.string.ocr_training_data_flushed),
					Toast.LENGTH_SHORT).show();
			return true;
		}
		else if( event.equals(CalcPadEventConstants.EVT_NO_TRAINING_DATA)){
			Toast.makeText(this.getActivity(),
					getString(R.string.ocr_no_data),
					Toast.LENGTH_SHORT).show();
			return true;
		}
		else if( event.equals(CalcPadEventConstants.EVT_TRAINING_COMPLETED)){
			// save to disk 
			CalcPadTrainingData.getSharedInstance().setData(
					CalcPadOCR.getSharedInstance().getTrainingData(), 
						CalcPadOCR.getSharedInstance().getLabels());
			
			CalcPadTrainingData.getSharedInstance().saveData(CalcPadApplication.getNextAsyncId());
			
			return true; 
		}
		else if( event.equals(CalcPadEventConstants.EVT_DATA_SAVED_TO_DISK)){
			setCanvasEnabled(true);
			reset();			
			Toast.makeText(this.getActivity(),
					getString(R.string.ocr_training_data_saved),
					Toast.LENGTH_SHORT).show();
			
			return true;
		}
		return false;
	}	

	@Override
	public boolean hasInstructionAlertBeenShown(){
		return TrainFragment.InstructionsAlertShown;
	}
	
	@Override
	public void setInstructionAlertBeenShown(boolean shown){
		TrainFragment.InstructionsAlertShown = shown; 
	}
	
	@Override
	public String getName(){
		return TAG; 
	}
	
	@Override
	protected String getBundleCanvasBitmapKey(){
		return TAG + "_canvas_bitmap";
	}
	
	@Override
	public String getInstructions(){
		return getResources().getString(R.string.instructions_train); 
	}
	
	@Override
	protected void drawCanvasBackgroundBitmap(Canvas c){
		float density = getResources().getDisplayMetrics().density; 
		
		Paint p = new Paint();
		p.setColor(getResources().getColor(R.color.grid_line));
		p.setStyle(Style.STROKE);
		p.setPathEffect(new DashPathEffect(new float[]{5 * density, 5 * density}, 0));
		p.setStrokeWidth(1);
		
		// cell dimensions (square)
		int cd = (int)(200 / getResources().getDisplayMetrics().density);
		
		// create grid paint 
		int x = (int) (c.getWidth() - cd)/2;
		int y = (int) (c.getHeight() - cd)/2;						
		
		c.drawRect(new Rect(x, y, x + cd, y + cd), p);							
	}
}
