package com.example.demo_camera2api;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2API {

    // input parameter
    private int mHeight, mWidth;
    private TextureView mTextureView;
    private Context mContext;

    // camera parameter
    private Size mPreviewSize;
    private int lensFacing = 1;
    private String mCameraID;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private CameraCaptureSession mPreviewSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private boolean bCapture = false;
    //video
    private MediaRecorder mMediaRecorder;
    private String mNextVideoAbsolutePath;
    private Size mVideoSize;
    //
    private static final int REQUEST_CAMERA_PERMISSION = 0;
    private static final int REQUEST_AUDIO_PERMISSIONS = 10;
    private static String TAG = "YEN_Camera2API";

    // detector
    private boolean bObjectDetector = false;
    private boolean bFaceDetector = false;
    private CaptureImage.callback objectDetectorCallback;

    Camera2API(Context context, TextureView textureView){
        this.mContext = context;
        this.mTextureView = textureView;
        this.mWidth = textureView.getWidth();
        this.mHeight = textureView.getHeight();
        this.mCameraManager = (CameraManager) this.mContext.getSystemService(Context.CAMERA_SERVICE);
    }

    public void init(){
        startBackgroundThread();
        this.cameraRegister(this.lensFacing);
        this.cameraOpen();
    }

    static List<String> ScanAllCamera(Context context){
        List<String> sResult = new ArrayList<>();
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            for(String sCameraID : cameraManager.getCameraIdList()){
                sResult.add(Parameter.getSOURCE().get(sCameraID));
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return sResult;
    }



    private void cameraRegister(int lens_facing){
        // 0: LENS_FACING_FRONT
        // 1: LENS_FACING_BACK
        // 2: LENS_FACING_EXTERNAL
        try {
            for(String sCameraID : mCameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(sCameraID);
                if(lens_facing == cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)){
                    StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    this.mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                    this.mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), this.mWidth, this.mHeight, this.mVideoSize);
//                    this.mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), this.mWidth, this.mHeight);
                    this.mCameraID = sCameraID;
                    setupImageReader();
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the capture Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable capture size");
            return choices[0];
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight()
                            - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    private void setupImageReader(){
        this.mImageReader = ImageReader.newInstance(this.mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
        this.mImageReader.setOnImageAvailableListener(imageAvailableListener, this.mBackgroundHandler);
    }

    ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            final Image image = imageReader.acquireNextImage();

            if(bCapture) {
                mBackgroundHandler.post(new CaptureImage(image, callback));
                bCapture = false;
            }
            else if(bObjectDetector){
                mBackgroundHandler.post(new CaptureImage(image, objectDetectorCallback));
            }
            else{
                image.close();
            }
        }
    };





    @SuppressLint("MissingPermission")
    private void cameraOpen() {
        try {
            if(!checkPERMISSIONS(VIDEO_PERMISSIONS)){
                ActivityCompat.requestPermissions((Activity) this.mContext, new String[]{
                        Manifest.permission.CAMERA
                }, REQUEST_CAMERA_PERMISSION);

                ActivityCompat.requestPermissions((Activity) this.mContext, new String[]{
                        Manifest.permission.RECORD_AUDIO
                }, REQUEST_AUDIO_PERMISSIONS);
            }

            mMediaRecorder = new MediaRecorder();
            if(!this.mCameraID.equals(null))
                mCameraManager.openCamera(this.mCameraID, mCameraDeviceStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    private boolean checkPERMISSIONS(String[] permissions){
        for(String permission : permissions){
            if (ActivityCompat.checkSelfPermission(this.mContext, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };

    @SuppressLint("NewApi")
    private void createCameraPreview(){
        SurfaceTexture mSurfaceTexture = this.mTextureView.getSurfaceTexture();
        assert mSurfaceTexture != null;
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        try {
            // captureRequest.Builder, CameraCaptureSession.stateCallbackPreview
            Surface mSurface = new Surface(mSurfaceTexture);
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mSurface);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), stateCallbackPreview, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    CameraCaptureSession.StateCallback stateCallbackPreview = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            try {
                if(mCameraDevice == null) return;

                mPreviewSession = session;
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                mPreviewSession.setRepeatingRequest(mCaptureRequestBuilder.build(), captureCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Toast.makeText(mContext, "Camera configuration change", Toast.LENGTH_SHORT).show();
        }
    };

    CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            Parameter.fpsCalculator.calculate();
        }
    };

    public void closeCamera(){

        stopBackgroundThread();
        closePreviewSession();

        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if(mImageReader != null){
            mImageReader.close();
            mImageReader = null;
        }

        if(this.mMediaRecorder != null){
            this.mMediaRecorder.release();
            this.mMediaRecorder = null;
        }
    }

    private void closePreviewSession(){
        if(mPreviewSession != null){
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private CaptureImage.callback callback;
    public void getCapture(CaptureImage.callback callback){
        try {
            this.callback = callback;

            this.mCaptureRequestBuilder.addTarget(this.mImageReader.getSurface());
            this.mPreviewSession.capture(this.mCaptureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void startRecordingVideo(){
        String sPath = "sdcard/" + this.mContext.getPackageName();
        Parameter.checkDir(sPath);
        mNextVideoAbsolutePath = sPath + "/" + System.currentTimeMillis() + ".mp4";

        try {
            closePreviewSession();
            setupMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera capture
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mCaptureRequestBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mCaptureRequestBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can capture the UI and start recording
            mCameraDevice.createCaptureSession(surfaces, stateCallbackPreview, null);

            mMediaRecorder.start();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void stopRecordingVideo(){
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        Toast.makeText(this.mContext, "Video saved: " + mNextVideoAbsolutePath, Toast.LENGTH_LONG).show();
        Log.d(TAG, "Video saved: " + mNextVideoAbsolutePath);

        mNextVideoAbsolutePath = null;
        createCameraPreview();
    }

    private void setupMediaRecorder(){
        if(mNextVideoAbsolutePath != null || !mNextVideoAbsolutePath.isEmpty()){
            try {
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
                mMediaRecorder.setVideoEncodingBitRate(10000000);
                mMediaRecorder.setVideoFrameRate(30);
                mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mMediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, e.toString());
            }
        }
    }

    public void setLensFacing(int lensFacing) {
        this.lensFacing = lensFacing;
    }

    public int getLensFacing() {
        return lensFacing;
    }

    public Size getmPreviewSize() {
        return mPreviewSize;
    }

    public void setbObjectDetector(boolean bObjectDetector) {
        this.bObjectDetector = bObjectDetector;
    }

    public void setbFaceDetector(boolean bFaceDetector) {
        this.bFaceDetector = bFaceDetector;
    }

    public void setbCapture(boolean bCapture) {
        this.bCapture = bCapture;
    }

    public void setObjectDetectorCallback(CaptureImage.callback objectDetectorCallback) {
        this.objectDetectorCallback = objectDetectorCallback;
    }
}
