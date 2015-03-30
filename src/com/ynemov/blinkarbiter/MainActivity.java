package com.ynemov.blinkarbiter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity implements CvCameraViewListener2 {

	private static final String TAG = "com_ynemov_blinkarbiter";
	private static final String mResFaceCascade = "haarcascade_frontalface_alt.xml";
	private static final String mResEyesCascade = "haarcascade_eye_tree_eyeglasses.xml";
	private static final String CASCADE_INIT_ERROR = "Cascede classifiers weren't initiated!";

	private static final float FACE_SIZE_PERCENTAGE = 0.3f;

	private static final int DETECTION_DURATION = 10000; // 10s
	private static final int SHOW_DURATION = 1000; // 10s
	private static final String BLINK_MSG = "Blink is detected"; // Message to place if blink is detected
	private String[] mRawRes = {mResFaceCascade, mResEyesCascade};

	private CameraBridgeViewBase mOpenCvCameraView;
	private CascadeClassifier mFaceCascade;
	private CascadeClassifier mEyesCascade;
	private Mat mGrayscaleImage;
	private int mAbsoluteFaceSize;
	private Toast mToast;

	private int mPreviousEyesState = -1;
	private boolean mIsEyeClosingDetected = false;
	private long mBlinkStartTime = 0;
	private int mBlinkCounter = 0;
	private List<Long> mBlinkingActivity = new ArrayList<Long>();

	static {
		if (!OpenCVLoader.initDebug()) {
			// Handle initialization error
		}
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				break;
			default:
				super.onManagerConnected(status);
				break;
			}
		}
	};

	class BlinkCounterTask extends TimerTask {

		@Override
		public void run() {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					mToast.cancel();
					mToast = Toast.makeText(getApplicationContext(), 
							"TOTAL NUMBER OF BLINKS: " + mBlinkCounter, DETECTION_DURATION);
					mToast.show();

					setupInitialCounterState();
				}
			});			
		}
	}

	BlinkCounterTask mBlinkCounterTask = new BlinkCounterTask();
	Timer mTimer = new Timer();
	
	private void setupInitialCounterState() {
		mPreviousEyesState = -1;
		mIsEyeClosingDetected = false;
		mBlinkStartTime = 0;
		mBlinkCounter = 0;
		mBlinkingActivity.clear();
		// TODO timer reopen 
//		mTimer = new Timer();
//		mTimer.schedule(mBlinkCounterTask, DETECTION_DURATION);		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);

		mToast = new Toast(this);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.disableView();

		(new AsyncTask<String, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(String... params) {

				try {
					// First URL
					{
						// Copy the resource into a temp file so OpenCV can load it
						InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
						File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
						File mCascadeFile = new File(cascadeDir, params[0]);
						FileOutputStream os = new FileOutputStream(mCascadeFile);


						byte[] buffer = new byte[4096];
						int bytesRead;
						while ((bytesRead = is.read(buffer)) != -1) {
							os.write(buffer, 0, bytesRead);
						}
						is.close();
						os.close();

						// Load the cascade classifier
						mFaceCascade = new CascadeClassifier(mCascadeFile.getAbsolutePath());
					}

					// Second URL
					{
						// Copy the resource into a temp file so OpenCV can load it
						InputStream is = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
						File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
						File mCascadeFile = new File(cascadeDir, params[1]);
						FileOutputStream os = new FileOutputStream(mCascadeFile);


						byte[] buffer = new byte[4096];
						int bytesRead;
						while ((bytesRead = is.read(buffer)) != -1) {
							os.write(buffer, 0, bytesRead);
						}
						is.close();
						os.close();

						// Load the cascade classifier
						mEyesCascade = new CascadeClassifier(mCascadeFile.getAbsolutePath());
					}					

				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;
			}

			@Override
			protected void onPostExecute(Boolean isSuccess) {
				if(isSuccess) {
					mOpenCvCameraView.enableView();
					mTimer.schedule(mBlinkCounterTask, DETECTION_DURATION);
				}
				else {
					mToast.cancel();
					mToast = Toast.makeText(getApplicationContext(), CASCADE_INIT_ERROR, Toast.LENGTH_SHORT);
					mToast.show();
				}
				super.onPostExecute(isSuccess);
			}
		}).execute(mRawRes);
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		mGrayscaleImage = new Mat(height, width, CvType.CV_8UC4);

		// The faces will be a 30% of the height of the screen
		mAbsoluteFaceSize = (int) (height * FACE_SIZE_PERCENTAGE);
	}

	@Override
	public void onCameraViewStopped() {}

	private Scalar mClr1 = new Scalar(0, 255, 0, 255);
	private Scalar mClr2 = new Scalar(255, 0, 0, 255);
	private Point mPt1 = new Point();
	private Point mPt2 = new Point();

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat inputMat = inputFrame.rgba();
		Imgproc.cvtColor(inputMat, mGrayscaleImage, Imgproc.COLOR_RGBA2RGB);

		MatOfRect mFaces = new MatOfRect();
		Size mMinSize = new Size(mAbsoluteFaceSize, mAbsoluteFaceSize);
		Size mMaxSize = new Size();

		if (mFaceCascade != null) {
			mFaceCascade.detectMultiScale(mGrayscaleImage, mFaces, 1.1, 2, 2, mMinSize, mMaxSize);
		}

		Rect[] facesArray = mFaces.toArray();

		/*
		 *  In case of simplicity first detected face is used
		 *  Replace comment from FOR-loop in case of advances detection
		 */
		//		for (int i = 0; < facesArray.length; ++i) 
		if(facesArray.length > 0) {
			int i = 0;
			/*
			 *  Face rectangle are used for debug purposes
			 *  Replace comments if has to debug
			 */
			Core.rectangle(inputMat, facesArray[i].tl(), facesArray[i].br(), mClr1, 3);

			Mat faceROI = mGrayscaleImage.submat(facesArray[i]);
			MatOfRect mEyes = new MatOfRect();

			//-- In each face, detect eyes
			mEyesCascade.detectMultiScale(faceROI, mEyes, 1.1, 2, 2, mMinSize, mMaxSize);
			Rect[] eyesArray = mEyes.toArray();

			// The condition handle only simple event "two eyes are open -> one (or two) is closed"
			if(eyesArray.length < 2 && mPreviousEyesState == 2) {
				mBlinkStartTime = System.currentTimeMillis();
				mIsEyeClosingDetected = true;
			}
			else if(eyesArray.length == 2 && mIsEyeClosingDetected) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mToast.cancel();
						mToast = Toast.makeText(getApplicationContext(), 
								"#" + (mBlinkCounter++) + " " + BLINK_MSG, SHOW_DURATION);
						mToast.show();
						mBlinkingActivity.add(mBlinkStartTime);
					}
				});
				mIsEyeClosingDetected = false;	
			}

			/*
			 *  Eyes rectangles are used for debug purposes
			 *  Replace comments if has to debug
			 */
			for(int j = 0; j < eyesArray.length; ++j) {
				mPt1.x = facesArray[i].x + eyesArray[j].x;
				mPt1.y = facesArray[i].y + eyesArray[j].y;

				mPt2.x = facesArray[i].x + eyesArray[j].x + eyesArray[j].width;
				mPt2.y = facesArray[i].y + eyesArray[j].y + eyesArray[j].height;

				Core.rectangle(inputMat, mPt1, mPt2, mClr2, 2);
			}

			mPreviousEyesState = eyesArray.length;
		}

		return inputMat;
	}
}