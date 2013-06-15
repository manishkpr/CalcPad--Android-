package com.wmp.calcpad.ocr;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.CvKNearest;
import org.opencv.ml.CvSVM;
import org.opencv.ml.CvSVMParams;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Handler;

import com.wmp.calcpad.CalcPadEventConstants;
import com.wmp.calcpad.messaging.CalcPadSubscriber;
import com.wmp.calcpad.utils.FileUtil;

public class CalcPadOCR extends CalcPadSubscriber {	
	
	public enum ClassificationTypeEnum{
		SVM, 
		KNN
	}
	
	private static CalcPadOCR _instance = null; 
	
	public static CalcPadOCR getSharedInstance(){
		if( _instance == null ){
			throw new RuntimeException("CalcPadOCR hasn't been initilised");
		}
		
		return _instance;
	}
	
	public static CalcPadOCR initSharedInstance(Context context){
		if( _instance != null ){
			throw new RuntimeException("CalcPadOCR has already been initilised");
		}
		
		_instance = new CalcPadOCR(context);
		
		return _instance;
	}
	
	public static void killSharedInstace(){
		if( _instance != null ){
			_instance.cleanup();
			_instance = null; 
		}
	}
	
	private boolean _saveTemplateImagesToDisk = false; 
	
	private int classificationImageSize = 40;
	
	private int threshold = 200; 
	
	private Context _context = null;
	
	private Mat _trainingData = null; 
	
	private Mat _labels = null; 
	
	private ClassificationTypeEnum _classificationType = ClassificationTypeEnum.SVM;
	
	private CvSVM _svm = null; 
	
	private CvKNearest _knn; 
	
	private int _k = 10; 
	
	private boolean _dirty = true;
	
	// working variables for processing the images for training and classification  
	
	private Mat _wrkGreyMat = null;
	
	private Mat _wkrBlurredMat = null; 
	
	private Mat _wrkThresholdMat = null; 
	
	private Mat _wrkOpenMorhMat = null; 
	
	private List<MatOfPoint> _wrkContours = null;
	
	private Mat _wrkHierarchy = null; 
	
	private CalcPadClassificationResult _classificationResult;		
	
	// some thread variables 
	
	private Handler _handler; 
	
	private boolean _busy = false; 
	
	private boolean _debugClassifiedResult = true; 
	
	private CalcPadOCR(Context context){
		_context = context; 	
		_handler = new Handler();
	}	
	
	private void cleanup(){
		if( _wrkGreyMat != null ){
			_wrkGreyMat.release(); 
		}
		_wrkGreyMat = null;
		
		if( _wkrBlurredMat != null ){
			_wkrBlurredMat.release(); 
		}
		_wkrBlurredMat = null;
		
		if( _wrkThresholdMat != null ){
			_wrkThresholdMat.release(); 
		}
		_wrkThresholdMat = null;
		
		if( _wrkOpenMorhMat != null ){
			_wrkOpenMorhMat.release(); 
		}
		_wrkOpenMorhMat = null;
		
		if( _wrkContours != null ){
			_wrkContours.clear(); 
		}
		_wrkContours = null;
		
		if( _wrkHierarchy != null ){
			_wrkHierarchy.release(); 
		}
		_wrkHierarchy = null;
	}
	
	public boolean isDebugingClassifiedResult(){
		return _debugClassifiedResult; 
	}
	
	public void setDebugClassifiedResult(boolean debug){
		_debugClassifiedResult = debug; 
	}
	
	public void SetTrainingDataAndLabels(Mat trainingData, Mat labels){
		if( _trainingData != null ){
			_trainingData.release();
		}
		
		_trainingData = trainingData; 
		
		if( _labels != null ){
			_labels.release(); 
		}
		
		_labels = labels; 
	}
	
	public boolean isBusy(){
		return _busy; 
	}
	
	public boolean isReady(){
		return !_busy && _trainingData != null && _labels != null; 
	}
	
	public Mat getTrainingData(){
		return _trainingData; 
	}
	
	public Mat getLabels(){
		return _labels; 
	}
	
	public Mat getCurrentGreyAsCvMat(){
		return _wrkGreyMat; 
	}
	
	public Bitmap getCurrentGreyAsBitmap(){
		return CreateBitmapFromCvMat(_wrkGreyMat); 
	}
	
	public Mat getCurrentThresholdAsCvMat(){
		return _wrkThresholdMat; 
	}
	
	public Bitmap getCurrentThresholdAsBitmap(){
		return CreateBitmapFromCvMat(_wrkThresholdMat); 
	}
	
	public Mat getCurrentOpenMorhAsCvMat(){
		return _wrkOpenMorhMat; 
	}
	
	public Bitmap getCurrentOpenMorhAsBitmap(){
		return CreateBitmapFromCvMat(_wrkOpenMorhMat); 
	}
	
	public boolean FlushTrainingDataAndLabels(final long eventId){
		if( _busy ){
			return false; 
		}
		
		_busy = true; 
		
		new Thread(new Runnable() {
	        public void run() {
	        	DoFlush(); 
	        	
	            _handler.post(new Runnable() {
	                public void run() {
	                	_busy = false; 
	                    broadcast(eventId, CalcPadEventConstants.EVT_TRAINING_DATA_FLUSHED);
	                }
	            });
	        }
	    }).start();
		
		return true; 
	}
	
	private void DoFlush(){
		if( _trainingData != null ){
			_trainingData.release();
		}
		_trainingData = null; 
		
		if( _labels != null ){
			_labels.release();
		}
		_labels = null;
		
		FileUtil.RemoveAllFiles(FileUtil.getTemplateImageFilePath());
	}
	
	public CalcPadClassificationResult getClassificationResult(){
		if( _busy ){
			return null; 
		}
		
		return _classificationResult; 
	}
	
	public boolean TrainFromDisk(final long eventId){	
		if( _busy ){
			return false; 
		}
		
		_busy = true; 
		
		new Thread(new Runnable() {
	        public void run() {
	        	DoTrainFromDisk(); 
	        	
	            _handler.post(new Runnable() {
	                public void run() {
	                    _busy = false; 
	                    broadcast(eventId, CalcPadEventConstants.EVT_LOADED_FROM_DISK_COMPLETED);
	                }
	            });
	        }
	    }).start();
		
		return true; 
	}
	
	private void DoTrainFromDisk(){
		Mat imgSrc; 
		
		// iterate through all images
		String path = "images/trainingpng/";
		StringBuffer sb = new StringBuffer();
		
		for(int label = 0; label<10; label++){
			for( int sampleIndex = 0; sampleIndex< 50; sampleIndex++){
				if( sb.length() > 0 ){			
					sb.delete(0,sb.length());
				}
				sb.append(path);
				
				// img001-001
				sb.append("img0");
				
				if( label < 9 ){
					sb.append(String.format("0%d-", label+1));
				} else{
					sb.append(String.format("%d-", label+1));
				}								
				
				if( sampleIndex < 9 ){
					sb.append(String.format("00%d.png", sampleIndex+1));
				} else{
					sb.append(String.format("0%d.png", sampleIndex+1));
				}
								
				//Load file								
				imgSrc = loadMatFromAssets(sb.toString());
				
				// train file 
				DoTrain(imgSrc, label);
			}
		}
	} 
	
	public boolean Train( final long eventId, final Bitmap bitmap, final int label ){
		if( _busy ){
			return false; 
		}
		
		_busy = true; 
		
		new Thread(new Runnable() {
	        public void run() {
	        	DoTrain(bitmap, label); 
	        	
	            _handler.post(new Runnable() {
	                public void run() {
	                    _busy = false; 
	                    broadcast(eventId, CalcPadEventConstants.EVT_TRAINING_COMPLETED);
	                }
	            });
	        }
	    }).start();
		
		return true; 
	}
	
	private boolean DoTrain(Bitmap bitmap, int label){
		Mat mat = CreateCvMatFromBitmap(bitmap);
		
		return DoTrain(mat, label);
	}
	
	//private int _currentIndex = 0;
	
	private boolean DoTrain(Mat mat, int label){		
		// create our matrixes to hold the trained data and associated labels
		if( _trainingData == null ){
			_trainingData = new Mat(1, classificationImageSize * classificationImageSize, CvType.CV_32FC1); // trained data
			//_trainingData = new Mat( 10 * 50, classificationImageSize * classificationImageSize, CvType.CV_32FC1); // trained data
		} else{		 
			//_trainingData.create(_trainingData.rows() + 1, classificationImageSize * classificationImageSize, CvType.CV_32FC1);
			//Imgproc.resize(_trainingData, _trainingData, new Size(classificationImageSize * classificationImageSize, _trainingData.rows() + 1));
			
			int rows = _trainingData.rows(); 
			Mat newTrainingData = new Mat(rows + 1, _trainingData.cols(), _trainingData.type());
			for( int r=0; r<rows; r++ ){
				_trainingData.row(r).copyTo(newTrainingData.row(r)); 
			}
			
			_trainingData.release(); 
			_trainingData = newTrainingData; 
		}
		
		if( _labels == null ){
			_labels = new Mat(1, 1, CvType.CV_32FC1); // labels
			//_labels = new Mat(10 * 50, 1, CvType.CV_32FC1); // labels
		} else{
			int rows = _labels.rows(); 
			Mat newLabels = new Mat(rows + 1, 1, CvType.CV_32FC1);

			for( int r=0; r<rows; r++ ){
				_labels.row(r).copyTo(newLabels.row(r)); 
			}
			
			_labels.release(); 
			_labels = newLabels;			
		}								
		
		int countourCount = preProcess(mat);
		
		if( countourCount <= 0 ){
			return false; 
		}				
		
		for( int i=0; i<countourCount; i++ ){
			
			Rect rect = Imgproc.boundingRect(_wrkContours.get(i));
			Mat croppedMat = preProcessResize(_wrkOpenMorhMat, rect, classificationImageSize, classificationImageSize);
			Mat classificationReadyMat = preProcessForClassification(croppedMat);
			
			if( _saveTemplateImagesToDisk ){
				saveMatToDisk(croppedMat, FileUtil.getTemplateImageFilePath(), Integer.toString(label));
			}
			
			int rowIndex = _labels.rows()-1;
			
			// set class label 
			Mat row = _labels.row(rowIndex);
			row.setTo(new Scalar((float)label));

			// set data 
			row = _trainingData.row(rowIndex);
			
			classificationReadyMat.copyTo(row);
			
			// not the end then extend our trainingData and labels arrays 
			if( i < countourCount-1 ){	
				// increase training data by 1 
				int rows = _trainingData.rows(); 
				Mat newTrainingData = new Mat(rows + 1, _trainingData.cols(), _trainingData.type());
				for( int r=0; r<rows; r++ ){
					_trainingData.row(r).copyTo(newTrainingData.row(r)); 
				}
				
				_trainingData.release(); 
				_trainingData = newTrainingData;
				
				// increase labels by 1 
				rows = _labels.rows(); 
				Mat newLabels = new Mat(rows + 1, 1, CvType.CV_32FC1);

				for( int r=0; r<rows; r++ ){
					_labels.row(r).copyTo(newLabels.row(r)); 
				}
				
				_labels.release(); 
				_labels = newLabels;												
			}
		}		
		
		_dirty = true;		
		
		return true; 
	}
	
	public boolean Classify( final long eventId, final Bitmap bitmap, final List<CalcPadClassificationResult> results){
		if( _busy ){
			return false; 
		}
		
		// do we have data 
		if( _trainingData.empty() || _labels.empty() ){
			broadcast(eventId, CalcPadEventConstants.EVT_NO_TRAINING_DATA);
			return false; 
		}
		
		_busy = true; 
		
		new Thread(new Runnable() {
	        public void run() {
	        	_classificationResult = DoClassify(eventId, bitmap, results);
	        	
	            _handler.post(new Runnable() {
	                public void run() {
	                   _busy = false; 
	                    broadcast(eventId, CalcPadEventConstants.EVT_CLASSIFICATION_COMPLETED);
	                }
	            });
	        }
	    }).start();
		
		return true; 
	}
	
	private CalcPadClassificationResult DoClassify(long eventId, Bitmap bitmap, List<CalcPadClassificationResult> results){
		
		if( _dirty ){
			updateClassifier();
			_dirty = false; 
		}
		
		Mat sourceMat = CreateCvMatFromBitmap(bitmap);
		
		return DoClassify(eventId, sourceMat, results);
	}
	
	private CalcPadClassificationResult DoClassify(long eventId, Mat srcImage, List<CalcPadClassificationResult> results){
		if( _trainingData == null || _labels == null ){
			throw new RuntimeException("CalcPadOCR hasn't been initilised");
		}
		
		CalcPadClassificationResult res = new CalcPadClassificationResult();
		res.eventId = eventId; 
		
		int countourCount = preProcess(srcImage);
		
		if( countourCount <= 0 ){
			res.status = CalcPadClassificationResult.STATUS_NO_CONTOURS_FOUND; 				
			return res; 
		}
		
		if( countourCount > 1 && results == null){
			res.status = CalcPadClassificationResult.STATUS_INVALID_PARAMETERS;
			return res;
		}				 		
		
		for( int i=0; i<countourCount; i++ ){
			res = new CalcPadClassificationResult();
			res.eventId = eventId;
			
			Rect rect = Imgproc.boundingRect(_wrkContours.get(i));
			Mat croppedMat = preProcessResize(_wrkOpenMorhMat, rect, classificationImageSize, classificationImageSize);
			Mat classificationReadyMat = preProcessForClassification(croppedMat);
			
			float predictionResult = -1; 
			
			// classify
			if( _classificationType == ClassificationTypeEnum.SVM ){
				predictionResult = _svm.predict(classificationReadyMat, false);
			} else{
				Mat nearest = new Mat( 1,_k, CvType.CV_32FC1 );
				float result;
				
				// result=knn->find_nearest(row1,K,0,0,nearest,0);
				//_knn.find_nearest(classificationReadyMat, _k, result, neighborResponses, dists)
				result = _knn.find_nearest(classificationReadyMat, _k, nearest, new Mat(), new Mat());
				
				int accuracy=0;
				float[] data = {0}; 
				for(int j=0;j<_k;j++){
					nearest.get(0, j, data);
					if( data[0] == result){
						accuracy++;
					}					
				}
				
				float pre=100*((float)accuracy/(float)_k);
				
				//Log.v("CalcPadOCR.DoClassify", "result " + result + ", accuracy (per) " + pre );
				
				predictionResult = result;
			}
			
			res.rect = rect; 
			res.label = (int)predictionResult;
			
			if( _debugClassifiedResult ){
				res.croppedBitmap = CreateBitmapFromCvMat(croppedMat);
			}
			
			if( results != null ){								
				results.add(res);
			}			
		}
				
		return res; 
	}
	
	/**
	 * 1. grey scale (_wrkGreyMat)
	 * 2. threshold (_wrkThresholdMat) 
	 * 3. apply morfologic filgers (close/open)  (_wrkOpenMorhMat) 
	 * 4. find contours (_wrkContours, _wrkHierarchy)  
	 * @param sourceMat
	 */
	private int preProcess(Mat sourceMat){
		// 1. convert to greyscale 
		if( _wrkGreyMat == null ){
			_wrkGreyMat = new Mat(sourceMat.height(), sourceMat.width(), CvType.CV_8UC1);
		}
		Imgproc.cvtColor(sourceMat, _wrkGreyMat, Imgproc.COLOR_RGB2GRAY);
		
		// 2. threshold 
		if( _wrkThresholdMat == null || _wrkThresholdMat.width() != _wrkGreyMat.width() || _wrkThresholdMat.height() != _wrkGreyMat.height() ){
			_wrkThresholdMat = new Mat(_wrkGreyMat.height(), _wrkGreyMat.width(), CvType.CV_8UC1);
		}
		
		Imgproc.threshold(_wrkGreyMat, _wrkThresholdMat, threshold, 255, Imgproc.THRESH_BINARY_INV);
		
		// 3. Morfologic filters
		if( _wrkOpenMorhMat == null || _wrkOpenMorhMat.width() != _wrkGreyMat.width() || _wrkOpenMorhMat.height() != _wrkGreyMat.height() ){
			_wrkOpenMorhMat = new Mat(_wrkGreyMat.height(), _wrkGreyMat.width(), CvType.CV_8UC1);
		}
		
		Imgproc.dilate(_wrkThresholdMat, _wrkOpenMorhMat, new Mat());		
		Imgproc.erode(_wrkOpenMorhMat, _wrkOpenMorhMat, new Mat());
		
		// so far we've converted the input image to a greyscale image, blurred it, and applied a threshold to it
		// ... now lets find contours
		if( _wrkContours == null ){
			_wrkContours = new Vector<MatOfPoint>();
		} else{
			_wrkContours.clear();			
		}
		
		//if( _wrkHierarchy == null ){
			_wrkHierarchy = new Mat(); 
		//}
		
		// clone the last processed mat before finding contours 
		Mat contourTargetMat = _wrkOpenMorhMat.clone();
		
		Imgproc.findContours(contourTargetMat, _wrkContours, _wrkHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		// simplifier the contours 
		int contoursCount = _wrkContours.size();
		
		MatOfPoint2f contour2f = new MatOfPoint2f();
		
		for( int i=0; i<contoursCount; i++ ){			
			//Convert contours(i) from MatOfPoint to MatOfPoint2f
			_wrkContours.get(i).convertTo(contour2f, CvType.CV_32FC2);
		    //Processing on mMOP2f1 which is in type MatOfPoint2f
		    Imgproc.approxPolyDP(contour2f, contour2f, 1, true); 
		    //Convert back to MatOfPoint and put the new values back into the contours list
		    contour2f.convertTo(_wrkContours.get(i), CvType.CV_32S);		    						
		}
		
		return contoursCount; 
	}
	
	/**
	 * return a new Mat from the sourceMat and sourceRect of size newWidth and newHeight 
	 * @param sourceMat
	 * @param sourceRect
	 * @param newWidth
	 * @param newHeight
	 * @return
	 */
	private Mat preProcessResize(Mat sourceMat, Rect sourceRect, int newWidth, int newHeight){
		Mat data = null; 
		Mat dataA = null; 
		Mat res = null; 
		Mat scaledRes = null; 
		
		// Get bounding box data and no with aspect ratio, the x and y can be corrupted
		data = sourceMat.submat(sourceRect);		
		
		// Create image with this data with width and height with aspect ratio 1 
		// then we get highest size betwen width and height of our bounding box
		int size = Math.max(sourceRect.width, sourceRect.height);
		res = new Mat(new Size(size, size), CvType.CV_8UC1 );		
		res.setTo(new Scalar(0, 0, 0), new Mat());
				
		//Copy de data in center of image
		int x=(int)Math.floor((float)(size-sourceRect.width)/2.0f);
		int y=(int)Math.floor((float)(size-sourceRect.height)/2.0f);
		
		dataA = res.submat(new Rect(x, y, sourceRect.width, sourceRect.height));
		data.copyTo(dataA);
				
		//Scale result
		scaledRes = new Mat(new Size(newWidth, newHeight), CvType.CV_8UC1 );		
		Imgproc.resize(res, scaledRes, new Size(newWidth, newHeight));		
		
		//Return processed data
		return scaledRes;
	}
	
	/**
	 * converts the Mat for classification (training and classification); creates a new Mat whose type if CV_32FC1 (float which is resized to 1 column) 
	 * @param processedMat
	 * @return
	 */
	private Mat preProcessForClassification(Mat processedMat){
		//Set data
		Mat data = null; 
		Mat row_header = null;								
		
		Mat img = new Mat(new Size(classificationImageSize, classificationImageSize), CvType.CV_32FC1);

		//convert 8 bits image to 32 float image (source image, destination, scale, shift)		
		processedMat.convertTo(img, CvType.CV_32FC1, 0.0039215);		

		// get all the data 
		data = img.submat(new Rect(0, 0, classificationImageSize, classificationImageSize));
		
		//convert data matrix sizexsize to vecor
		row_header = data.reshape(0, 1);
		
		return row_header; 
	}
	
	
	private void updateClassifier(){
		if( _classificationType == ClassificationTypeEnum.SVM ){
			updateSVM(); 
		} else{
			updateKNN(); 
		}
	}
	
	private void updateKNN(){
		if( _knn == null ){
			_knn = new CvKNearest(_trainingData, _labels);
		} else{
			_knn.train(_trainingData, _labels);
		}
	}
	
	/**
	 * Updates the _svm with assigned training data and associated labels 
	 */
	private void updateSVM(){	
				
		// Set up SVM's parameters
		CvSVMParams params = new CvSVMParams();
	    params.set_svm_type(CvSVM.C_SVC);
	    params.set_kernel_type(CvSVM.LINEAR);
	    params.set_term_crit(new TermCriteria(TermCriteria.COUNT, 100, 1e-6));	    

	    // Train the SVM
	    if( _svm == null ){
	    	_svm = new CvSVM( _trainingData, _labels, new Mat(), new Mat(), params );
	    } else{
	    	_svm.train(_trainingData, _labels, new Mat(), new Mat(), params );
	    }	    	    	 
	}
	
	/**
	 * creates a new bitmap from the Mat parameter 
	 * @param mat
	 * @return
	 */
	private Bitmap CreateBitmapFromCvMat(Mat mat){
		// convert processed mat back into a bitmap 		
		Bitmap.Config conf = Bitmap.Config.RGB_565; // see other conf types
		Bitmap res = Bitmap.createBitmap(mat.width(), mat.height(), conf); // this creates a MUTABLE bitmap
		
		Utils.matToBitmap(mat, res);
		
		return res; 
	}
	
	/**
	 * creates a OpenCV Mat from a given Bitmap 
	 * @param bitmap
	 * @return
	 */
	private Mat CreateCvMatFromBitmap(Bitmap bitmap){
		// convert bitmap to OpenCV Mat		
		Mat bitmapMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);	
		Utils.bitmapToMat(bitmap, bitmapMat, true);
		
		return bitmapMat;
	}
	
	/** loads a bitmap (via loadBitmapFromAssets) from the application assets and then converts it into a CvMat **/ 
	private String saveMatToDisk(Mat mat, String path, String filePrefix){
		StringBuilder fileName = new StringBuilder(filePrefix);
		fileName.append("_");
		
		Bitmap bitmap = null; 
		
		bitmap = CreateBitmapFromCvMat(mat);
		
		String filename = FileUtil.GetUniqueFilename(path, fileName.toString(), ".png");
		
		FileUtil.StoreBitmapData(_context, bitmap, path + filename, false, CompressFormat.PNG);
		
		bitmap.recycle();
		
		return fileName.toString(); 
	}
	
	/** loads a bitmap (via loadBitmapFromAssets) from the application assets and then converts it into a CvMat **/ 
	private Mat loadMatFromAssets(String filePath){
		Mat bitmapMat = null; 
		Bitmap bitmap = null; 
		
		bitmap = loadBitmapFromAssets(filePath);
		
		// convert bitmap to OpenCV Mat		
		bitmapMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);	
		Utils.bitmapToMat(bitmap, bitmapMat, false);
		
		bitmap.recycle();
		
		return bitmapMat;
	}
	
	/** loadas a bitmap from the application assets directory **/ 
	private Bitmap loadBitmapFromAssets( String filePath ){
		
		AssetManager assetManager = _context.getAssets();	
		
		InputStream in = null;
        Bitmap bitmap = null;
        
        try {
        	BitmapFactory.Options options = new BitmapFactory.Options();        	
            //options.inSampleSize = 2;
            in = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(in, null, options);
            if (bitmap == null){
                throw new RuntimeException("Couldn't load bitmap from asset '" + filePath + "'");
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load bitmap from asset '" + filePath + "'");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        
        return bitmap;		
	}	
	
}
