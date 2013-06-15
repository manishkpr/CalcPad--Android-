package com.wmp.ui.circularpopup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.wmp.ui.circularpopup.PopupMenuItem.PopupMenuItemState;
import com.wmp.utils.Mathf;

import android.graphics.Canvas;
import android.util.Log;

public class PopupMenu {
	
	public enum PopupMenuState{
		Opening, 
		Open, 
		Closing, 
		Closed		
	} 
	
	public final float TRANSITION_IN_OUT_DURATION 	= 2.0f; 
	
	public final float TRANSITION_IN_OUT_PASSES	 	= 3; 
	
	private List<PopupMenuItem> _children = new ArrayList<PopupMenuItem>();
	
	public int positionX;
	
	public int positionY; 	
	
	private int _innerRadius = 30; 
    
    private int _scaledInnerRadius = PopupMenuView.ScalePX(_innerRadius);
    
    private int _outterRadius = 60; 
    
    private int _scaledOutterRadius = PopupMenuView.ScalePX(_outterRadius);
	
	private boolean _dirty = true;	
	
	private float _scale = 0.0f; 
	
	private float _alpha = 0.0f; 
	
	private PopupMenuState _state = PopupMenuState.Closed; 
	
	private long _lastDrawTime = 0; 
	
	private long _stateChangedTimestamp = 0; 
	
	public PopupMenu(PopupMenuState state){
		_state = state; 
		
		if( _state == PopupMenuState.Open ){
			_alpha = 1.0f; 
			_scale = 1.0f; 
		} else{
			_alpha = 0.0f; 
			_scale = 0.0f; 
		}
	}	
	
	public PopupMenu setInnerOuterRadius( int innerRadius, int outerRadius )
    {
       this._innerRadius = innerRadius; 
       this._outterRadius = outerRadius; 
       _dirty = true;
       
       return this; 
    }  
	
	public PopupMenu addMenuItem( PopupMenuItem item ){
		_children.add(item);
		_dirty = true; 
		
		return this; 
	}
	
	public PopupMenu setPosition(int x, int y){
		this.positionX = x; 
		this.positionY = y; 
		_dirty = true;
		
		return this; 
	}
	
	public boolean isAnimating(){
		return _state == PopupMenuState.Opening || _state == PopupMenuState.Closing; 
	}
	
	public PopupMenuState getState(){
		return _state; 
	}
	
	public void setState( PopupMenuState state ){
		if( _state == state ){
			return; 
		}			
		
		// cannot go from Opened to Opening or Closed to Closing 
		if( (_state == PopupMenuState.Open && state == PopupMenuState.Opening) || 
				(_state == PopupMenuState.Closed && state == PopupMenuState.Closing) ){
			return; 
		}
		
		_state = state; 
		_stateChangedTimestamp = System.currentTimeMillis();
		
		if( _state == PopupMenuState.Opening ){
			
		}
		else if( _state == PopupMenuState.Open ){
			_scale = 1.0f; 
			_alpha = 1.0f; 
		} 
		else if( _state == PopupMenuState.Closing ){
			
		}
		else if( _state == PopupMenuState.Closed ){
			_alpha = 0.0f; 
			_scale = 0.0f; 
		}
		
		_lastDrawTime = System.currentTimeMillis();		
	}
	
	public void onDraw(Canvas canvas){
		if( !isVisible() ){
			return; 
		}
		
		if( isAnimating() ){
			float tAlpha = 1.0f; 
			float tScale = 1.0f; 
			
			if( _state == PopupMenuState.Closing ){
				tAlpha = 0.0f; 
				tScale = 0.0f; 
			} 
			float elapsedTime = (float)(System.currentTimeMillis() - _stateChangedTimestamp)/1000.0f;			
			float percentComplete = elapsedTime / TRANSITION_IN_OUT_DURATION;
			
			_lastDrawTime = System.currentTimeMillis(); 
			
			Log.d("PopupMenu", "elapsedTime = " + elapsedTime + ", old " + tScale + ", new " + (_scale + (tScale - _alpha) * elapsedTime) + ", " + tAlpha);
			
			_alpha = Mathf.Lerp(_alpha, tAlpha, elapsedTime * TRANSITION_INOUT_SPEED);
			_scale = Mathf.Lerp(_scale, tScale, elapsedTime * TRANSITION_INOUT_SPEED);
			
			if( Math.abs(_alpha-tAlpha) < 0.1f && Math.abs(_scale-tScale) < 0.1f ){
				if( getState() == PopupMenuState.Closing ){
					setState(PopupMenuState.Closed);
				} else if( getState()  == PopupMenuState.Opening ){
					setState(PopupMenuState.Open);
				}
			}
		}
		
		canvas.save();
		
		if( _scale != 1.0f ){
			float positionXOffset = (positionX * (1.0f - _scale));
			float positionYOffset = (positionY * (1.0f - _scale));			
			canvas.translate(positionXOffset, positionYOffset );			
			canvas.scale(_scale, _scale);							
		}				
		
		if( _dirty ){
			layoutPopupMenuItems(); 
		}
		
		for( PopupMenuItem item : _children ){
			item.onDraw(canvas);
		}
		
		canvas.restore();				
	}
	
	private final float TRANSITION_INOUT_SPEED = 40.0f;  
	
	private void layoutPopupMenuItems(){
		_scaledInnerRadius = PopupMenuView.ScalePX(_innerRadius);
        _scaledOutterRadius = PopupMenuView.ScalePX(_outterRadius);
        
        //Log.v("PopupMenu", "scaledInnerRadius from " + _innerRadius + " to " + _scaledInnerRadius + ", scaledOuterRadius from " + _outterRadius + " to " + _scaledOutterRadius );
        
        if( _children.size() == 1 ){
        	float cx = positionX; 
        	float cy = positionY; 
        	
        	PopupMenuItem item = _children.get(0);
        	
        	item.initSegments((int)cx, (int)cy, positionX, positionY, _scaledInnerRadius, _scaledOutterRadius, 0, 360 );
        	
        } else{
        	int count = _children.size(); 
        	
        	float degSlice = 360.0f / (float)count;
			float start_degSlice = 270.0f - (degSlice/2);
	    	//calculates where to put the images
			double rSlice = (2*Math.PI) / (float)count;
			double rStart = (2*Math.PI)*(0.75f) - (rSlice/2);								
					
			for (int i = 0; i < count; i++) {
				
				PopupMenuItem item = _children.get(i);
				
				float cx = (float)(Math.cos(((rSlice*i)+(rSlice*0.5f))+rStart) * (_scaledOutterRadius+_scaledInnerRadius)/2)+positionX;
				float cy = (float)(Math.sin(((rSlice*i)+(rSlice*0.5f))+rStart) * (_scaledOutterRadius+_scaledInnerRadius)/2)+positionY;
				
				item.initSegments((int)cx, (int)cy, positionX, positionY, _scaledInnerRadius, _scaledOutterRadius, (i * degSlice)+start_degSlice, degSlice );				
			}
        }
	}
	
	public PopupMenuItem onTouchDown( int pointerId, int x, int y){
		PopupMenuItem touchedItem = null; 
		
		for( PopupMenuItem item : _children ){
			if( touchedItem == null ){
				if( item.onTouchDown(pointerId, x, y)){											
					touchedItem = item; 
				}
			} else{
				item.onTouchDown(pointerId, x, y); 
			}
		}
		  
		return touchedItem; 
	}
	
	public PopupMenuItem onTouchUp(int pointerId, int x, int y){
		PopupMenuItem touchedItem = null; 
		
		for( PopupMenuItem item : _children ){
			if( touchedItem == null ){
				if( item.onTouchUp(pointerId, x, y)){
					touchedItem = item;  
				}
			} else{
				item.onTouchUp(pointerId, x, y); 
			}
		} 
		
		return touchedItem; 
	}
	
	public void onCancelTouch(int pointerId){
		for( PopupMenuItem item : _children ){
			item.onCancelTouch(pointerId); 
		}  
	}
	
	public float getRadius(){
		// TODO; check for any sub menus open and add the size 
		return _outterRadius;
	}
	
	public void show( boolean animate ){
		if( animate ){
			setState(PopupMenuState.Opening);
		} else{
			setState(PopupMenuState.Open);
		}
	}
	
	public void hide( boolean animate ){
		if( animate ){
			setState(PopupMenuState.Closing);
		} else{
			setState(PopupMenuState.Closed);
		}				
	}
	
	public boolean isVisible(){
		return _state != PopupMenuState.Closed; 
	}	
}
