package com.wmp.calcpad;

import org.opencv.android.OpenCVLoader;

import com.wmp.calcpad.interrupter.CalcPadInterrupter;
import com.wmp.calcpad.messaging.CalcPadSubscriber;
import com.wmp.calcpad.messaging.ICalcPadObserver;
import com.wmp.calcpad.messaging.ICalcPadSubscriber;
import com.wmp.calcpad.ocr.CalcPadLabelEnum;
import com.wmp.calcpad.ocr.CalcPadOCR;
import com.wmp.calcpad.ocr.CalcPadTrainingData;

import android.app.Application;
import android.app.ActionBar.LayoutParams;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Base64DataException;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

public class CalcPadApplication extends Application implements ICalcPadObserver, ICalcPadSubscriber {
	
	private static long _calcPadAsyncEventId = 0; 
	
	public static long getNextAsyncId(){
		return _calcPadAsyncEventId++; 
	}
	
	private CalcPadSubscriber _subscriber = null;
	
	private CalcPadOCR _ocrInstance; 
	private CalcPadTrainingData _trainingDataInstance; 
	private CalcPadInterrupter _parser; 
	
	private Typeface _opensansRegularTypeface = null; 
	
	private static final CalcPadLabelEnum[] _labels = new CalcPadLabelEnum[]{
			// numbers 
			CalcPadLabelEnum.Label_0, CalcPadLabelEnum.Label_1, CalcPadLabelEnum.Label_2, 
			CalcPadLabelEnum.Label_3, CalcPadLabelEnum.Label_4, CalcPadLabelEnum.Label_5, 
			CalcPadLabelEnum.Label_6, CalcPadLabelEnum.Label_7, CalcPadLabelEnum.Label_8, 
			CalcPadLabelEnum.Label_9, CalcPadLabelEnum.Label_DOT,
			// simple operations 
			CalcPadLabelEnum.Label_Plus, CalcPadLabelEnum.Label_Minus, CalcPadLabelEnum.Label_Multiply, 
			CalcPadLabelEnum.Label_Divide,
			CalcPadLabelEnum.Label_Pow, CalcPadLabelEnum.Label_Sqrt, 							
			// variables 
			CalcPadLabelEnum.Label_x, CalcPadLabelEnum.Label_y,			
			// constants
			CalcPadLabelEnum.Label_PI,			
			// 
			CalcPadLabelEnum.Label_Open_Prenthesis, CalcPadLabelEnum.Label_Closed_Prenthesis,
			// algebra 
			CalcPadLabelEnum.Label_Function, CalcPadLabelEnum.Label_Summation, 
			// assignment 
			CalcPadLabelEnum.Label_Equals, 	
	};
	
	public static CalcPadLabelEnum getCalcPadLabelEnumWithLabel(int label){
		int count = _labels.length; 
		for( int i=0; i<count; i++ ){
			if( _labels[i].label() == label ){
				return _labels[i]; 
			}
		}
		
		return CalcPadLabelEnum.Unknown;
	}
	
	public static String getCalcPadLabelEnumAsCharWithLabel(int label){
		int count = _labels.length; 
		for( int i=0; i<count; i++ ){
			if( _labels[i].label() == label ){
				return Character.toString(_labels[i].toChar()); 
			}
		}
		
		return CalcPadLabelEnum.Unknown.toString();
	}
	
	static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }
	
	@Override
	public void onCreate (){
		super.onCreate(); 
		
		_subscriber = new CalcPadSubscriber(); 
		
		loadTrainingData();					
		
		_parser = CalcPadInterrupter.initSharedInstance(this, _labels);
		_parser.addObserver(this);
	}
	
	private void loadTrainingData(){
		_ocrInstance = CalcPadOCR.initSharedInstance(this);
		_trainingDataInstance = CalcPadTrainingData.initSharedInstance(this);
		
		_ocrInstance.addObserver(this);
		_trainingDataInstance.addObserver(this);
		
		_trainingDataInstance.loadData(CalcPadApplication.getNextAsyncId());		
	}
	
	public void refreshTrainingData(){
		if( _ocrInstance != null ){
			_ocrInstance.FlushTrainingDataAndLabels(CalcPadApplication.getNextAsyncId()); 
			_ocrInstance.TrainFromDisk(CalcPadApplication.getNextAsyncId());
		}
	}
	
	@Override
	public void onLowMemory (){		
		super.onLowMemory(); 
	}
	
	@Override
	public void onTerminate (){
		// destroy singletons 
		CalcPadOCR.killSharedInstace();
		CalcPadTrainingData.killSharedInstace();
		CalcPadInterrupter.killSharedInstace(); 
		
		super.onTerminate(); 
	}

	@Override
	public boolean OnCalcPadEvent(long eventId, String event, Object caller) {
		
		boolean evtHandled = false; 
		
		if( caller == _trainingDataInstance ){
			if( event.equals(CalcPadEventConstants.EVT_NO_DATA_ON_DISK)){
				// train 
				_ocrInstance.TrainFromDisk(CalcPadApplication.getNextAsyncId());
			} else if( event.equals(CalcPadEventConstants.EVT_DATA_LOADED_FROM_DISK)){
				_ocrInstance.SetTrainingDataAndLabels(_trainingDataInstance.GetTrainingData(), _trainingDataInstance.GetLabels());
					
				evtHandled = broadcast(eventId, CalcPadEventConstants.EVT_TRAINING_DATA_LOADED);
			} else{
				evtHandled = broadcast(eventId, event);
			}
		}
		else if( caller == _ocrInstance ){
			if( event.equals(CalcPadEventConstants.EVT_LOADED_FROM_DISK_COMPLETED)){
				// save 
				_trainingDataInstance.setData(_ocrInstance.getTrainingData(), _ocrInstance.getLabels());
				_trainingDataInstance.saveData(CalcPadApplication.getNextAsyncId()); 
				
				evtHandled = broadcast(eventId, CalcPadEventConstants.EVT_TRAINING_DATA_LOADED);
			} else{
				evtHandled = broadcast(eventId, event);				
			}
		}
		else if( caller == _parser ){
			evtHandled = broadcast(eventId, event);
		}
		
		return evtHandled; 
	}

	@Override
	public void addObserver(ICalcPadObserver observer) {
		_subscriber.addObserver(observer);		
	}

	@Override
	public void removeObserver(ICalcPadObserver observer) {
		_subscriber.removeObserver(observer);
	}

	@Override
	public boolean broadcast(long eventId, String event) {
		return _subscriber.broadcast(eventId, event, this);
	}
	
	public ArrayAdapter<CalcPadLabelEnum> createLabelListAdapater(){
		ArrayAdapter<CalcPadLabelEnum> labelEnums = new ArrayAdapter<CalcPadLabelEnum>(getApplicationContext(),R.layout.label_select_item);
		labelEnums.addAll(_labels);
		return labelEnums;
	}
	
	public class LabelListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return _labels.length; 
		}

		@Override
		public Object getItem(int position) {
			return _labels[position];
		}

		@Override
		public long getItemId(int position) {
			return _labels[position].label();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if( _opensansRegularTypeface == null ){				
				_opensansRegularTypeface = Typeface.createFromAsset(getAssets(),"fonts/opensans_regular.ttf"); 
			}
			
			float density = getResources().getDisplayMetrics().density; 
			
			TextView tv = new TextView(parent.getContext());
			tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER));
			tv.setTypeface(_opensansRegularTypeface);
			tv.setTextSize(9 * density);
			tv.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
			tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			int hTitleTVPadding = (int)(2 * density);  
			int vTitleTVPadding = (int)(2 * density);
			tv.setPadding(hTitleTVPadding, vTitleTVPadding, hTitleTVPadding, vTitleTVPadding);
			tv.setText(String.format("%s (%s)", _labels[position].toString(), _labels[position].friendlyName()));
			
			return tv; 
		}				
	}
		
}
