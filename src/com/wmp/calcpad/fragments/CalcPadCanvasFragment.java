package com.wmp.calcpad.fragments;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samsung.samm.common.SObjectStroke;
import com.samsung.spen.settings.SettingStrokeInfo;
import com.samsung.spensdk.SCanvasConstants;
import com.samsung.spensdk.SCanvasView;
import com.samsung.spensdk.applistener.SCanvasInitializeListener;
import com.samsung.spensdk.applistener.SCanvasLongPressListener;
import com.samsung.spensdk.applistener.SCanvasMatrixChangeListener;
import com.samsung.spensdk.applistener.SPenDetachmentListener;
import com.samsung.spensdk.applistener.SPenHoverListener;
import com.samsung.spensdk.applistener.SPenTouchListener;
import com.samsung.spensdk.applistener.SettingStrokeChangeListener;
import com.wmp.calcpad.CalcPadApplication;
import com.wmp.calcpad.R;
import com.wmp.calcpad.messaging.ICalcPadObserver;
import com.wmp.calcpad.ocr.CalcPadClassificationResult;
import com.wmp.calcpad.ocr.CalcPadOCR;
import com.wmp.calcpad.parser.CalcPadParser;
import com.wmp.calcpad.utils.SPenSDKUtils;
import com.wmp.ui.circularpopup.PopupMenu;
import com.wmp.ui.circularpopup.PopupMenuItem;
import com.wmp.ui.circularpopup.PopupMenuView;

public abstract class CalcPadCanvasFragment extends Fragment implements SPenTouchListener, SPenHoverListener, SPenDetachmentListener, SCanvasLongPressListener, SettingStrokeChangeListener, 
	SCanvasInitializeListener, SCanvasMatrixChangeListener, ICalcPadObserver{

	protected static final float DEFAULT_PEN_WIDTH = 5; 
	
	protected static final int TOOL_UNKNOWN = 0;
	protected static final int TOOL_FINGER = 1;
	protected static final int TOOL_PEN = 2;
	protected static final int TOOL_PEN_ERASER = 3;
	
	protected int _currentTool = TOOL_UNKNOWN;
	
	protected static final int INSTRUCTIONS_ANIM_DURATION = 250; 
	
	protected RelativeLayout _canvasContainer; 
	
	protected SCanvasView _sCanvas;	
	
	protected SettingStrokeInfo _strokeInfoPen;
	
	protected SettingStrokeInfo _strokeInfoFinger;
	
	protected View _view = null; 
	
	protected Bitmap _canvasBitmap; 
	
	protected Paint _canvasPaint; 
	
	protected Canvas _canvasBitmapCanvas = null; 
	
	protected Bitmap _savedCanvasBitmap = null; 
	
	protected List<CalcPadClassificationResult> _classificationResults = new Vector<CalcPadClassificationResult>();
	
	protected List<CalcPadParser> _interrupterResults = new Vector<CalcPadParser>();	
	
	protected boolean _runningTest = false;
	
	protected boolean _showingInstructions = false; 
	
	/** bitmap holding the canvas background image */ 
	protected Bitmap _canvasBackgroundBitmap = null; 
	
	protected boolean _menuVisible = false;
	
	protected Typeface _opensansRegularTypeface = null; 
	
	protected Paint _backgroundFillPaint; 
	
	protected PopupMenuView _popupMenuView; 
	
	////
	// Start; Fragment callbacks
	////
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);		
		
		((CalcPadApplication)this.getActivity().getApplication()).addObserver(this);
    }
	
	@Override
	public void onDestroy() {
		super.onDestroy();		
		
		CalcPadOCR.getSharedInstance().removeObserver(this);
	} 
	
	private void setupPopupMenuForTesting(){
		PopupMenuItem item1 = new PopupMenuItem();
		item1.setLabel("hello");
		
		PopupMenuItem item2 = new PopupMenuItem();
		item2.setLabel("hello 2");
		
		PopupMenuItem item3 = new PopupMenuItem();
		item3.setLabel("hello 3");
		
		PopupMenu menu = new PopupMenu(PopupMenu.PopupMenuState.Closed);
		menu.setInnerOuterRadius(0, 35);
		menu.addMenuItem(item1); menu.addMenuItem(item2); menu.addMenuItem(item3); 
		
		_popupMenuView = new PopupMenuView(getActivity());
		//_popupMenuView.SetCentreMenuItem("close", 20, null, getResources().getDrawable(R.drawable.ic_action_info));
		_popupMenuView.popupMenu = menu;
		_popupMenuView.setPosition(_canvasContainer.getWidth()/2, _canvasContainer.getHeight()/2);
		_popupMenuView.hide(false);
		
		//((LinearLayout)_view.findViewById(R.id.parent_container)).addView(_popupMenuView,0);
		_canvasContainer.addView(_popupMenuView);
		
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState){
    	
    	super.onCreateView(inflater, container, savedInstanceState);
    	
    	// Inflate your layout
        _view = inflater.inflate(R.layout.fragment_test, container, false);
        
        _canvasContainer = (RelativeLayout)_view.findViewById(R.id.canvas_container);
        _sCanvas = (SCanvasView)_view.findViewById(R.id.canvas_view);       
        
        setupPopupMenuForTesting(); 
        
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
    	
		// calculate stroke width 
		float density = getResources().getDisplayMetrics().density; 
		float sw = DEFAULT_PEN_WIDTH * density; 
		
		// Initialize Stroke Setting
		_strokeInfoFinger = new SettingStrokeInfo();
		//_strokeInfoFinger.setStrokeStyle(SObjectStroke.SAMM_STROKE_STYLE_CRAYON);		
		//_strokeInfoFinger.setStrokeColor(Color.RED);
		//_strokeInfoFinger.setStrokeWidth(40);
		_strokeInfoFinger.setStrokeStyle(SObjectStroke.SAMM_STROKE_STYLE_PENCIL);		
		_strokeInfoFinger.setStrokeColor(Color.BLACK);
		_strokeInfoFinger.setStrokeWidth(sw);
		
		_strokeInfoPen = new SettingStrokeInfo();
		_strokeInfoPen.setStrokeStyle(SObjectStroke.SAMM_STROKE_STYLE_PENCIL);
		_strokeInfoPen.setStrokeColor(Color.BLACK);
		_strokeInfoPen.setStrokeWidth(sw);				
				
		_sCanvas.setSCanvasInitializeListener(this);
		
		initBackgroundFillPaint(); 
					
        return _view;
    }          
    
    @Override
    public void onDestroyView (){
    	super.onDestroyView();
    	
    	// keep hold of the reference to the canvas bitmap when the view is destoyed so we can restore it 
    	_savedCanvasBitmap = _sCanvas.getBitmap(true);    	    	
    	
    	// Release SCanvasView resources
		if(!_sCanvas.closeSCanvasView()){
			Log.e(getName(), "Fail to close SCanvasView");
		}
    }
	
	@Override
	public void onViewStateRestored(Bundle savedInstanceState){
		super.onViewStateRestored(savedInstanceState);
		
		if( savedInstanceState != null && savedInstanceState.containsKey(getBundleCanvasBitmapKey())){
			Bitmap savedBitmap = (Bitmap)savedInstanceState.getParcelable(getBundleCanvasBitmapKey());
			if( _sCanvas != null ){
				_sCanvas.setBitmap(savedBitmap, true);
			}
			savedBitmap.recycle();
		}				
	}	
	
	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		
		if( _sCanvas != null ){
			outState.putParcelable(getBundleCanvasBitmapKey(), _sCanvas.getCanvasBitmap(true));
		}			
	}
	
	@Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if( visible && isAdded() ){
        	if( !hasInstructionAlertBeenShown() ){
        		setInstructionAlertBeenShown(true);
    			showInstructions();
        	}
        }
        
        _menuVisible = visible; 
    }
	
	@Override
	public void onResume(){
		super.onResume(); 				
	}
	
	@Override
	public void onPause(){
		super.onPause(); 
	}
	
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);				
	}
	
	@Override
	public void onDetach(){
		super.onDetach();
	}
	
	////
	// End; Fragment callbacks 
	////
	
	////
	// Start; helper methods 
	////
	
	protected float getStrokeWidth(){		
		return _sCanvas.getSettingStrokeInfo().getStrokeWidth();
	}
	
	protected boolean isCanvasEnabled(){
		return _sCanvas.isEnabled(); 
	}
	
	protected void setCanvasEnabled(boolean enabled){
		_sCanvas.setEnabled(enabled);
	}
	
	protected void initBackgroundFillPaint(){
		if( _backgroundFillPaint == null ){
			
			//Bitmap paperTexBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.paper_tex_1);
			//BitmapShader shader = new BitmapShader(paperTexBitmap, Shader.TileMode.REPEAT,
			//             Shader.TileMode.REPEAT);
			
			//_backgroundFillPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
			//_backgroundFillPaint.setShader(shader);
			
			_backgroundFillPaint = new Paint();
			_backgroundFillPaint.setStyle(Style.FILL);
			_backgroundFillPaint.setColor(Color.TRANSPARENT);
		}
	}
	
	protected void showConfirmation(String title, String msg, DialogInterface.OnClickListener positiveCallback, DialogInterface.OnClickListener negativeCallback ){
		final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity(), AlertDialog.THEME_HOLO_LIGHT);
		
		builder.setTitle(getName());
		
		if( _opensansRegularTypeface == null ){
			_opensansRegularTypeface = Typeface.createFromAsset(getActivity().getAssets(),"fonts/opensans_regular.ttf"); 
		}
		//Typeface semiBoldTypeFace = Typeface.createFromAsset(getActivity().getAssets(),"fonts/opensans_semibold.ttf");
		
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
		
		builder.setMessage(msg);
		if( positiveCallback != null ){
			builder.setPositiveButton(getResources().getString(R.string.ok), positiveCallback);
		}
		if( negativeCallback != null ){
			builder.setNegativeButton(getResources().getString(R.string.cancel), negativeCallback);
		}
		AlertDialog ad = builder.show();				
		TextView tv = (TextView) ad.findViewById(android.R.id.message);		
		tv.setTextSize(8f * getResources().getDisplayMetrics().density);
		tv.setTypeface(_opensansRegularTypeface);
	}
	
	/** shows the instructions **/ 
	protected void showInstructions(){
		final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity(), AlertDialog.THEME_HOLO_LIGHT);
		
		builder.setTitle(getName());
		
		if( _opensansRegularTypeface == null ){
			_opensansRegularTypeface = Typeface.createFromAsset(getActivity().getAssets(),"fonts/opensans_regular.ttf"); 
		}
		//Typeface semiBoldTypeFace = Typeface.createFromAsset(getActivity().getAssets(),"fonts/opensans_semibold.ttf");
		
		TextView titleTV = new TextView(this.getActivity());
		titleTV.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		titleTV.setTypeface(_opensansRegularTypeface);
		titleTV.setTextSize(10 * getResources().getDisplayMetrics().density);
		titleTV.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
		titleTV.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
		int hTitleTVPadding = (int)(10 * getResources().getDisplayMetrics().density);  
		int vTitleTVPadding = (int)(5 * getResources().getDisplayMetrics().density);
		titleTV.setPadding(hTitleTVPadding, vTitleTVPadding, hTitleTVPadding, vTitleTVPadding);
		titleTV.setText(getName());
		builder.setCustomTitle(titleTV);
		
		builder.setMessage(getInstructions());
		builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {				
				arg0.dismiss();
			}
		}); 
		AlertDialog ad = builder.show();				
		TextView tv = (TextView) ad.findViewById(android.R.id.message);		
		tv.setTextSize(8f * getResources().getDisplayMetrics().density);
		tv.setTypeface(_opensansRegularTypeface);	
	}
	
	private void drawCanvasBackgroundBitmap(){
		if( _canvasBackgroundBitmap == null ){
			_canvasBackgroundBitmap = Bitmap.createBitmap(Math.min(_sCanvas.getWidth(), _sCanvas.getHeight()), 
				Math.max(_sCanvas.getWidth(), _sCanvas.getHeight()), 
					Bitmap.Config.ARGB_8888);
		}
		
		Canvas c = new Canvas(_canvasBackgroundBitmap);
		
		//Paint bgPaint = new Paint();
		//bgPaint.setStyle(Style.FILL);
		//bgPaint.setColor(Color.WHITE);
		c.drawColor(Color.TRANSPARENT, Mode.CLEAR);
		//c.drawRect(new Rect(0, 0, c.getWidth(), c.getHeight()), _backgroundFillPaint);
					
		drawCanvasBackgroundBitmap(c);
		
	}
	
	protected abstract void drawCanvasBackgroundBitmap(Canvas c);
	
	public RectF getCanvasRect(){
		return new RectF(0,0,_sCanvas.getWidth(), _sCanvas.getHeight());
	}
    
    public void reset(){  
    	// remove all previous results 
    	_classificationResults.clear();
    	_interrupterResults.clear();
    	
    	// this will clear the canvas and background 
    	_sCanvas.clearSCanvasView();	    
    	// refresh the background 
    	resetBackgroundBitmap();
    }
    
    public Bitmap getCanvasBitmap(){    	
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
    	
    	return _canvasBitmap;
    }
    
    public Bitmap getCanvasBitmap(RectF rect){    	
    	Bitmap bitmap = _sCanvas.getCanvasBitmap(true);
    	
    	// create a bitmap for the canvas (remove transparency) 
    	if( _canvasPaint == null ){
    		_canvasPaint = new Paint(); 
    		_canvasPaint.setColor(Color.WHITE);
    	}
    	
    	Bitmap canvasBitmap = Bitmap.createBitmap((int)rect.width(), (int)rect.height(), Config.RGB_565);
    	Canvas canvasBitmapCanvas = new Canvas(canvasBitmap);    
    	//canvasBitmapCanvas.drawColor(Color.WHITE, Mode.CLEAR);
    	
    	canvasBitmapCanvas.drawRect(new RectF(0, 0, canvasBitmap.getWidth(), canvasBitmap.getHeight()), _canvasPaint);
    	canvasBitmapCanvas.drawBitmap(bitmap, (int)-rect.left, (int)-rect.top, new Paint());    	    	
    	
    	return canvasBitmap;
    }
    
    public void setBackgroundBitmap(Bitmap bitmap){
    	_sCanvas.setBackgroundImage(bitmap);
    	_sCanvas.invalidate(); 
    }    
    
    public void resetBackgroundBitmap(){      	
    	// called each time the view needs refreshing  
    	drawCanvasBackgroundBitmap(); 
    	
    	setBackgroundBitmap(_canvasBackgroundBitmap);
    }
	
	////
	// End; helper methods 
	////
    
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
		// remove overlay
		if( _showingInstructions ){
		//	fadeOutInstructions();
		}
		
		// Update Current Color
		if(_currentTool!= TOOL_FINGER){
			_currentTool = TOOL_FINGER;
		}

		if(event.getAction()==MotionEvent.ACTION_DOWN){
			_sCanvas.setSettingViewStrokeInfo(_strokeInfoFinger);
		}
		return false;	// dispatch event to SCanvasView for drawing
	}

	@Override
	public boolean onTouchPen(View arg0, MotionEvent event) {
		// remove overlay 
		if( _showingInstructions ){
		//	fadeOutInstructions();
		}
		
		// Update Current Color
		if(_currentTool!=TOOL_PEN){
			_currentTool = TOOL_PEN;
		}
		
		if(event.getAction()==MotionEvent.ACTION_DOWN){
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
		
	}
	
	@Override
	public void onLongPressed(float eventX, float eventY) {
		
		_popupMenuView.setPosition(eventX, eventY);
		_popupMenuView.show(true);	
				
		
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
		// add listeners 
		_sCanvas.setSPenTouchListener(this);
		//_sCanvas.setSPenHoverListener(this);
		_sCanvas.setSPenDetachmentListener(this);
		_sCanvas.setSCanvasLongPressListener(this);
		_sCanvas.setSettingStrokeChangeListener(this);
		_sCanvas.setSCanvasMatrixChangeListener(this);
		
		_sCanvas.setCanvasSize(Math.min(_sCanvas.getWidth(), _sCanvas.getHeight()), Math.max(_sCanvas.getWidth(), _sCanvas.getHeight()));
		
		float widthHeightRatio = _canvasContainer.getWidth() / _canvasContainer.getHeight();
		if( widthHeightRatio > 1.0f ){
			 _sCanvas.zoomTo(widthHeightRatio);
		} else{
			_sCanvas.zoomTo(1.0f);
		}
		
		_sCanvas.setSettingViewStrokeInfo(_strokeInfoPen);	
		
		resetBackgroundBitmap();
		
		if( _savedCanvasBitmap != null ){
			_sCanvas.setBitmap(_savedCanvasBitmap, true);
			_savedCanvasBitmap.recycle(); 
			_savedCanvasBitmap = null; 
		}				
	}
	
	////
	// End; SCanvasInitializeListener 
	////
	
	////
	// Start; SCanvasMatrixChangedListener 
	////
	
	// Method descriptor #4 (Landroid/graphics/Matrix;)V
	public void onMatrixChanged(android.graphics.Matrix arg0){				
				
	}
	  
	// Method descriptor #3 ()V
	public void onMatrixChangeFinished(){
		
	}
	
	////
	// End; SCanvasMatrixChangedListener
	////
    
    ////
    // Start; abstract methods 
    ////
	
	public abstract String getName(); 
	
	public abstract boolean hasInstructionAlertBeenShown(); 
	
	public abstract void setInstructionAlertBeenShown(boolean shown);		
	
	protected abstract String getBundleCanvasBitmapKey();
	
	protected abstract String getInstructions(); 
	
	////
	// End; abstract methods 
	////
	
	public float getRelativeHorizontalPositionForScreenPosition( float x, ViewGroup container ){
		//Screen Sizes
		int xScreenSize = (getResources().getDisplayMetrics().widthPixels);
		int xLayoutSize = container.getWidth();
		
		if (xScreenSize != xLayoutSize) {
			x = x-(xScreenSize-xLayoutSize);
		}	
		
		return x; 
	}
	
	public float getRelativeVerticalPositionForScreenPosition( float y, ViewGroup container ){
		//Screen Sizes
		int yScreenSize = (getResources().getDisplayMetrics().heightPixels);
		int yLayoutSize = container.getHeight();
		
		if (yScreenSize != yLayoutSize) {
			y = y-(yScreenSize-yLayoutSize);
		}	
		
		return y; 
	}
	
}
