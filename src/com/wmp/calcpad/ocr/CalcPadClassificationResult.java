package com.wmp.calcpad.ocr;

import org.opencv.core.Rect;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.RectF;

@SuppressLint("DefaultLocale")
public class CalcPadClassificationResult {

	public final static int STATUS_NO_CONTOURS_FOUND = -1;
	
	public final static int STATUS_INVALID_PARAMETERS = -2;
	
	public final static int STATUS_GENERIC_ERROR = -3;
	
	/** associated event id **/ 
	public long eventId = -1; 
	
	/** status of classification */ 
	public int status = 0; 
	
	/** classified label **/
	public int label; 	
	
	/** contour rect **/ 
	public Rect rect; 	
	
	public Bitmap croppedBitmap;  
	
	public String toString(){
		return String.format("ClassificationResult{Status: %d, Label: %d, Rect: %s", status, label, (rect == null ? "" : rect.toString()));
	}
	
	public RectF cvRectToRectF(){
		return new RectF(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
	}
	
}
