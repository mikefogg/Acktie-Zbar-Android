package com.acktie.mobile.android.camera;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.proxy.TiViewProxy;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;

import com.acktie.mobile.android.InputArgs;
import com.acktie.mobile.android.zbar.ZBarManager;

public class CameraCallback implements PreviewCallback {
	private static final String LCAT = "Acktiemobile:CameraCallback";
	private CameraManager cameraManager = null;
	private ImageScanner scanner = null;
	private TiViewProxy viewProxy = null;
	private InputArgs args = null;
	private long lastScanDetected = System.currentTimeMillis();
	private boolean pictureTaken = false;

	public CameraCallback(int[] symbolsToScan, TiViewProxy viewProxy,
			CameraManager cameraManager, InputArgs args) {

		/* Instance scanner */
		scanner = ZBarManager.getImageScannerInstance(symbolsToScan);
		this.cameraManager = cameraManager;
		this.viewProxy = viewProxy;
		this.args = args;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		Log.d(LCAT, "Calling onPreviewFrame");
		// These could have been || (or'ed) together but I wanted to make it
		// easier
		// to understand why the image was not processed
		if (hasEnoughTimeElapsedToScanNextImage()) {
			return;
		} else if (args.isScanFromImageCapture() && !pictureTaken) {
			return;
		}

		scanImage(data, camera, pictureTaken);

		pictureTaken = false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void scanImage(byte[] data, Camera camera, boolean fromPictureTaken) {

		if (cameraManager.isStopped()) {
			return;
		}

		Camera.Parameters parameters = cameraManager.getCameraParameters();

		// If null, likely called after camera has been released.
		if (parameters == null) {
			return;
		}

		Size size = parameters.getPreviewSize();

		// Supported image formats
		// http://sourceforge.net/apps/mediawiki/zbar/index.php?title=Supported_image_formats
		Image resultImage = ZBarManager.getImageInstance(size.width,
				size.height, ZBarManager.Y800, data);

		int result = scanner.scanImage(resultImage);

		int quality = 0;
		Symbol symbol = null;

		if (result != 0) {
			SymbolSet syms = scanner.getResults();
			for (Symbol sym : syms) {
				System.out.println("Quality of Scan (Higher than 0 is good): "
						+ sym.getQuality());
				if (sym.getQuality() > quality) {
					symbol = sym;
				}
			}

			if (viewProxy != null && symbol != null) {

				Charset cs = Charset.forName("UTF-8");
				;
				String resultData = new String(symbol.getData().getBytes(), cs);

				if (args.isUseJISEncoding()) {
					cs = Charset.forName("Shift_JIS");
					resultData = new String(symbol.getData().getBytes(), cs);
				}

				System.out.println(resultData);

				if (!args.isContinuous()) {
					cameraManager.stop();
				}

				HashMap results = new HashMap();

				results.put("data", resultData);
				results.put("type", getTypeNameFromType(symbol.getType()));

				callSuccessInViewProxy(results);

				lastScanDetected = getOneSecondFromNow();
			}
		} else if (fromPictureTaken) {
			callErrorInViewProxy();
		}
	}

	private boolean hasEnoughTimeElapsedToScanNextImage() {
		return lastScanDetected > System.currentTimeMillis();
	}

	private long getOneSecondFromNow() {
		return System.currentTimeMillis() + 1000;
	}

	public void setPictureTaken(boolean pictureTaken) {
		this.pictureTaken = pictureTaken;
	}

	private String getTypeNameFromType(int type) {
		switch (type) {
		case Symbol.EAN8:
			return "EAN-8";
		case Symbol.EAN13:
			return "EAN-13";
		case Symbol.UPCA:
			return "UPC-A";
		case Symbol.UPCE:
			return "UPC-E";
		case Symbol.ISBN10:
			return "ISBN-10";
		case Symbol.ISBN13:
			return "ISBN-13";
		case Symbol.I25:
			return "I2/5";
		case Symbol.DATABAR:
			return "DataBar";
		case Symbol.DATABAR_EXP:
			return "DataBar-Exp";
		case Symbol.CODE39:
			return "CODE-39";
		case Symbol.CODE93:
			return "CODE-93";
		case Symbol.CODE128:
			return "CODE-128";
		case Symbol.CODABAR:
			return "Codabar";
		case Symbol.QRCODE:
			return "QRCODE";
		}

		return "";
	}

	private void callErrorInViewProxy() {
		try {
			Method successMethod = viewProxy.getClass().getMethod(
					"errorCallback");

			if (successMethod != null) {
				successMethod.invoke(viewProxy);
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void callSuccessInViewProxy(
			@SuppressWarnings("rawtypes") HashMap results) {
		try {
			Method successMethod = viewProxy.getClass().getMethod(
					"successCallback", HashMap.class);

			if (successMethod != null) {
				successMethod.invoke(viewProxy, results);
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}