package com.wmp.calcpad.interrupter;

import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.wmp.calcpad.CalcPadEventConstants;
import com.wmp.calcpad.messaging.CalcPadSubscriber;
import com.wmp.calcpad.ocr.CalcPadClassificationResult;
import com.wmp.calcpad.ocr.CalcPadLabelEnum;
import com.wmp.calcpad.ocr.CalcPadLabelTypeEnum;
import com.wmp.calcpad.parser.CalcPadParser;

public class CalcPadInterrupter extends CalcPadSubscriber implements CalcPadExpressionBuilderDataSource{

private static CalcPadInterrupter _instance = null; 	

	public static CalcPadInterrupter getSharedInstance(){
		if( _instance == null ){
			throw new RuntimeException("CalcPadInterrupter hasn't been initilised");
		}
		
		return _instance;
	}
	
	public static CalcPadInterrupter initSharedInstance(Context context, CalcPadLabelEnum[] labels){
		if( _instance != null ){
			throw new RuntimeException("CalcPadInterrupter has already been initilised");
		}
		
		_instance = new CalcPadInterrupter(context, labels);
		
		return _instance;
	}
	
	public static void killSharedInstace(){
		if( _instance != null ){
			_instance.cleanup();
			_instance = null; 
		}
	}
	
	private Handler _handler; 
	
	private Context _context = null;
	
	private CalcPadLabelEnum[] _labels; 
	
	private boolean _busy = false; 
	
	private CalcPadInterrupter(Context context, CalcPadLabelEnum[] labels){
		_context = context; 	
		_labels = labels; 
		_handler = new Handler();
		
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);			
		Display display = wm.getDefaultDisplay();
		DisplayMetrics dm = new DisplayMetrics(); 
		display.getMetrics(dm);
		
		//Point size = new Point();
		//display.getSize(size);		
	}	
	
	private void cleanup(){
				
	}
	
	public boolean parse(final long eventId, final List<CalcPadClassificationResult> classifications, final List<CalcPadParser> results){
		if( results == null ){
			broadcast(eventId, CalcPadEventConstants.EVT_INTERRUPTER_ERR_INVALID_PARAMATERS);
			return false; 
		}
		
		if( isBusy() ){
			broadcast(eventId, CalcPadEventConstants.EVT_INTERRUPTER_BUSY);
			return false; 
		}
		
		// flush previous results 
		//results.clear(); 
		
		if( classifications == null || classifications.size() == 0 ){
			broadcast(eventId, CalcPadEventConstants.EVT_INTERRUPTER_COMPLETED);
			return true; 
		}
		
		setBusy(true); 
		
		new Thread(new Runnable() {
	        public void run() {
	        	doParse(eventId, classifications, results);
	        	
	            _handler.post(new Runnable() {
	                public void run() {
	                   setBusy(false);
	                    broadcast(eventId, CalcPadEventConstants.EVT_INTERRUPTER_COMPLETED);
	                }
	            });
	        }
	    }).start();
		
		return true; 
	}
	
	private void doParse(long eventId, List<CalcPadClassificationResult> classifications, List<CalcPadParser> results){
		// assign each character to a bucket 
		List<CalcPadExpressionBuilder> expressions = new Vector<CalcPadExpressionBuilder>(); 
		
		for( CalcPadClassificationResult classification : classifications){
			boolean addedToExpression = false;
			
			char label = getCharacterFromLabel(classification.label);
			
			if( label == 0 ){
				continue; 
			}
			
			org.opencv.core.Rect rect = classification.rect;
			
			if( rect.width == 0 || rect.height == 0 ){
				continue; 
			}
			
			for (CalcPadExpressionBuilder expression : expressions) {

				if (expression.inBounds(rect, false)) {
					expression.addItem(label, rect);
					addedToExpression = true; 
					break; 
				}
			}
			
			if( !addedToExpression ){
				//Log.i("CalPadInterrupter", "doParse - creating new expression for " + label);
				CalcPadExpressionBuilder expression = new CalcPadExpressionBuilder(this); 
				expression.addItem(label, rect);
				expressions.add(expression);
			} else{
				// collapse any expressions  
				for( int i=0; i<expressions.size(); i++ ){
					CalcPadExpressionBuilder expressionA = expressions.get(i);
					
					for( int j = expressions.size()-1; j>i; j-- ){
						CalcPadExpressionBuilder expressionB = expressions.get(j);
						
						if( expressionA.inBounds( expressionB ) ){
							// collapse the 2 expressions together 
							expressionA.addExpression(expressionB);
							// remove this expression 
							expressions.remove(j);
						}
					}
				}
			}
		}
		
		// for each express result the results 
		for( CalcPadExpressionBuilder expression : expressions ){
			CalcPadParser paser = new CalcPadParser(expression);
			paser.eventId = eventId; 
			results.add(paser);
		}
		
	}
	
	private char getCharacterFromLabel(int labelValue) {
		if( _labels == null ){
			return 0;   
		}
		
		for(CalcPadLabelEnum label : _labels ){
			if( label.label() == labelValue ){
				return label.toChar(); 
			}
		}
		
		return 0; 
	}
	
	private synchronized void setBusy(boolean busy){
		this._busy = busy; 
	}
	
	public synchronized boolean isBusy(){
		return this._busy; 
	}
	
	////
	// Start; CalcPadExpressionBuilderDataSource methods 
	////
	
	@Override
	public CalcPadLabelTypeEnum getLabelTypeForAsciiValue(char asciiValue){
		if( _labels == null ){
			return CalcPadLabelTypeEnum.Void; // null 
		}
		
		for( CalcPadLabelEnum label : _labels ){
			if( label.toChar() == asciiValue ){
				return label.type();
			}
		}
		
		return CalcPadLabelTypeEnum.Void; // null		
	}
	
	////
	// End; CalcPadExpressionBuilderDataSource methods 
	////
}
