package com.wmp.calcpad.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import com.samsung.sdraw.CanvasView;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;

/**
 * Collection of helper methods for reading and writing to disk 
 */
@SuppressLint("DefaultLocale")
public final class FileUtil {
	
	public static final String FOLDER_ROOT = "calcpad";
	
	public static final String FOLDER_DATA = "data";
	
	public static final String FOLDER_TEMPLATES = "templates";
	
	public static final String FILE_TRAINED_DATA = "trained.dat";
	
	public static final String FILE_LABEL_DATA = "label.dat";
	
	protected static File assetFolder = null;		
	
	public static String getDataPath(){
		return FileUtil.GetDataFilePath(FOLDER_ROOT, FOLDER_DATA) + "/"; 
	}
	
	public static String getTrainingDataFilePath(){
		return FileUtil.GetDataFilePath(FOLDER_ROOT, FOLDER_DATA) + "/" + FILE_TRAINED_DATA; 
	}
	
	public static String getLabelDataFilePath(){
		return FileUtil.GetDataFilePath(FOLDER_ROOT, FOLDER_DATA) + "/" + FILE_LABEL_DATA;
	}
	
	public static String getTemplateImageFilePath(){
		return FileUtil.GetDataFilePath(FOLDER_ROOT, FOLDER_TEMPLATES) + "/";
	}
	
	/** responsible for returning the Folder for where all the contents will be saved (create one if it doesn't already exist) **/
	public static File GetAssetFolder(String appName){
		if( assetFolder == null ){
			try{
				LoadAssetFolder(appName);
			} catch( IOException e ){
				e.printStackTrace();
			}
		}
		
		return assetFolder; 
	}		
	
	public static String GetDataFilePath(String appName, String subDirectory){
		File parentFolder = GetAssetFolder(appName); 
		
		File dataFolder = new File( parentFolder.getAbsoluteFile() + "/" + subDirectory );
		
		if( !dataFolder.exists() ){
			if( !dataFolder.mkdir() ){
				Log.e("GetDataFilePath", "Unable to create app folder -> " + parentFolder.getAbsoluteFile() + subDirectory);
				return parentFolder.getAbsolutePath(); 
			}
		}
		
		return dataFolder.getAbsolutePath(); 
	}	
	
	public static String GetUniqueFilename(String path, String prefix, String extension){
		StringBuilder sb = new StringBuilder(prefix); 		
		
		int index = 0; 
		
		File tempFolder = new File( path );
		
		if( tempFolder.exists() ){
			File[] files = tempFolder.listFiles();
			
			if( files != null ){
				while(true){
					boolean found = false; 
					
					String filename = prefix + Integer.toString(index) + extension;
					
					for( int i=files.length-1; i>0 && !found; i-- ){
						if( filename.equals(files[i].getName()) ){
							found = true; 
						}
					}				
				
					if( !found ){
						break;
					}
					index++; 
				}	
			}
		}
		
		return sb.append(index).append(extension).toString(); 
	}	
	
	public static boolean DeleteFile( String filepath ){
		File file = new File( filepath );
		return file.delete();		
	}
			
	
	public static void RemoveAllFiles(String appName, String subDirectory){
		File parentFolder = GetAssetFolder(appName); 		
		RemoveAllFiles( parentFolder.getAbsoluteFile() + subDirectory );		
	}	
	
	public static void RemoveAllFiles(String path){				
		File tempFolder = new File( path );
		
		if( tempFolder.exists() ){
			File[] files = tempFolder.listFiles();
			
			if( files != null ){
				for( int i=files.length-1; i>0; i-- ){
					files[i].delete();
				}				
			}		
		}
	}	
	
	protected static void LoadAssetFolder(String appName) throws IOException{
		if( IsExternalStorageAvailable() && !IsExternalStorageReadOnly() ){
			File sdDir = Environment.getExternalStorageDirectory();
			if( sdDir.exists() && sdDir.canWrite() ){
				assetFolder = new File( sdDir.getAbsolutePath() + "/" + appName );
				if( !assetFolder.exists() ){
					if( assetFolder.mkdirs() ){
						 // create sub directories; 
						
					} else{
						Log.e("FileUtil.GetGamesFilePath", "Unable to create app folder -> " + sdDir.getAbsoluteFile() + appName);
						assetFolder = null;
					}
				}
			}
		}
		
		// cannot access external storage so use local 
		if( assetFolder == null ){
			assetFolder = new File( appName );
			
			if( !assetFolder.exists() ){
				if( !assetFolder.mkdir() ){
					Log.e("FileUtil.GetGamesFilePath", "Unable to create app folder -> " + appName);
					assetFolder = null; 
				}
			}
		}
		
		if( assetFolder == null ){
			throw new IOException("Unable to create/load folder for storing data files");
		}
	}
	
	/* 
	  * Helper Method to Test if external Storage is Available
	 */
	public static boolean IsExternalStorageAvailable() {
	    boolean state = false;
	    String extStorageState = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
	        state = true;
	    }
	    return state;
	}
	
	/**
	 * Helper Method to Test if external Storage is read only
	 */
	public static boolean IsExternalStorageReadOnly() {
	    boolean state = false;
	    String extStorageState = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
	        state = true;
	    }
	    return state;
	}

	public static byte[] ReadBytedata (String aFilename) throws IOException {
		byte[] imgBuffer = null;
		
		FileInputStream fileInputStream = null;
		try {
        	File file = new File(aFilename);
        	fileInputStream = new FileInputStream(file);
        	int byteSize = (int)file.length();
        	imgBuffer = new byte[byteSize];

            if ( fileInputStream.read(imgBuffer) == -1 ) {
            	throw new IOException("failed to read image folder");
            }
            fileInputStream.close();
		} catch (FileNotFoundException e) {
        	throw new IOException(e.toString());            
        } catch (IOException e2) {
        	throw e2;             
        } finally {
        	if(fileInputStream != null) {
	        	try{	        		
	        		fileInputStream.close();	        		
	        	} catch (IOException e) {
	            	e.printStackTrace(); 
	            } 
        	}
        }
		
        
        return imgBuffer;
	}	

	public static boolean WriteBytedata (String aFilename, byte[] imgBuffer) throws IOException {
		FileOutputStream fileOutputStream = null;
		boolean result = true;
		
		try {
        	File file = new File(aFilename);
        	fileOutputStream = new FileOutputStream(file);
        	fileOutputStream.write(imgBuffer);
        	
            fileOutputStream.close();
		} catch (FileNotFoundException e) {
        	throw new IOException(e.getMessage());         	
        } catch (IOException e2) {
        	throw e2; 
        } finally {
        	if(fileOutputStream != null) {
	        	try{	        		
	        		fileOutputStream.close();	        		
	        	} catch (IOException e) {
	            	e.printStackTrace(); 
	            	result = false;
	            } 
        	}        	
        }
		
		return result;
	}
	
	public static boolean DeleteBitmapData( String filePath ){
		boolean removed = false; 
		File f = new File(filePath);
		if( f.exists() ){
			removed = f.delete(); 
		}
		
		return removed; 
	}
	
	public static boolean StoreBitmapData(Context context, Bitmap source, String filePath, boolean setBackgroundToWhite) {
		return StoreBitmapData(context, source, filePath, setBackgroundToWhite, Bitmap.CompressFormat.JPEG);
	}
	
		
	public static boolean StoreBitmapData(Context context, Bitmap source, String filePath, boolean setBackgroundToWhite, Bitmap.CompressFormat compressFormat) {
		FileOutputStream out = null; 
		Bitmap newBitmap = null; 
		try {
			out = new FileOutputStream(filePath);
			
			if( setBackgroundToWhite ){
				//Bitmap bg = BitmapFactory.decodeResource(context.getResources(), R.drawable.image_tex);
				newBitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
				Canvas canvas = new Canvas(newBitmap);
				// #F5FFF8
				
				//canvas.drawBitmap(bg, 0, 0, null);
				canvas.drawBitmap(source, 0, 0, null);
				
				//bg.recycle(); 
				
				newBitmap.compress(compressFormat, 100, out);												
			} else{
				source.compress(compressFormat, 100, out);
			}
			
			//source.compress(Bitmap.CompressFormat.PNG, 70, out);			
			//source.compress(Bitmap.CompressFormat.PNG, 70, out);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally{
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		if( newBitmap != null ){
			newBitmap.recycle();
			newBitmap = null; 
		}

		return true;
	}
	
	public static Bitmap LoadBitmap(String filename){
		Bitmap bitmap = BitmapFactory.decodeFile(filename);
		return bitmap; 
	}	
	
	
	/*
	 * To convert the InputStream to String we use the BufferedReader.readLine()
	 * method. We iterate until the BufferedReader return null which means
	 * there's no more data to read. Each line will appended to a StringBuilder
	 * and returned as String.
	 */
	public static String ConvertStreamToString(InputStream is) {	
		BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
		StringBuilder sb = new StringBuilder();
 
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		return sb.toString();
	}
	
	/** 
	 * save image to the gallery 
	 * @param imagePath
	 * @param title
	 * @param description
	 * @param dateTaken
	 * @param orientation
	 * @param loc
	 * @return
	 */
	public static Uri SaveMediaEntry( String imagePath, String title, String description, long dateTaken, int orientation, Location loc, ContentResolver c) {		
		ContentValues v = new ContentValues();		
		v.put(Images.Media.TITLE, title);
		v.put(Images.Media.DISPLAY_NAME, title);
		v.put(Images.Media.DESCRIPTION, description);
		v.put(Images.Media.DATE_ADDED, dateTaken);
		v.put(Images.Media.DATE_TAKEN, dateTaken);
		v.put(Images.Media.DATE_MODIFIED, dateTaken) ;
		v.put(Images.Media.MIME_TYPE, "image/png");
		v.put(Images.Media.ORIENTATION, orientation);
		
		File f = new File(imagePath) ;
		File parent = f.getParentFile() ;
		
		String path = parent.toString().toLowerCase() ;
		String name = parent.getName().toLowerCase() ;
		
		v.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
		v.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
		v.put(Images.Media.SIZE,f.length()) ;
		
		f = null ;
		if( loc != null ) {
			v.put(Images.Media.LATITUDE, loc.getLatitude());
			v.put(Images.Media.LONGITUDE, loc.getLongitude());
		}
		v.put(Images.Media.DATA,imagePath);				
		return c.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
	}
	
	public static Uri SaveMediaEntry( byte[] imageData, String title, String description, long dateTaken, int orientation, Location loc, ContentResolver c) {		
		ContentValues v = new ContentValues();		
		v.put(Images.Media.TITLE, title);
		v.put(Images.Media.DISPLAY_NAME, title);
		v.put(Images.Media.DESCRIPTION, description);
		v.put(Images.Media.DATE_ADDED, dateTaken);
		v.put(Images.Media.DATE_TAKEN, dateTaken);
		v.put(Images.Media.DATE_MODIFIED, dateTaken) ;
		v.put(Images.Media.MIME_TYPE, "image/png");
		v.put(Images.Media.ORIENTATION, orientation);

		v.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, title);
		v.put(Images.Media.SIZE,imageData.length) ;
		
		//f = null ;
		if( loc != null ) {
			v.put(Images.Media.LATITUDE, loc.getLatitude());
			v.put(Images.Media.LONGITUDE, loc.getLongitude());
		}
		v.put(Images.Media.DATA,imageData);				
		return c.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
	}
}