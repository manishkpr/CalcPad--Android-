package com.wmp.calcpad.ocr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import com.wmp.calcpad.CalcPadEventConstants;
import com.wmp.calcpad.messaging.CalcPadSubscriber;
import com.wmp.calcpad.utils.FileUtil;

import android.R;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class CalcPadTrainingData extends CalcPadSubscriber {		
	
	private static CalcPadTrainingData _instance = null;
	
	private Mat _loadedTrainingData = null; 
	
	private Mat _loadedLabels = null; 
	
	public static CalcPadTrainingData getSharedInstance(){
		if( _instance == null ){
			throw new RuntimeException("CalcPadTrainingData hasn't been initilised");
		}
		
		return _instance;
	}
	
	public static CalcPadTrainingData initSharedInstance(Context context){
		if( _instance != null ){
			throw new RuntimeException("CalcPadTrainingData has already been initilised");
		}
		
		_instance = new CalcPadTrainingData(context);
		
		return _instance;
	}
	
	public static void killSharedInstace(){
		if( _instance != null ){
			_instance.cleanup(); 
		}
		_instance = null; 
	}
	
	private Context _context = null; 
	
	private CalcPadTrainingData(Context context){
		_context = context;  
	}
	
	private void cleanup(){
		
	}
	
	public Mat GetTrainingData(){
		return _loadedTrainingData; 
	}
	
	public Mat GetLabels(){
		return _loadedLabels; 
	}
	
	public void loadData(final long eventId){
		if( !hasSavedData() ){
			broadcast(eventId, CalcPadEventConstants.EVT_NO_DATA_ON_DISK);
		} else{
			new LoadTrainingData(eventId).execute(FileUtil.getTrainingDataFilePath(), FileUtil.getLabelDataFilePath(), Long.toString(eventId));
		}
	}		
	
	public boolean hasSavedData(){
		File f = new File(FileUtil.getTrainingDataFilePath());
		return f.exists();
	}
	
	public void setData(Mat trainingData, Mat trainingLabels){
		_loadedTrainingData = trainingData; 
		_loadedLabels = trainingLabels; 
	}
	
	public void saveData(final long eventId){
		if( _loadedTrainingData != null && _loadedLabels != null ){
			new SaveTrainingData(eventId).execute(FileUtil.getTrainingDataFilePath(), FileUtil.getLabelDataFilePath(), Long.toString(eventId));
		}
	}
	
	public boolean SerialiseCvMatToDisk(String filePath, Mat mat) throws IOException{
		File file = null; 
		FileOutputStream fos = null;
		ObjectOutputStream oos = null; 
			
		if( mat == null || mat.rows() == 0 || mat.cols() == 0 ){
			return false; 
		}
		
		try {
        	file = new File(filePath);
        	fos = new FileOutputStream(file);
        	oos = new ObjectOutputStream(fos);
        	
        	int cols = mat.cols();
        	int rows = mat.rows();         	
        	
        	oos.writeInt(cols);
        	oos.writeInt(rows);        	        	
        	
        	float data[] = new float[mat.channels()];
        	
        	for( int r=0; r<rows; r++ ){
        		for( int c=0; c<cols; c++ ){
        			mat.get(r, c, data);        			
        			oos.writeFloat(data[0]); 	
        		}
        	}        	        	        	      
        	
        	oos.flush();
            
            return true; 
		} catch (FileNotFoundException e) {
        	throw new IOException(e.getMessage());         	
        } catch (IOException e2) {
        	throw e2; 
        } finally {
        	if(fos != null) {
	        	try{	        		
	        		fos.close();	        		
	        	} catch (IOException e) {
	            	e.printStackTrace(); 	            	
	            } 
        	}        	
        }
	}
	
	public Mat DeserialiseCvMatFromDisk(String filePath) throws IOException{
		FileInputStream fis = null; 
		ObjectInputStream oos = null;  
		File file = null; 
		Mat mat = null; 		
		
		try {
        	file = new File(filePath);
        	
        	if( !file.exists()){
        		return null; 
        	}
        	
        	fis = new FileInputStream(file);
        	oos = new ObjectInputStream(fis);
        	
        	int cols = oos.readInt();
        	int rows = oos.readInt(); 
        	
        	if( cols == 0 || rows == 0 ){
        		return null; 
        	}
        	
        	mat = new Mat(rows, cols, CvType.CV_32FC1); 
        	
        	float data[] = new float[1];
        	
        	for( int r=0; r<rows; r++ ){
        		for( int c=0; c<cols; c++ ){
        			data[0] = oos.readFloat();
        			mat.put(r, c, data);
        		}
        	}         	        	
        	
        	oos.close();
            
            return mat; 
		} catch (FileNotFoundException e) {
        	throw new IOException(e.getMessage());         	
        } catch (IOException e2) {
        	throw e2; 
        } finally {
        	if(fis != null) {
	        	try{	        		
	        		fis.close();	        		
	        	} catch (IOException e) {
	            	e.printStackTrace(); 	            	
	            } 
        	}        	
        }
		
	}
	
	private class LoadTrainingData extends AsyncTask<String, Void, Mat[]> {
	    /** The system calls this to perform work in a worker thread and
	      * delivers it the parameters given to AsyncTask.execute() */
		
		private long _eventId = 0; 
		
		public LoadTrainingData( long eventId ){
			_eventId = eventId; 
		}
		
	    protected Mat[] doInBackground(String... filePaths) {
	    	Mat data = null; 
	    	Mat labels = null; ;
	    	
	    	try{
	    		data = DeserialiseCvMatFromDisk(filePaths[0]);
	    		labels = DeserialiseCvMatFromDisk(filePaths[1]);
	    		
	    	} catch( IOException e ){
	    		Log.e(this.getClass().getName(), "LoadTrainingData IOException; " + e.toString() );
	    	}
	        
	        return new Mat[]{data,labels};
	    }
	    
	    /** The system calls this to perform work in the UI thread and delivers
	      * the result from doInBackground() */
	    protected void onPostExecute(Mat[] result) {
	    	_loadedTrainingData = result[0]; 
	    	_loadedLabels = result[1];
	    	
	    	broadcast(_eventId, CalcPadEventConstants.EVT_DATA_LOADED_FROM_DISK);
	    	
	    }
	}
	
	private class SaveTrainingData extends AsyncTask<String, Void, Boolean> {
	    /** The system calls this to perform work in a worker thread and
	      * delivers it the parameters given to AsyncTask.execute() */
		
		private long _eventId = 0;
		
		public SaveTrainingData( long eventId){
			_eventId = eventId;
		}
		
	    protected Boolean doInBackground(String... filePaths) {	    	    		    	
	        if( _loadedLabels == null || _loadedTrainingData == null ){
	        	return Boolean.valueOf(false);
	        }
	        
	        boolean resData = false; 
	        boolean resLabels = false; 	        	        
	        
	        try{
	        	resData = SerialiseCvMatToDisk(filePaths[0], _loadedTrainingData);
	        	resLabels = SerialiseCvMatToDisk(filePaths[1], _loadedLabels);
	        	
	        } catch( IOException e ){
	        	Log.e(this.getClass().getName(), "SaveTrainingData IOException; " + e.toString() );
	        }
	        
	    	return Boolean.valueOf(resData && resLabels); 
	    }
	    
	    /** The system calls this to perform work in the UI thread and delivers
	      * the result from doInBackground() */
	    protected void onPostExecute(Boolean result) {
	        if( result.booleanValue() ){
	        	broadcast(_eventId, CalcPadEventConstants.EVT_DATA_SAVED_TO_DISK);
	        } else{
	        	broadcast(_eventId, CalcPadEventConstants.EVT_DATA_ERROR_WHILE_SAVING);
	        }
	    }
	}	
	
	////
	// Data conversion helper methods
	////
	
	public static float toFloat(byte[] data) {
		if (data == null || data.length != 4)
			return 0x0;
		// ---------- simple:
		return Float.intBitsToFloat(toInt(data));
	}

	public static float[] toFloatArray(byte[] data) {
		if (data == null || data.length % 4 != 0)
			return null;
		// ----------
		float[] flts = new float[data.length / 4];
		for (int i = 0; i < flts.length; i++) {
			flts[i] = toFloat(new byte[] { data[(i * 4)], data[(i * 4) + 1],
					data[(i * 4) + 2], data[(i * 4) + 3], });
		}
		return flts;
	}

	public static int toInt(byte[] data) {
		if (data == null || data.length != 4)
			return 0x0;
		// ----------
		return (int) ( // NOTE: type cast not necessary for int
		(0xff & data[0]) << 24 | (0xff & data[1]) << 16 | (0xff & data[2]) << 8 | (0xff & data[3]) << 0);
	}

	public static int[] toIntArray(byte[] data) {
		if (data == null || data.length % 4 != 0)
			return null;
		// ----------
		int[] ints = new int[data.length / 4];
		for (int i = 0; i < ints.length; i++)
			ints[i] = toInt(new byte[] { data[(i * 4)], data[(i * 4) + 1],
					data[(i * 4) + 2], data[(i * 4) + 3], });
		return ints;
	}
	
	public static byte[] toByteArray(float data) {
		return toByteArray(Float.floatToRawIntBits(data));
	}

	public static byte[] toByteArray(float[] data) {
		if (data == null)
			return null;
		// ----------
		byte[] byts = new byte[data.length * 4];
		for (int i = 0; i < data.length; i++)
			System.arraycopy(toByteArray(data[i]), 0, byts, i * 4, 4);
		return byts;
	}
	
}
