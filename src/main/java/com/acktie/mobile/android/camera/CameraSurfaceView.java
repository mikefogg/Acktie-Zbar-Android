package com.acktie.mobile.android.camera;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraSurfaceView extends SurfaceView implements
		SurfaceHolder.Callback {
	private CameraManager cameraManager = null;
	private Camera camera = null;
	private PreviewCallback cameraPreviewCallback = null;
	private static final String LCAT = "Acktiemobile:CameraSurfaceView";

	public CameraSurfaceView(Context context,
			PreviewCallback cameraPreviewCallback, CameraManager cameraManager) {
		super(context);
		this.cameraPreviewCallback = cameraPreviewCallback;
		this.cameraManager = cameraManager;
		this.camera = cameraManager.getCamera();
		getHolder().addCallback(this);

		// Needed for older version of Android prior to 3.0
		getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		//
		if (holder.getSurface() == null) {
			return;
		}

		Camera.Parameters parameters = cameraManager.getCameraParameters();
		// If null, likely called after camera has been released.
		if (parameters == null) {
			return;
		}

		camera.setDisplayOrientation(90);
		
		Camera.Size size = cameraManager.getBestPreviewSize(camera, width,
				height);
		
		int previewWidth = width;
		int previewHeight = height;
		
		if(size != null)
		{
			previewWidth = size.width;
			previewHeight = size.height;
		}
		// If size is null try and reverse height and width.
		// Thanks HTC desire
		else
		{
			size = cameraManager.getBestPreviewSize(camera, height,
					width);
			
			if(size != null)
			{
				previewWidth = size.width;
				previewHeight = size.height;
			}
			else
			{
				previewWidth = 640;
				previewHeight = 480;
			}
		}
		
		Log.d(LCAT, "Setting Preview Size to: " + previewWidth + "x" + previewHeight);
		parameters.setPreviewSize(previewWidth, previewHeight);
		camera.setParameters(parameters);
		camera.startPreview();
		cameraManager.enableAutoFocus();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {

			if(camera==null){
				camera = cameraManager.getCamera();
			}

			camera.setPreviewCallback(cameraPreviewCallback);
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			Log.d("DBG", "Error setting camera preview: " + e.getMessage());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		cameraManager.stop();
		camera = null;
	}
}