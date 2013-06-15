package com.wmp.ui.circularpopup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class PopupMenuItem {
	
	private static final int INVALID_POINTER_ID = -1;

	
	public enum PopupMenuItemState{
		Default, 
		Disbaled, 
		Hidden, 
		Selected
	}
		
	public int outlineColor = Color.DKGRAY; 
	public int outlineAlpha = 200;
	
	public int fillColor = Color.LTGRAY; 
	public int fillAlpha = 175;
	
	public int selectedFillColor = Color.GREEN; 
	public int selectedFillAlpha = 175;
	
	public int disabledFillColor = Color.DKGRAY; 
	public int disabledFillAlpha = 175;
	
	public int textColour = Color.BLACK;
	
	public int iconAlpha = 255; 
	
	private PopupMenuItemState _state = PopupMenuItemState.Default;
	
	private String _name = "";
	
    private String _label = null;
    
    private Drawable _icon = null;
    
    private PopupMenu _subMenu = null; 
    
    private PopupMenuItemPath _segmentBg;
    
    private Paint _paint; 
    
    private Rect _iconBounds = new Rect(); 
    
    private Rect _textBounds = new Rect(); 
    
    private int _textSize = 10; 
    
    private int _scaledTextSize = PopupMenuView.ScalePX(_textSize); 
    
    private boolean _dirty = true; 
    
    private int _centreX; 
    
    private int _centreY;         
    
    private int _minIconSize = 15; 
    
    private int _scaledMinIconSize = PopupMenuView.ScalePX(_minIconSize);
    
    private int _maxIconSize = 35;
    
    private int _scaledMaxIconSize = PopupMenuView.ScalePX(_maxIconSize);
    
    private int _lineSpacing = 3; 
    
    private int _scaledLineSpacing = PopupMenuView.ScalePX(_lineSpacing);
    
    private int _padding = 3;
    
    private int _scaledPadding = PopupMenuView.ScalePX(_padding);
    
    private int _pointerId = INVALID_POINTER_ID; 
    
    public PopupMenuItem(){    	
    	_paint = new Paint();
    	_paint.setAntiAlias(true);
    	_paint.setStrokeWidth(3);
    }
    
    public int getTextSize(){
    	return _textSize;
    }
    
    public PopupMenuItem setTextSize(int textSize){
    	_textSize = textSize; 
    	_dirty = true; 
    	return this; 
    }        
    
    public PopupMenuItem setMinMaxIconSize( int min, int max ){
    	_minIconSize = min; 
    	_maxIconSize = max; 
    	_dirty = true;  
    	return this; 
    }
    
    public int getCentreX(){
    	return _centreX; 
    }
    
    public int getCentreY(){
    	return _centreY;
    }
    
    public PopupMenuItemState getState(){
    	return _state; 
    }
    
    public PopupMenuItem setState(PopupMenuItemState state){
    	if( _state == state ){
    		return this; 
    	}
    	
    	_state = state; 
    	_dirty = true;
    	
    	if( _state == PopupMenuItemState.Selected ){
    		showSubMenu();
    	}
    	
    	return this; 
    }
    
    public void showSubMenu(){
    	if( _subMenu != null ){
    		
    	}
    }
    
    public void hideSubMenu(){
    	if( _subMenu != null ){
    		
    	}
    }
    
    public void onDraw(Canvas canvas){
    	if( this._state == PopupMenuItem.PopupMenuItemState.Hidden ){
    		return; 
    	}
    	
    	if( _dirty ){
    		refreshView(); 
    	}
    	
    	_paint.setColor(outlineColor);
    	_paint.setAlpha(outlineAlpha); 
    	_paint.setStyle(Paint.Style.STROKE);    	
    	canvas.drawPath(_segmentBg, _paint);
    	
    	if( _state == PopupMenuItem.PopupMenuItemState.Default ){
    		_paint.setColor(fillColor);
    		_paint.setAlpha(fillAlpha);
    		_paint.setStyle(Paint.Style.FILL);
	    	canvas.drawPath(_segmentBg, _paint);
    	} else if( _state == PopupMenuItem.PopupMenuItemState.Disbaled ){
    		_paint.setColor(disabledFillColor);
    		_paint.setAlpha(disabledFillAlpha);
    		_paint.setStyle(Paint.Style.FILL);
	    	canvas.drawPath(_segmentBg, _paint);
    	} else if( _state == PopupMenuItem.PopupMenuItemState.Selected ){
    		_paint.setColor(selectedFillColor);
    		_paint.setAlpha(selectedFillAlpha);
    		_paint.setStyle(Paint.Style.FILL);
	    	canvas.drawPath(_segmentBg, _paint);
    	}
    	
    	if( getIcon() != null && getLabel() != null ){ // label and icon     		
    		onDrawLabel( canvas );
    		onDrawIcon( canvas );     		
    	} else if( getIcon() != null ){ // icon only 
    		onDrawIcon( canvas ); 
    	} else if( getLabel() != null ){ // label only 
    		onDrawLabel( canvas );
    	}    	
    }
    
    private void onDrawLabel( Canvas canvas ){
    	// split string based on the newline ('\n') character
    	String menuItemName = getLabel();
		String[] stringArray = menuItemName.split("\n");
		
		_paint.setColor(textColour);		
		_paint.setAlpha( _state == PopupMenuItemState.Disbaled ? disabledFillAlpha : selectedFillAlpha );
		_paint.setStyle(Paint.Style.FILL);
		_paint.setTextSize(_scaledTextSize);				
		
		float top = _textBounds.top; 
		Rect rect = new Rect(); 
		for( int i=0; i<stringArray.length; i++ ){
			_paint.getTextBounds(stringArray[i],0,stringArray[i].length(),rect);
			float textLeft = _textBounds.centerX() - rect.width()/2;
			canvas.drawText(stringArray[i], textLeft-rect.left, top + rect.height(), _paint);
			top += rect.height() + _scaledLineSpacing; 
		}
		
    }
    
    private void onDrawIcon( Canvas canvas ){
    	Drawable drawable = getIcon();    	
		drawable.setBounds(_iconBounds);
		drawable.setAlpha(_state == PopupMenuItemState.Disbaled ? disabledFillAlpha : iconAlpha);				
		drawable.draw(canvas);				
    }
    
    private void refreshView(){
    	// adjust sizes pased on screen density 
    	_scaledTextSize = PopupMenuView.ScalePX(_textSize);    	    	
        
        _scaledMinIconSize = PopupMenuView.ScalePX(_minIconSize);
        _scaledMaxIconSize = PopupMenuView.ScalePX(_maxIconSize);
    	
    	// work out icon rect
        if( _iconBounds == null ){
			_iconBounds = new Rect(); 
		}
        
        int h = _scaledMaxIconSize; 
        int w = _scaledMaxIconSize;                  
        
    	if( getIcon() != null ){
    		if( _iconBounds == null ){
    			_iconBounds = new Rect(); 
    		}
			
		    Drawable drawable = getIcon();

		    h = PopupMenuView.GetIconSize(drawable.getIntrinsicHeight(),_scaledMinIconSize,_scaledMaxIconSize);
		    w = PopupMenuView.GetIconSize(drawable.getIntrinsicWidth(),_scaledMinIconSize,_scaledMaxIconSize);		    		     
    	}
    	
    	_iconBounds.set(_centreX-w/2, _centreY-h/2, _centreX+w/2, _centreY+h/2);
    	
    	// work out text rect 
    	if( getLabel() != null ){
    		String menuItemName = getLabel();
    		String[] stringArray = menuItemName.split("\n");
    		
    		_paint.setTextSize(_scaledTextSize);
			
    		float textHeight = 0;  
    		Rect rect = new Rect();  					
			for (int j = 0; j < stringArray.length; j++){
				_paint.getTextBounds( stringArray[j],0,stringArray[j].length(),rect );
				textHeight = textHeight+( rect.height() + _scaledLineSpacing );
		    }

			if( _textBounds == null ){
				_textBounds = new Rect(); 
			}
			
			if( getIcon() == null ){		
				_textBounds.set(_iconBounds.left, _iconBounds.centerY()-((int)textHeight/2), _iconBounds.right, _iconBounds.centerY() + ((int)textHeight/2));
			} else{
				// modify both 
				float totalHeight = _iconBounds.height() + _scaledPadding + textHeight; 
				// place the icon at the top 
				_iconBounds.set(_iconBounds.left, _centreY - (int)(totalHeight/2), _iconBounds.right, _centreY - (int)(totalHeight/2) + h);				
				_textBounds.set(_iconBounds.left, _iconBounds.bottom + _scaledPadding, _iconBounds.right, _iconBounds.bottom + _scaledPadding + (int)textHeight);
			}
    	}
    	
    	_dirty = false; 
    }
    
    public int getInnerSize(){
    	return _segmentBg != null ? _segmentBg._innerSize : 0; 
    }
    
    public int getOuterSize(){
    	return _segmentBg != null ? _segmentBg._innerSize : 0; 
    }
    
    public void initSegments(int cx, int cy, int x, int y, int innerSize, int outerSize, float startArc, float arcWidth){
    	this._centreX = cx; 
    	this._centreY = cy;
    	
    	this._segmentBg = new PopupMenuItemPath(x, y, innerSize, outerSize, startArc, arcWidth);
    	
    	_dirty = true; 
    }
    
    class PopupMenuItemPath extends Path {
		
    	private int _x, _y; 
    	private int _innerSize, _outerSize; 
    	private float _startArc; 
    	private float _arcWidth;
    	
    	private float _startArcRadians; 
    	private float _arcWidthRadians;
    	
    	public PopupMenuItemPath(int x, int y, int innerSize, int outerSize, float startArc, float arcWidth){
    		super(); 
    		
    		if( startArc >= 360 ){
    			startArc = startArc - 360; 
    		}
    		
    		this._x = x; 
    		this._y = y; 
    		this._innerSize = innerSize; 
    		this._outerSize = outerSize;
    		this._startArc = startArc; 
    		this._arcWidth = arcWidth;
    		
    		this._startArcRadians = (float)Math.toRadians(startArc);
    		this._arcWidthRadians = (float)Math.toRadians(arcWidth);
    		
    		buildPath(); 
    	}
    	
    	private void buildPath(){
    		
    		this.reset();
    		
    		
    		if( _innerSize == _outerSize ){
    			this.addCircle(_x, _y, _innerSize, Direction.CCW);
    		} else if( (_arcWidth-_startArc) == 360){
    			final RectF rect = new RectF();
    			final RectF rect2 = new RectF();
    			
    			rect.set(this._x-this._innerSize, this._y-this._innerSize, this._x+this._innerSize, this._y+this._innerSize);
    			rect2.set(this._x-this._outerSize, this._y-this._outerSize, this._x+this._outerSize, this._y+this._outerSize);
    			
    			this.addCircle(_x, _y, _outerSize, Direction.CCW);
    			this.addCircle(_x, _y, _innerSize, Direction.CW);
    			
    			//this.arcTo(rect2, _startArc, _arcWidth);
    			//this.arcTo(rect, _startArc+_arcWidth, -_arcWidth);
    		}else{
	    		final RectF rect = new RectF();
	       	    final RectF rect2 = new RectF();       	           	    
	       	    
	       	    //Rectangles values
	       	    rect.set(this._x-this._innerSize, this._y-this._innerSize, this._x+this._innerSize, this._y+this._innerSize);
	       	    rect2.set(this._x-this._outerSize, this._y-this._outerSize, this._x+this._outerSize, this._y+this._outerSize);       	   		       		
	
	       		this.arcTo(rect2, _startArc, _arcWidth);
	       		this.arcTo(rect, _startArc+_arcWidth, -_arcWidth);
    		}
       				
       		this.close();
    	}	
    }        
    
    public PopupMenuItem setName(String name){
    	this._name = name; 
    	_dirty = true; 
    	return this; 
    }
    
    public String getName(){
    	return _name; 
    }
	
    public PopupMenuItem setLabel(String label){
    	this._label = label;
    	_dirty = true; 
    	return this; 
    }
    
    public String getLabel(){
    	return _label; 
    }
    
    public Drawable getIcon(){
    	return _icon; 
    }
    
    public PopupMenuItem setIcon( Drawable icon){
    	this._icon = icon; 
    	_dirty = true; 
    	return this; 
    }
    
    public PopupMenu getSubMenu(){
    	return _subMenu;     	
    }
    
    public PopupMenuItem setSubMenu(PopupMenu subMenu){
    	_subMenu = subMenu; 
    	_dirty = true; 
    	return this; 
    }
    
    public boolean onTouchDown( int pointerId, int x, int y){
    	if( _state == PopupMenuItemState.Disbaled ){
			return false; 
		}
    	
		if( isTouching(x, y) ){		
			_pointerId = pointerId;
			setState(PopupMenuItemState.Selected);			
			return true; 
		} else if( _pointerId != INVALID_POINTER_ID ){
			if( _state == PopupMenuItemState.Selected ){
				setState(PopupMenuItemState.Default);
			}
		}
		
		return false; 
	}
	
	public boolean onTouchUp(int pointerId, int x, int y){
		if( _state == PopupMenuItemState.Disbaled ){
			return false; 
		}
		
		if( isTouching(x, y) && _pointerId == pointerId ){
			// on touched
			_pointerId = INVALID_POINTER_ID; 
			setState(PopupMenuItemState.Default);
			return true; 
		} else if( _pointerId == pointerId ){
			_pointerId = INVALID_POINTER_ID;
			
			if( _state == PopupMenuItemState.Selected ){
				setState(PopupMenuItemState.Default);
			}
		}
		
		return false; 
	}
	
	public void onCancelTouch(int pointerId){
		if( _state == PopupMenuItemState.Disbaled ){
			return; 
		}
		
		_pointerId = INVALID_POINTER_ID; 
		
		setState(PopupMenuItemState.Default);
	}
	
	public boolean isTouching(int x, int y){
		float dx = x-_segmentBg._x;
		float dy = y-_segmentBg._y;
		
		float angle = (float)Math.atan2(dy,dx);
		
		if (angle < 0){
		  angle += (2*Math.PI);
		}
		
		float startAngle = _segmentBg._startArcRadians;
		float sweepAngle = _segmentBg._arcWidthRadians;

		if (startAngle >= (2*Math.PI)) {
			startAngle = (float) (startAngle-(2*Math.PI));
		}
		
		//checks if point falls between the start and end of the wedge
		if ((angle >= startAngle && angle <= startAngle + sweepAngle) ||
				(angle+(2*Math.PI) >= startAngle && (angle+(2*Math.PI)) <= startAngle + sweepAngle)) {
		    
			// checks if point falls inside the radius of the wedge
			float dist = dx*dx + dy*dy;
			if (dist < _segmentBg._outerSize*_segmentBg._outerSize && (_segmentBg._innerSize == _segmentBg._outerSize || dist > _segmentBg._innerSize*_segmentBg._innerSize)) {
				return true;
			}
		}
		
		return false; 
	}
	
}
