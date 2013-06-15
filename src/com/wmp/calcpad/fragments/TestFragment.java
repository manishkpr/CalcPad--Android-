package com.wmp.calcpad.fragments;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.wmp.calcpad.messaging.ICalcPadObserver;
import com.wmp.calcpad.ocr.CalcPadClassificationResult;
import com.wmp.calcpad.ocr.CalcPadOCR;

public class TestFragment extends CalcPadCanvasFragment implements SPenTouchListener, SPenHoverListener, SPenDetachmentListener, SCanvasLongPressListener, SettingStrokeChangeListener, 
SCanvasInitializeListener, SCanvasMatrixChangeListener, ICalcPadObserver{

	private static boolean InstructionsAlertShown = false; 
	
	public static final int PAGE_ID = 1;
	
	public static final String TAG = "Test";
		 
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
        inflater.inflate(R.menu.menu_test, menu);       
    } 
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.mi_classify: {

			if (!CalcPadOCR.getSharedInstance().isReady()) {
				Toast.makeText(this.getActivity(),
						getString(R.string.ocr_is_busy), Toast.LENGTH_SHORT)
						.show();
				return true;
			}			

			_classificationResults.clear();
			
			Bitmap bitmap = getCanvasBitmap();
			if (bitmap != null) {
				if (!CalcPadOCR.getSharedInstance().Classify(CalcPadApplication.getNextAsyncId(), bitmap,
						_classificationResults)) {
					Toast.makeText(this.getActivity(),
							getString(R.string.ocr_is_busy), Toast.LENGTH_SHORT)
							.show();
				} else {
					Toast.makeText(this.getActivity(),
							getString(R.string.ocr_classifiction_started),
							Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(this.getActivity(),
						getString(R.string.ocr_error_no_bitmap_available),
						Toast.LENGTH_SHORT).show();
			}

			return true;
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
	
	////
	// End; Fragment callbacks 
	////	
	
	////
	// Start; Implementation of abstract methods 
	////
	
	@Override
	public boolean hasInstructionAlertBeenShown(){
		return TestFragment.InstructionsAlertShown;
	}
	
	@Override
	public void setInstructionAlertBeenShown(boolean shown){
		TestFragment.InstructionsAlertShown = shown; 
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
		return getResources().getString(R.string.instructions_test); 
	}	
	
	@Override
	protected void drawCanvasBackgroundBitmap(Canvas c){
		
		float density = getResources().getDisplayMetrics().density; 
		
		Paint p = new Paint();
		p.setColor(getResources().getColor(R.color.grid_line));
		p.setStyle(Style.STROKE);		
		p.setStrokeWidth(1);
		
		// cell dimensions (square)
		float cd = 200 / density;
		
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
    	if( _classificationResults == null || _classificationResults.size() == 0 ){
    		return; 
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
    	
    	/**
    	 if you wanted to texture a stroke; use the following code: 
    	 p = new Paint(Paint.FILTER_BITMAP_FLAG);
		 Shader shader = new BitmapShader(bitmapOfTexture, Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);
		 p.setShader(shader);     	
    	 */
    	
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
    	
    	for(CalcPadClassificationResult result : _classificationResults ){
    		// get bounding box 
    		RectF bb = result.cvRectToRectF(); 
    		// draw a rect around boundary 
    		c.drawRect(bb, p);
    		// get label
    		String label = CalcPadApplication.getCalcPadLabelEnumAsCharWithLabel( result.label );    		    		
    		// calc positions 
    		float x = bb.right + padding; 
    		float y = bb.bottom - sw - padding;    		
    		// draw label 
    		c.drawText(label, x, y, fp);
    	}  	
    }
	
	////
	// End; Implementation of abstract methods 
	////		           	
	
	////
	// Start; ICalcPadObserver
	////
	
	@Override
	public boolean OnCalcPadEvent(long eventId, String event, Object caller) {

		if( !_menuVisible ){
			return false; 
		}
		
		Log.i(this.getClass().getName(), String.format("OnOCREvent %s", event));

		if (event.equals(CalcPadEventConstants.EVT_CLASSIFICATION_COMPLETED)) {

			if (_classificationResults.size() == 0) {
				Toast.makeText(
						this.getActivity(),
						getString(R.string.ocr_classifiction_finished_with_no_results),
						Toast.LENGTH_SHORT).show();
				
				resetBackgroundBitmap();		
				
				return true; 
			} else {
				Toast.makeText(this.getActivity(),
						getString(R.string.ocr_classifiction_finished),
						Toast.LENGTH_SHORT).show();

				if (_runningTest) {
					Bitmap bitmap = CalcPadOCR.getSharedInstance()
							.getCurrentOpenMorhAsBitmap();
					Canvas bitmapCanvas = new Canvas(bitmap);

					Paint p = new Paint();
					p.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
					p.setStyle(Paint.Style.STROKE);
					p.setColor(Color.RED);

					Paint p2 = new Paint();
					p2.setStyle(Paint.Style.FILL_AND_STROKE);
					p2.setColor(Color.YELLOW);
					p2.setTextSize(12 * getResources().getDisplayMetrics().density);
					p2.setTextAlign(Align.LEFT);

					for (CalcPadClassificationResult result : _classificationResults) {
						bitmapCanvas.drawRect(result.cvRectToRectF(), p);
						bitmapCanvas.drawText(Integer.toString(result.label),
								result.rect.x, result.rect.y, p2);
					}

					setBackgroundBitmap(bitmap);

					_runningTest = false;
				} else {
					resetBackgroundBitmap();				
				}
				
				return true; 
			}
		} 
		else if (event.equals(CalcPadEventConstants.EVT_TRAINING_DATA_LOADED)) {
			Toast.makeText(this.getActivity(),
					getString(R.string.ocr_classifiction_data_loaded),
					Toast.LENGTH_SHORT).show();
			
			return true; 
		}
		else if( event.equals(CalcPadEventConstants.EVT_NO_TRAINING_DATA)){
			Toast.makeText(this.getActivity(),
					getString(R.string.ocr_no_data),
					Toast.LENGTH_SHORT).show();
			
			return true; 
		}
		
		return false; 
	}
	
	////
	// End; ICalcPadObserver
	////

}
