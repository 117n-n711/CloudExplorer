/*

		Licensed under the Apache License, Version 2.0 (the "License");
		you may not use this file except in compliance with the License.
		You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

		Unless required by applicable law or agreed to in writing, software
		distributed under the License is distributed on an "AS IS" BASIS,
		WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
		See the License for the specific language governing permissions and
   		limitations under the License.   			
 */

package com.cloudstudio.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import com.cloudstudio.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

/**
 * Camera Activity Class. Configures Android camera to take picture and show it.
 */
public class CameraActivity extends Activity {

	private static final String TAG = "CameraActivity";

	private Camera mCamera;
	private ForegroundCameraPreview mPreview;
	private boolean pressed = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.foregroundcameraplugin);

		// Create an instance of Camera
		mCamera = getCameraInstance();

		// Create a Preview and set it as the content of activity.
		mPreview = new ForegroundCameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);

		// Add a listener to the Capture button
		Button captureButton = (Button) findViewById(R.id.button_capture);
		captureButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				if (pressed)
					return;

				// Set pressed = true to prevent freezing.
				// Issue 1 at
				// http://code.google.com/p/foreground-camera-plugin/issues/detail?id=1
				pressed = true;

				// get an image from the camera
				mCamera.autoFocus(new AutoFocusCallback() {

					public void onAutoFocus(boolean success, Camera camera) {
						mCamera.takePicture(null, null, mPicture);
					}
				});
			}
		});

		Button cancelButton = (Button) findViewById(R.id.button_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pressed = false;
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}

	@Override
	protected void onPause() {
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
		super.onPause();
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		int cameraIndex = -1;
		try {
			cameraIndex = FindFrontCamera();
			if(cameraIndex == -1) cameraIndex = FindBackCamera();
			//c = Camera.open(cameraIndex); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		c=Camera.open();
		Log.d("camera", "return camera at index "+cameraIndex);
		return c; // returns null if camera is unavailable
	}

	@SuppressLint("NewApi")
	private static int FindFrontCamera() {
		int cameraCount = 0;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras(); // get cameras number

		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				// 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
				return camIdx;
			}
		}
		return -1;
	}

	@SuppressLint("NewApi")
	private static int FindBackCamera() {
		int cameraCount = 0;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras(); // get cameras number

		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
			Log.d("camera","camera facing : "+cameraInfo.facing);
			Log.d("camera","back facing : "+Camera.CameraInfo.CAMERA_FACING_BACK);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				Log.d("camera","return "+camIdx);
				// 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
				return camIdx;
			}
		}
		Log.d("camera","return -1");
		return -1;
	}

	private PictureCallback mPicture = new PictureCallback() {

		public void onPictureTaken(byte[] data, Camera camera) {

			Uri fileUri = (Uri) getIntent().getExtras().get(
					MediaStore.EXTRA_OUTPUT);

			File pictureFile = new File(fileUri.getPath());

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.d(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, "Error accessing file: " + e.getMessage());
			}
			setResult(RESULT_OK);
			pressed = false;
			finish();
		}
	};
}