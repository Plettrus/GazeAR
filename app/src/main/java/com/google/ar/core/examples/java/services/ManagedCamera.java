package com.google.ar.core.examples.java.services;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.ar.core.examples.java.helloar.MainActivity;
import com.google.ar.core.examples.java.utils.CompareSizesByArea;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ManagedCamera extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    public String systemId;
    public String threadName;
    public TextureView textureView;
    public boolean isPreviewing;
    private HandlerThread backgroundThread = null;
    private Handler backgroundHandler = null;
    private ImageReader imageReader = null;
    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    private CaptureRequest.Builder previewRequestBuilder; //CaptureRequest.Builder;
    private CaptureRequest previewRequest;
    private CameraDevice cameraDevice = null;
    private CameraCaptureSession captureSession = null;
    private int sensorOrientation = 0;
    private Size previewSize;
    private boolean flashSupported = false;
    int CAMERASTATE_IDLE = 1;
    int CAMERASTATE_PREVIEW = 0;
    int cameraState = CAMERASTATE_IDLE;
    private int MAX_PREVIEW_WIDTH = 1920;
    private int MAX_PREVIEW_HEIGHT = 1080;


    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice device) {
            cameraOpenCloseLock.release();
            cameraDevice = device;
            createCameraPreviewSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice device) {
            super.onClosed(device);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice device) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice device, int i) {
            onDisconnected(device);
        }
    };

    public ManagedCamera(String systemId, String threadName, TextureView textureView) {
        this.systemId = systemId;
        this.threadName = threadName;
        this.textureView = textureView;
        Log.i(TAG, "Creo la camera " + textureView);
    }

    private void setUpCameraOutputs(int width, int height){
        Activity activity = (Activity) textureView.getContext();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try{
            String[] cameraId = manager.getCameraIdList();
            for (int i = 0; i < cameraId.length; i++) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId[i]);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if(map == null) continue;

                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());

                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
                imageReader.setOnImageAvailableListener(null,backgroundHandler);

                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();

                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatePreviewWidth, rotatePreviewHeight, maxPreviewWidth, maxPreviewHeight;
                if(swappedDimensions) {
                    rotatePreviewWidth = height;
                    rotatePreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                } else {
                    rotatePreviewWidth = width;
                    rotatePreviewHeight = height;
                    maxPreviewWidth = displaySize.x;
                    maxPreviewHeight = displaySize.y;
                }

                if(maxPreviewWidth > MAX_PREVIEW_WIDTH)
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                if(maxPreviewHeight > MAX_PREVIEW_HEIGHT)
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;

                previewSize = new Size(1440,1080);

                flashSupported = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true;

                return;
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    private void openCamera(int width, int height) {
        Activity activity = (Activity) textureView.getContext();
        int permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        if(permission != PackageManager.PERMISSION_GRANTED){
            return;
        }

        setUpCameraOutputs(width, height);
        configureTransform(width, height);

        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MICROSECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening");
            }
            manager.openCamera(systemId, cameraStateCallback, backgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            captureSession.close();
            captureSession = null;
            cameraDevice.close();
            cameraDevice = null;
            imageReader.close();
            imageReader = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    private void createCameraPreviewSession () {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            Surface surface = new Surface(texture);

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null)
                                return;
                            captureSession = cameraCaptureSession;
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            setAutoFlash(previewRequestBuilder);
                            previewRequest = previewRequestBuilder.build();
                            updatePreviewStatus();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        }
                    }, null);
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight){
        Activity activity = (Activity) textureView.getContext();
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0f,0f,viewHeight,viewWidth);
        RectF bufferRect = new RectF(0f,0f,previewSize.getHeight(),previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if(Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            Float scale = (float) Math.max(viewHeight / previewSize.getHeight(), viewWidth / previewSize.getWidth());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            matrix.postScale(scale,scale,centerX, centerY);
            matrix.postRotate((float)(90 * (rotation - 2)), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder){
        if (flashSupported){
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    public void prepareToPreview(){
        backgroundThread = new HandlerThread(threadName);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        Log.i(TAG, "DA QUI INIZIA L'ERRORE: " + textureView);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        if(textureView.isAvailable()){
            openCamera(textureView.getWidth(), textureView.getHeight());
        }
    }

    public void updatePreviewStatus() {

        try {
            if (isPreviewing){
                captureSession.setRepeatingRequest(previewRequest, null, backgroundHandler);
                cameraState = CAMERASTATE_PREVIEW;
            } else {
                captureSession.stopRepeating();
                cameraState = CAMERASTATE_IDLE;
            }
        } catch (CameraAccessException e) {
        e.printStackTrace();
        }
    }

    public void releaseResources() {
        closeCamera();
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio){
        return choices[0];
    }

}
