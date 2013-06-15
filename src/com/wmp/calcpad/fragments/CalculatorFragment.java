package com.wmp.calcpad.fragments;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.support.v4.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.samsung.spensdk.applistener.SCanvasInitializeListener;
import com.samsung.spensdk.applistener.SCanvasLongPressListener;
import com.samsung.spensdk.applistener.SCanvasMatrixChangeListener;
import com.samsung.spensdk.applistener.SPenDetachmentListener;
import com.samsung.spensdk.applistener.SPenHoverListener;
import com.samsung.spensdk.applistener.SPenTouchListener;
import com.samsung.spensdk.applistener.SettingStrokeChangeListener;
import com.wmp.calcpad.CalcPadApplication;
import com.wmp.calcpad.CalcPadEventConstants;
import com.wmp.calcpad.R;
import com.wmp.calcpad.interrupter.CalcPadInterrupter;
import com.wmp.calcpad.messaging.ICalcPadObserver;
import com.wmp.calcpad.ocr.CalcPadClassificationResult;
import com.wmp.calcpad.ocr.CalcPadOCR;
import com.wmp.calcpad.parser.CalcPadParser;
import com.wmp.calcpad.parser.CalcPadVariable;
import com.wmp.calcpad.parser.exception.VariableNotFoundException;

public class CalculatorFragment extends CalcPadCanvasFragment implements SPenTouchListener, SPenHoverListener, SPenDetachmentListener, SCanvasLongPressListener, SettingStrokeChangeListener, 
SCanvasInitializeListener, SCanvasMatrixChangeListener, ICalcPadObserver{	
	
	public static final int PAGE_ID = 0; 
	
	public static final String TAG = "Work";
	
	private static final long REFRESH_DELAY = 1000; 
	
	private static boolean InstructionsAlertShown = false;
	
	private boolean _refreshing = false; 	
	
	private Handler _handler = new Handler();
	
	private Timer _timer = new Timer("cvTimer");
	
	private List<RefreshTimerTask> _timerTasks = new Vector<CalculatorFragment.RefreshTimerTask>();
	
	private RectF _dirtyRect = new RectF();
	
	private boolean _isDirty = false; 
	
	private List<AsyncRequest> _asyncRequests = new Vector<CalculatorFragment.AsyncRequest>(); 		
	
	////
	// Start; Fragment callbacks 
	////
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);		
    }
	
	@Override
	public void onViewStateRestored(Bundle savedInstanceState){
		super.onViewStateRestored(savedInstanceState);						
	}		
	
	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);					
	}
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){    	               
        inflater.inflate(R.menu.menu_calculator, menu);       
    } 
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {		
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
				refresh(); 
			}
			return true; 
		}		
		case R.id.mi_redo:
		{
			if( _sCanvas.redo() ){
				refresh(); 
			}
			return true; 
		}
		case R.id.mi_test_math_parser:
		{
			
			return true; 
		}
		}
		return false;
	}  
	
	@Override
	public void onResume(){
		super.onResume(); 
		
		setRefreshing(false);
	}
	
	////
	// End; Fragment callbacks 
	////	
	
	////
	// Start; Overrides 
	////
	
	@Override
	public boolean onTouchFinger(View arg0, MotionEvent event) {
		super.onTouchFinger(arg0, event);				
		
		if(event.getAction()==MotionEvent.ACTION_UP){
			//_handler.postDelayed(RefreshRunnable, 1000);
			refresh();							
		}
		else if( event.getAction() == MotionEvent.ACTION_DOWN ){
			cancelRefresh(); 
		}	
		
		expandDirtyRect(event);
		
		return false;	// dispatch event to SCanvasView for drawing
	}

	@Override
	public boolean onTouchPen(View arg0, MotionEvent event) {
		super.onTouchPen(arg0, event);
		
		if(event.getAction()==MotionEvent.ACTION_UP){
			//_handler.postDelayed(RefreshRunnable, 1000);
			refresh();					
		}
		else if( event.getAction() == MotionEvent.ACTION_DOWN ){
			cancelRefresh(); 
		}
		
		expandDirtyRect(event);
		
		return false;
	}
	
	////
	// End; Overrides 
	////

    ////
    // Start; Implementation of abstract methods 
    ////
    
	@Override
	public boolean OnCalcPadEvent(long eventId, String event, Object caller) {
		if( !_menuVisible ){
			return false; 
		}
		
		Log.i(this.getClass().getName(), String.format("OnOCREvent %s", event));

		////
		// CalcPadTrainingData events 
		////
		if (event.equals(CalcPadEventConstants.EVT_TRAINING_DATA_LOADED)) {
			Toast.makeText(this.getActivity(),getString(R.string.ocr_classifiction_data_loaded),Toast.LENGTH_SHORT).show();						
			
			return true; 
		}
		else if( event.equals(CalcPadEventConstants.EVT_NO_TRAINING_DATA)){
			Toast.makeText(this.getActivity(),getString(R.string.ocr_no_data),Toast.LENGTH_SHORT).show();
			
			return true; 
		}
		////
		// CalcPad ORC events 
		////
		else if (event.equals(CalcPadEventConstants.EVT_CLASSIFICATION_COMPLETED)) {
			if (_classificationResults.size() == 0) {
				setRefreshing(false);
				
				Toast.makeText(this.getActivity(),getString(R.string.ocr_classifiction_finished_with_no_results),Toast.LENGTH_SHORT).show();
				resetBackgroundBitmap();								
			} else {
				Toast.makeText(this.getActivity(),getString(R.string.ocr_classifiction_finished),Toast.LENGTH_SHORT).show();
				
				// get associated request details  
				AsyncRequest request = getAsyncRequestWithEventId(eventId);
				
				if( request != null ){
					// update classification results based on rect
					for( CalcPadClassificationResult cr : _classificationResults ){
						cr.rect.x += request.rect.left;
						cr.rect.y += request.rect.top; 
					}
					
					// now parse the classification 
					CalcPadInterrupter.getSharedInstance().parse(eventId, _classificationResults, _interrupterResults);
										
				}
				else{										
					Log.e(TAG, "OnCalcPadEvent - unassociated async event request with ID " + eventId );
				}										
			}	
			
			removeAsyncRequestWithEventId(eventId);
			return true; 
		} 
		////
		// CalcPad Interrupter events 
		////
		else if (event.equals(CalcPadEventConstants.EVT_INTERRUPTER_BUSY)) {	
			setRefreshing(false);
			Toast.makeText(this.getActivity(),getString(R.string.ocr_is_busy),Toast.LENGTH_SHORT).show();
			return true; 
		} else if (event.equals(CalcPadEventConstants.EVT_INTERRUPTER_ERR_INVALID_PARAMATERS)) {
			setRefreshing(false);
			Toast.makeText(this.getActivity(),getString(R.string.ocr_error_invalid_parameters_for_interrupter),Toast.LENGTH_SHORT).show();
			return true; 
		} else if (event.equals(CalcPadEventConstants.EVT_INTERRUPTER_COMPLETED)) {
			List<CalcPadVariable> variables = new Vector<CalcPadVariable>(); 
			processInterrupterExpressions(_interrupterResults, variables, true);
			
			setRefreshing(false);			
			resetBackgroundBitmap();	
			return true; 
		}				
		
		return false;
	}	
	
	private void processInterrupterExpressions( List<CalcPadParser> interruptedExpressions, List<CalcPadVariable> variables, boolean firstPass ){
		List<CalcPadParser> interrupterResultsDepedentOnVariables = new Vector<CalcPadParser>(); 				
		
		for(CalcPadParser parser : _interrupterResults ){
			Log.i(TAG, parser.getExpression().toString());
			try{
				parser.parse(variables);
				
				if( firstPass ){
					if( parser.getResult().isLabelSet() ){
						variables.add(parser.getResult());
					}
				}
			} catch( VariableNotFoundException e ){
				Log.w(TAG, "VariableNotFoundException " + e.getVariableLabel() );
				
				if( firstPass ){
					interrupterResultsDepedentOnVariables.add(parser);
				}
			}
		}
		
		if( interrupterResultsDepedentOnVariables.size() > 0 ){
			processInterrupterExpressions(interrupterResultsDepedentOnVariables, variables, false);
		}
	}

	@Override
	protected void drawCanvasBackgroundBitmap(Canvas c){
		
		float density = getResources().getDisplayMetrics().density; 
		
		Paint p = new Paint();
		p.setColor(getResources().getColor(R.color.grid_line));
		p.setStyle(Style.STROKE);		
		p.setStrokeWidth(1);
		
		// cell dimensions (square)
		float cd = 50 / density;
		
		// create grid paint 
		int cols = (int) (c.getWidth() / cd);
		int rows = (int) (c.getHeight() / cd);
		
		float hPadding = (c.getWidth() % cd) / 2;
		float vPadding = (c.getHeight() % cd) / 2;								
						
		// draw cols
		for( int col=0; col<=cols; col++ ){
			float x = hPadding + col * cd; 
				
			c.drawLine(x, vPadding, x, c.getHeight() - vPadding, p);						
		}
		
		// draw rows 
		for( int row=0; row<=rows; row++ ){
			float y = vPadding + row * cd; 
			
			c.drawLine(hPadding, y, c.getWidth() - hPadding, y, p);
		}
		
		drawResults(c);
	}
	
	protected void drawResults(Canvas c){
    	if( _interrupterResults == null || _interrupterResults.size() == 0 ){
    		//return; 
    	}
    	
    	if( _opensansRegularTypeface == null ){
    		_opensansRegularTypeface = Typeface.createFromAsset(getActivity().getAssets(),"fonts/opensans_regular.ttf"); 
    	}
    	
    	// stroke swith 
    	float sw = 1.0f * getResources().getDisplayMetrics().density;
    	
    	// font size 
    	float fs = 15.0f * getResources().getDisplayMetrics().density; 
    	
    	// padding 
    	float padding = 1.0f * getResources().getDisplayMetrics().density;    	
    	
    	// draw results on the overlay 
    	Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    	p.setColor(getResources().getColor(R.color.test_results));    	
    	p.setStyle(Paint.Style.STROKE);
    	p.setStrokeCap(Cap.ROUND);
    	p.setStrokeJoin(Join.ROUND);
    	p.setStrokeWidth(sw);    
    	// create a new paint for the text 
    	Paint fp = new Paint(p);
    	// make some bounding box specific adjustments 
    	p.setPathEffect( new DashPathEffect(new float[]{10, 10}, 0));    		    	
    	// finish off setting up the font paint 
    	fp.setTextSize(fs);   
    	fp.setTextAlign(Align.LEFT);
    	fp.setStyle(Paint.Style.FILL);
    	fp.setTypeface(_opensansRegularTypeface); 
    	
    	/*for( AsyncRequest as : _asyncRequests ){
    		// draw a rect around boundary 
    		c.drawRect(as.rect, p);
    	}*/
    	    	    	
    	/*for(CalcPadClassificationResult result : _classificationResults ){
    		// get bounding box 
    		RectF bb = result.cvRectToRectF(); 
    		// draw a rect around boundary 
    		c.drawRect(bb, p);
    		// get label
    		String label = CalcPadApplication.getCalcPadLabelEnumAsCharWithLabel( result.label );    		    		
    		// calc positions 
    		float x = bb.centerX(); 
    		float y = bb.centerY();    		
    		// draw label 
    		c.drawText(label, x, y, fp);
    	} */ 	
    	
    	//p.setStrokeWidth(sw * 2);
    	//p.setColor(Color.GREEN);
    	
    	Paint bgP = new Paint(); 
		bgP.setStyle(Style.FILL);
		bgP.setColor(getResources().getColor(R.color.highlight_equation_color));
    	
    	for(CalcPadParser parser : _interrupterResults ){
    		RectF boundingBox = parser.getExpression().getBoundingBox(); 
    		String expression = parser.toString();    		    		
    		
    		c.drawRect(boundingBox, bgP); 
    		c.drawText(expression, boundingBox.left, boundingBox.bottom + fs, fp);
    	}
    }

	@Override
	public String getName() {
		return TAG; 
	}

	@Override
	public boolean hasInstructionAlertBeenShown() {
		return CalculatorFragment.InstructionsAlertShown;
	}

	@Override
	public void setInstructionAlertBeenShown(boolean shown) {
		CalculatorFragment.InstructionsAlertShown = shown; 		
	}

	@Override
	protected String getBundleCanvasBitmapKey() {
		return TAG + "_canvas_bitmap";
	}

	@Override
	protected String getInstructions() {
		return getResources().getString(R.string.instructions_calculator); 
	}
	
	////
	// End; Implementation of abstract methods 
	////
	
	////
	// Start; class methods 
	////
	
	private void expandDirtyRect(MotionEvent event){
		float eventX = event.getX();
        float eventY = event.getY();

        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
        	  expandDirtyRect( eventX, eventY );
        	  break;
          case MotionEvent.ACTION_MOVE:
        	  expandDirtyRect( eventX, eventY );
        	  break;
          case MotionEvent.ACTION_UP:
        	  expandDirtyRect( eventX, eventY );
        	  
        	  // When the hardware tracks events faster than they are delivered, the
              // event will contain a history of those skipped points.
              int historySize = event.getHistorySize();
              for (int i = 0; i < historySize; i++) {
                float historicalX = event.getHistoricalX(i);
                float historicalY = event.getHistoricalY(i);
                
                expandDirtyRect(historicalX, historicalY);               
              }
        	  break;
        }
	}
	
	private synchronized void expandDirtyRect( float pointX, float pointY ){
		float halfSW = getStrokeWidth()/2.0f;
		
		if( !_isDirty ){
			_dirtyRect.left = pointX-halfSW; 
			_dirtyRect.top = pointY-halfSW;
			_dirtyRect.right = pointX+halfSW; 
			_dirtyRect.bottom = pointY+halfSW;
			_isDirty = true; 
		} else{
			// expand 
			if( pointX-halfSW < _dirtyRect.left ){
				_dirtyRect.left = pointX-halfSW; 
			} else if( pointX+halfSW > _dirtyRect.right ){
				_dirtyRect.right = pointX+halfSW;
			}
			
			if( pointY-halfSW < _dirtyRect.top ){
				_dirtyRect.top = pointY-halfSW;
			} else if( pointY+halfSW > _dirtyRect.bottom ){
				_dirtyRect.bottom = pointY+halfSW;
			}
		}
	}
	
	private synchronized void resetDirtyRect(){
		_isDirty = false; 
	}
	
	private void cancelRefresh(){
		for( RefreshTimerTask task : _timerTasks ){
			task.cancel(); 
		}
		_timerTasks.clear();
		_timer.purge();
	}
	
	private void refresh(){		
		
		for( RefreshTimerTask task : _timerTasks ){
			task.cancel(); 
		}
		_timerTasks.clear();
		_timer.purge();
		
		RefreshTimerTask refreshTask = new RefreshTimerTask(); 
		_timerTasks.add(refreshTask);
		
		_timer.schedule(refreshTask, REFRESH_DELAY);
		
		//_refreshTimer.cancel(); 
		//_refreshTimer.schedule(_refreshTimerTask, 1000);
		//doRefresh(); 
	}
	
	private void doRefresh(){
		if( IsRefreshing() ){
			return; 
		}
						
		if (!CalcPadOCR.getSharedInstance().isReady()) {
			Toast.makeText(this.getActivity(), getString(R.string.ocr_is_busy), Toast.LENGTH_SHORT).show();
			return; 
		}			
		
		setRefreshing(true);				

		_classificationResults.clear();
		_interrupterResults.clear();
		
		//AsyncRequest request = new AsyncRequest(CalcPadApplication.getNextAsyncId(), _dirtyRect);
		AsyncRequest request = new AsyncRequest(CalcPadApplication.getNextAsyncId(), getCanvasRect());
		_asyncRequests.add(request);
		resetDirtyRect();
		
		//Bitmap bitmap = getCanvasBitmap(request.rect);
		Bitmap bitmap = getCanvasBitmap();
		
		if (bitmap != null) {
			if (!CalcPadOCR.getSharedInstance().Classify(request.eventId, bitmap,_classificationResults)) {
				Toast.makeText(this.getActivity(), getString(R.string.ocr_is_busy), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this.getActivity(), getString(R.string.ocr_classifiction_started), Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(this.getActivity(), getString(R.string.ocr_error_no_bitmap_available), Toast.LENGTH_SHORT).show();
		}
	}
	
	public synchronized void setRefreshing(boolean refreshing){
		this._refreshing = refreshing; 
	}
	
	public synchronized  boolean IsRefreshing(){
		return this._refreshing; 
	}
	
	public AsyncRequest getAsyncRequestWithEventId( long eventId ){
		for( AsyncRequest ar : _asyncRequests ){
			if( ar.eventId == eventId ){
				return ar; 
			}
		}		
		
		return null; 
	}
	
	public boolean removeAsyncRequestWithEventId( long eventId ){
		int idx = -1; 
		
		for( int i=0; i<_asyncRequests.size(); i++ ){
			if( _asyncRequests.get(i).eventId == eventId ){
				idx = i; 
				break; 
			}
		}
		
		if( idx != -1 ){
			_asyncRequests.remove(idx);
		}
		
		return idx != -1; 
	}
	
	////
	// End; class methods 
	////
	
	private Runnable RefreshRunnable = new Runnable() {
	    public void run() {
	       doRefresh(); 
	    }
	};
	
	private class RefreshTimerTask extends TimerTask{

		@Override
		public void run() {
			_handler.post(RefreshRunnable);	
		}
		
	}	
	
	private class AsyncRequest{
		public RectF rect; 
		public long eventId; 
		
		public AsyncRequest( long eventId, RectF rect){
			this.eventId = eventId; 
			this.rect = rect; 
		}
	}
}
