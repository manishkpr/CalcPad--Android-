package com.wmp.ui.circularpopup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


public class PopupMenuView extends View {

	public static float ScreenDensity = -1.0f; 
	
	public PopupMenu popupMenu;
	
	//public PopupMenuItem centreMenuItem; 		
	
	public PopupMenuDelegate delegate; 	
	
	protected float _centreX = 0.0f; 
	
	protected float _centreY = 0.0f; 
	
	public PopupMenuView(Context context) {
		super(context);
		
		if( ScreenDensity == -1.0f ){
			ScreenDensity = context.getResources().getDisplayMetrics().density;
			Log.v( "PopupMenuView", "ScreenDensity = " + ScreenDensity );
		}
	}
	
	@Override
	public void onDraw(Canvas canvas){
		
		/*if( centreMenuItem != null ){
			centreMenuItem.onDraw(canvas);
		}*/
		
		popupMenu.onDraw(canvas);
		
		if( popupMenu.isAnimating() ){
			invalidate();
		} else if( !popupMenu.isVisible() ){
			setVisibility(INVISIBLE);
		}
	}
	
	public void setPosition(float x, float y){
		//super.setX(x);
		//super.setY(y);
		
		_centreX = x; 
		_centreY = y; 
		
		if( popupMenu != null ){
			popupMenu.setPosition((int)x, (int)y);
		}
		
		/*if( centreMenuItem != null ){
			centreMenuItem.initSegments((int)x, (int)y, (int)x, (int)y, centreMenuItem.getInnerSize(), centreMenuItem.getOuterSize(), 0, 360);
		}*/
		
		invalidate();
	}
	
	@Override
	public void setX(float x){
		//super.setX(x);
		
		_centreX = x; 
		
		if( popupMenu != null ){
			popupMenu.setPosition((int)_centreX, (int)_centreY);
		}
		
		/*if( centreMenuItem != null ){
			centreMenuItem.initSegments((int)_centreX, (int)_centreY, (int)_centreX, (int)_centreY, centreMenuItem.getInnerSize(), centreMenuItem.getOuterSize(), 0, 360);
		}*/
		
		invalidate();
	}
	
	@Override
	public void setY(float y){
		//super.setY(y);
		
		_centreY = y; 
		
		if( popupMenu != null ){
			popupMenu.setPosition((int)_centreX, (int)_centreY);
		}
		
		/*if( centreMenuItem != null ){
			centreMenuItem.initSegments((int)_centreX, (int)_centreY, (int)_centreX, (int)_centreY, centreMenuItem.getInnerSize(), centreMenuItem.getOuterSize(), 0, 360);
		}*/
		
		invalidate();
	}		
	
	/*public void SetCentreMenuItem( String name, int radius, String label, Drawable icon ){
		centreMenuItem = new PopupMenuItem();
		centreMenuItem.setName(name).setMinMaxIconSize(radius, radius).setLabel(label).setIcon(icon);
		
		radius = PopupMenuView.ScalePX(radius);
		
		centreMenuItem.initSegments((int)this.getX(), (int)this.getY(), (int)this.getX(), (int)this.getY(), radius, radius, 0, 360);
	}*/
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		int state = e.getAction();
		int eventX = (int) e.getX();
		int eventY = (int) e.getY();
		
		boolean touchHandled = false;				
		
		if (state == MotionEvent.ACTION_DOWN || state == MotionEvent.ACTION_MOVE) {
			int pointerId = e.getPointerId(0);
			
			//if( centreMenuItem != null && centreMenuItem.onTouchDown(pointerId, eventX, eventY) ){
			//	touchHandled = true; 
			//} else{
				if( popupMenu.onTouchDown(pointerId, eventX, eventY) != null ){
					touchHandled = true; 
				}
			//}
		} else if (state == MotionEvent.ACTION_UP) {
			int pointerId = e.getPointerId(0);
			
			//if( centreMenuItem != null && centreMenuItem.onTouchUp(pointerId, eventX, eventY) ){
			//	touchHandled = true; 
			//	if( delegate != null ){					
			//		delegate.onPopupMenuTouched(centreMenuItem);
			//	}
			//} else{
				PopupMenuItem item = popupMenu.onTouchUp(pointerId, eventX, eventY);
				if( item != null ){
					touchHandled = true; 
					if( delegate != null ){						
						delegate.onPopupMenuTouched(item);
					}
				}
			//}				
		} else if( state == MotionEvent.ACTION_CANCEL ){
			int pointerId = e.getPointerId(0);
			
			//centreMenuItem.onCancelTouch(pointerId);
			popupMenu.onCancelTouch(pointerId); 
		}
		
		if( !touchHandled ){ 
			// if within radius then ignore i.e. return true
			float dx = _centreX - eventX;
			float dy = _centreY - eventY;
			
			float dis = (float)Math.sqrt(dx * dx + dy * dy);
			
			if( dis < popupMenu.getRadius() ){
				touchHandled = true; 
			} else{
				touchHandled = true; 
				hide(true); 
			}
		}
		
		invalidate();
		
		return touchHandled;
	}

	public static int ScalePX( int dp_size ) {
       return (int) (dp_size * ScreenDensity + 0.5f);
    }
	
	public static int GetIconSize(int iconSize, int minSize, int maxSize) {		
	    if (iconSize > minSize) {
	    	if (iconSize > maxSize) {
	    		return maxSize;
	    	} else {	//iconSize < maxSize
	    		return iconSize;
	    	}
	    } else {  //iconSize < minSize
	    	return minSize;
	    }
	}
	
	public void show(boolean animate){
		/*if( getVisibility() == VISIBLE ){
			return; // ignore 
		}*/
		
		popupMenu.show(animate);
		setVisibility(View.VISIBLE);		
	}
	
	public void hide(boolean animate){
		if( getVisibility() == INVISIBLE ){
			return; // ignore 
		}
		
		popupMenu.hide(animate);
		
		if( !animate ){			
			setVisibility(View.INVISIBLE);
		}
	}
	
}
