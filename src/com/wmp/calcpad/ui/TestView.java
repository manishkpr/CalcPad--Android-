package com.wmp.calcpad.ui;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.samsung.samm.common.SObjectStroke;
import com.samsung.spen.settings.SettingStrokeInfo;
import com.samsung.spensdk.SCanvasConstants;
import com.samsung.spensdk.SCanvasView;
import com.samsung.spensdk.applistener.SCanvasInitializeListener;
import com.samsung.spensdk.applistener.SCanvasLongPressListener;
import com.samsung.spensdk.applistener.SPenDetachmentListener;
import com.samsung.spensdk.applistener.SPenHoverListener;
import com.samsung.spensdk.applistener.SPenTouchListener;
import com.samsung.spensdk.applistener.SettingStrokeChangeListener;
import com.wmp.calcpad.R;
import com.wmp.calcpad.ocr.CalcPadTrainingData;
import com.wmp.calcpad.utils.FileUtil;
import com.wmp.calcpad.utils.SPenSDKUtils;

public class TestView extends FrameLayout implements SPenTouchListener, SPenHoverListener, SPenDetachmentListener, SCanvasLongPressListener, SettingStrokeChangeListener, 
SCanvasInitializeListener{

	public static final String TAG = "Test";
	
	private static final int TOOL_UNKNOWN = 0;
	private static final int TOOL_FINGER = 1;
	private static final int TOOL_PEN = 2;
	private static final int TOOL_PEN_ERASER = 3;
	
	private int _currentTool = TOOL_UNKNOWN;
	
	private SCanvasView _sCanvas;	
	
	private SettingStrokeInfo _strokeInfoPen;
	
	private SettingStrokeInfo _strokeInfoFinger;
	
	private View _view = null; 
	
	private Bitmap _canvasBitmap; 
	
	private Paint _canvasPaint; 
	
	private Canvas _canvasBitmapCanvas = null; 
	
	public TestView(Context context) {
		super(context);
		//init(); 
	}
	
	public TestView(Context context, AttributeSet attrs){
		super(context, attrs);		
		init();
	}
	
	public TestView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);		
		init();
	}
	
	
	private void init(){
		_view = LayoutInflater.from(this.getContext()).inflate(R.layout.view_test, this);
		
		// Any additional initialization
        // ie add listners, adapters, ... 
        _sCanvas = (SCanvasView)_view.findViewById(R.id.canvas_view);        
        
      //------------------------------------
		// SettingView Setting
		//------------------------------------
		// Resource Map for Layout & Locale
		HashMap<String,Integer> settingResourceMapInt = SPenSDKUtils.getSettingLayoutLocaleResourceMap(true, true, false, false);
		
		// Resource Map for Custom font path
		HashMap<String,String> settingResourceMapString = SPenSDKUtils.getSettingLayoutStringResourceMap(true, true, false, false);
		
		RelativeLayout settingViewContainer = (RelativeLayout) _view.findViewById(R.id.canvas_container);		
		_sCanvas.createSettingView(settingViewContainer, settingResourceMapInt, settingResourceMapString);    	  
		_sCanvas.setSCanvasHoverPointerStyle(SCanvasConstants.SCANVAS_HOVERPOINTER_STYLE_NONE);    	    	
    	
		// Initialize Stroke Setting
		_strokeInfoFinger = new SettingStrokeInfo();
		_strokeInfoFinger.setStrokeStyle(SObjectStroke.SAMM_STROKE_STYLE_CRAYON);
		_strokeInfoFinger.setStrokeColor(Color.RED);
		_strokeInfoFinger.setStrokeWidth(40);
		
		_strokeInfoPen = new SettingStrokeInfo();
		_strokeInfoPen.setStrokeStyle(SObjectStroke.SAMM_STROKE_STYLE_PENCIL);
		_strokeInfoPen.setStrokeColor(Color.BLACK);
		_strokeInfoPen.setStrokeWidth(25);
		
		// add listeners 
		_sCanvas.setSPenTouchListener(this);
		_sCanvas.setSPenHoverListener(this);
		_sCanvas.setSPenDetachmentListener(this);
		_sCanvas.setSCanvasLongPressListener(this);
		_sCanvas.setSettingStrokeChangeListener(this); 
				
		_sCanvas.setSCanvasInitializeListener(this);
	}		
    
    public void clearCanvas(){
    	resetBackgroundBitmap();
    	_sCanvas.clearSCanvasView();
    	
    	//_canvasBitmapCanvas.drawRect(new RectF(0, 0, _canvasBitmap.getWidth(), _canvasBitmap.getHeight()), _canvasPaint);
		//_sCanvas.setBitmap(_canvasBitmap, false);
    }
    
    public Bitmap getCanvasBitmap(){
    	//ImageView sampleView = (ImageView)_view.findViewById(R.id.sample_imageview);
    	//sampleView.setImageBitmap(_sCanvas.getCanvasBitmap(true));
    	
    	Bitmap bitmap = _sCanvas.getCanvasBitmap(true);
    	
    	if( _canvasBitmapCanvas == null ){
    		// create a bitmap for the canvas (remove transparency) 
    		_canvasPaint = new Paint(); 
    		_canvasPaint.setColor(Color.WHITE);
    		_canvasBitmap = Bitmap.createBitmap(_sCanvas.getWidth(), _sCanvas.getHeight(), Config.RGB_565);
    		_canvasBitmapCanvas = new Canvas(_canvasBitmap);    				
    	}
    	
    	_canvasBitmapCanvas.drawRect(new RectF(0, 0, _canvasBitmap.getWidth(), _canvasBitmap.getHeight()), _canvasPaint);
    	_canvasBitmapCanvas.drawBitmap(bitmap, 0, 0, new Paint());
    	
    	FileUtil.StoreBitmapData(this.getContext(), 
    			_canvasBitmap, 
    				FileUtil.getDataPath() + "image.png", false, CompressFormat.PNG);
    	
    	return _canvasBitmap;
    }
    
    public void setBackgroundBitmap(Bitmap bitmap){
    	_sCanvas.setBackgroundImage(bitmap);
    	//_sCanvas.setBitmap(bitmap, false);
    	//ImageView sampleView = (ImageView)_view.findViewById(R.id.sample_imageview);
    	//sampleView.setImageBitmap(bitmap);
    }    
    
    public void resetBackgroundBitmap(){
    	setBackgroundBitmap(null);
    }
    
	////
	// Start; SPenTouchListener
	///

	@Override
	public void onTouchButtonDown(View arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTouchButtonUp(View arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onTouchFinger(View arg0, MotionEvent event) {
		// Update Current Color
		if(_currentTool!= TOOL_FINGER){
			_currentTool = TOOL_FINGER;

		if(event.getAction()==MotionEvent.ACTION_DOWN)
			_sCanvas.setSettingViewStrokeInfo(_strokeInfoFinger);
		}
		return false;	// dispatch event to SCanvasView for drawing
	}

	@Override
	public boolean onTouchPen(View arg0, MotionEvent event) {
		// Update Current Color
		if(_currentTool!=TOOL_PEN){
			_currentTool = TOOL_PEN;
		
		if(event.getAction()==MotionEvent.ACTION_DOWN)
			_sCanvas.setSettingViewStrokeInfo(_strokeInfoPen);
		}
		
		return false;
	}

	@Override
	public boolean onTouchPenEraser(View arg0, MotionEvent event) {
		if(_currentTool!=TOOL_PEN_ERASER){
			_currentTool = TOOL_PEN_ERASER;
		}
		
		if(event.getAction()==MotionEvent.ACTION_DOWN)
			_sCanvas.setEraserStrokeSetting(SObjectStroke.SAMM_DEFAULT_MAX_ERASERSIZE);

		return false;	// dispatch event to SCanvasView for drawing
	}
	
	////
	// End; SPenTouchListener
	///
	
	////
	// Start; SPenHoverListener
	///

	@Override
	public boolean onHover(View arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onHoverButtonDown(View arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onHoverButtonUp(View arg0, MotionEvent arg1) {
		_sCanvas.toggleShowSettingView(SCanvasConstants.SCANVAS_SETTINGVIEW_PEN);		
	}	
	
	////
	// End; SPenHoverListener
	///
	
	////
	// Start; SPenDetachmentListener
	///
	
	@Override
	public void onSPenDetached(boolean arg0) {
		// TODO Auto-generated method stub
		
	}
	
	////
	// End; SPenDetachmentListener
	////
	
	////
	// Start; SCanvasLongPressListener
	////
	
	@Override
	public void onLongPressed() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onLongPressed(float arg0, float arg1) {
		// TODO Auto-generated method stub
		
	}
	
	////
	// End; SCanvasLongPressListener
	////
	
	////
	// Start; SettingStrokeChangeListener 
	////
	
	@Override
	public void onClearAll(boolean bClearAllCompleted) {
	}
	@Override
	public void onEraserWidthChanged(int eraserWidth) {				
	}

	@Override
	public void onStrokeColorChanged(int strokeColor) {
		if(_currentTool == TOOL_PEN)
			_strokeInfoPen.setStrokeColor(strokeColor);
		else if(_currentTool == TOOL_FINGER)
			_strokeInfoFinger.setStrokeColor(strokeColor);

	}

	@Override
	public void onStrokeStyleChanged(int strokeStyle) {
		if(_currentTool == TOOL_PEN)
			_strokeInfoPen.setStrokeStyle(strokeStyle);
		else if(_currentTool == TOOL_FINGER)
			_strokeInfoFinger.setStrokeStyle(strokeStyle);
	}

	@Override
	public void onStrokeWidthChanged(int strokeWidth) {
		if(_currentTool == TOOL_PEN)
			_strokeInfoPen.setStrokeWidth(strokeWidth);
		else if(_currentTool == TOOL_FINGER)
			_strokeInfoFinger.setStrokeWidth(strokeWidth);
	}

	@Override
	public void onStrokeAlphaChanged(int strokeAlpha) {				
		if(_currentTool == TOOL_PEN)
			_strokeInfoPen.setStrokeAlpha(strokeAlpha);
		else if(_currentTool == TOOL_FINGER)
			_strokeInfoFinger.setStrokeAlpha(strokeAlpha);
	}			
	
	////
	// End; SettingStrokeChangeListener 
	////
	
	////
	// Start; SCanvasInitializeListener 
	////
	
	@Override
	public void onInitialized(){
		//_sCanvas.setBackgroundColor(Color.WHITE);
		_sCanvas.setSettingViewStrokeInfo(_strokeInfoPen);	
		
		resetBackgroundBitmap();
	}
	
	////
	// End; SCanvasInitializeListener 
	////

}
