package com.example.demo_camera2api;


import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;
import android.util.Size;

import com.example.demo_camera2api.tflite.Classifier;
import com.example.demo_camera2api.tflite.ImageUtils;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

@SuppressLint("NewApi")
public class Detector implements Classifier {
    private final String TAG = "YEN_Detector";
    private AssetManager mAssetManager;
    private String mModelFile;
    private String mLabelFile;
    private Vector<String> mLabel = new Vector<String>();
    private boolean mIsQuantized;
    private int numBytesPerChannel;
    private final int mBatchSize = 1;
    private Size mInputSize;
    private Size mPreviewSize;
    private int[] mBmp2Pixel;
    private float[][][] outputLocations;
    private float[][] outputClasses;
    private float[][] outputScores;
    private float[] numDetections;

    private Interpreter mInterpreter;
    private ByteBuffer mImageData;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private Bitmap mCropBitmap;
    private Integer sensorOrientation;
    private int NUM_DETECTIONS;



    Detector(final AssetManager assetManager,
             final String modelFile,
             final String labelFile,
             final Size inputSize,
             final Size previewSize,
             final Integer orientation,
             final int NUM_DETECTIONS,
             final boolean isQuantized){
        this.mAssetManager = assetManager;
        this.mModelFile = modelFile;
        this.mLabelFile = labelFile;
        this.mInputSize = inputSize;
        this.mPreviewSize = previewSize;
        this.sensorOrientation = orientation;
        this.NUM_DETECTIONS = NUM_DETECTIONS;
        this.mIsQuantized = isQuantized;
    }

    public void create(){
        boolean bLabel = loadLabel();
        boolean bModel = loadModel();
        if(!bLabel || !bModel)
            return;

        setInputDataSize();
        initMatrix();
        mBmp2Pixel = new int[mInputSize.getWidth()*mInputSize.getHeight()];

        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];

        Log.d(TAG, "create detector success.");
    }

    private boolean loadModel(){
        AssetFileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = mAssetManager.openFd(mModelFile);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

            mInterpreter = new Interpreter(mappedByteBuffer);

            Log.d(TAG, "Load model file.");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Load model file error!");
            Log.d(TAG, e.toString());
            return false;
        }
    }

    private boolean loadLabel(){
        try {
            InputStream labelsInput = mAssetManager.open(mLabelFile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(labelsInput));
            String line;
            while ((line = bufferedReader.readLine()) != null){
                mLabel.add(line);
            }
            bufferedReader.close();

            Log.d(TAG, "Load label file!");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Load label file error!");
            Log.d(TAG, e.toString());
            return false;
        }
    }

    private void setInputDataSize(){
        if(mIsQuantized)
            numBytesPerChannel = 1;
        else
            numBytesPerChannel = 4;

        mImageData = ByteBuffer.allocateDirect(
                mBatchSize *
                mInputSize.getWidth() *
                mInputSize.getHeight()*
                3 * numBytesPerChannel);
        mImageData.order(ByteOrder.nativeOrder());
    }

    private void initMatrix(){
        frameToCropTransform = ImageUtils.getTransformationMatrix(
                mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                mInputSize.getWidth(), mInputSize.getHeight(),
                sensorOrientation,  false
        );

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
    }

    @Override
    public List<Recognition> recognizeImage(Bitmap bmp) {
        mCropBitmap = Bitmap.createBitmap(mInputSize.getWidth(), mInputSize.getHeight(), Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(mCropBitmap);
        canvas.drawBitmap(bmp, frameToCropTransform, null);
        mCropBitmap.getPixels(mBmp2Pixel, 0, mCropBitmap.getWidth(), 0, 0, mCropBitmap.getWidth(), mCropBitmap.getHeight());

        mImageData.rewind();
        for(int i=0; i<mInputSize.getHeight(); i++) {
            for (int j = 0; j < mInputSize.getWidth(); j++) {
                int pixelValue = mBmp2Pixel[i * mInputSize.getWidth() + j];

                if (mIsQuantized) {
                    mImageData.put((byte) ((pixelValue >> 16) & 0xFF));
                    mImageData.put((byte) ((pixelValue >> 8) & 0xFF));
                    mImageData.put((byte) (pixelValue & 0xFF));
                } else {
                    mImageData.putFloat((((pixelValue >> 16) & 0xFF) - Parameter.IMAGE_MEAN) / Parameter.IMAGE_STD);
                    mImageData.putFloat((((pixelValue >> 8) & 0xFF) - Parameter.IMAGE_MEAN) / Parameter.IMAGE_STD);
                    mImageData.putFloat(((pixelValue & 0xFF) - Parameter.IMAGE_MEAN) / Parameter.IMAGE_STD);
                }
            }
        }

        // init output result
        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];
        Object[] inputArray = {mImageData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);

        mInterpreter.runForMultipleInputsOutputs(inputArray, outputMap);


        final ArrayList<Recognition> recognitions = new ArrayList<>(NUM_DETECTIONS);
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
            final RectF location =
                new RectF(
                    outputLocations[0][i][1] * mInputSize.getWidth(),
                    outputLocations[0][i][0] * mInputSize.getHeight(),
                    outputLocations[0][i][3] * mInputSize.getWidth(),
                    outputLocations[0][i][2] * mInputSize.getHeight());
            // SSD Mobilenet V1 Model assumes class 0 is background class
            // in label file and class labels start from 1 to number_of_classes+1,
            // while outputClasses correspond to class index from 0 to number_of_classes
            int labelOffset = 1;
            recognitions.add(
                new Recognition(
                    "" + i,
                    mLabel.get((int) outputClasses[0][i] + labelOffset),
                    outputScores[0][i],
                    location));
        }
        return recognitions;
    }

    public Matrix getCropToFrameTransform() {
        return cropToFrameTransform;
    }

    @Override
    public void enableStatLogging(boolean debug) {

    }

    @Override
    public String getStatString() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void setNumThreads(int num_threads) {
        tfliteOptions.setNumThreads(num_threads);
        reCreateInterpreter();
    }

    Interpreter.Options tfliteOptions = new Interpreter.Options();
    @Override
    public void useNNAPI() {
        NnApiDelegate nnApiDelegate = new NnApiDelegate();
        tfliteOptions.addDelegate(nnApiDelegate);
        reCreateInterpreter();
    }

    @Override
    public void useGpu() {
        GpuDelegate gpuDelegate = new GpuDelegate();
        tfliteOptions.addDelegate(gpuDelegate);
        reCreateInterpreter();
    }

    @Override
    public void useCpu() {
        tfliteOptions = new Interpreter.Options();
        reCreateInterpreter();
    }

    private void reCreateInterpreter(){
        if(this.mInterpreter != null){
            this.mInterpreter.close();
            loadModel();
        }
    }

    public Integer getSensorOrientation() {
        return sensorOrientation;
    }

    public Size getmPreviewSize() {
        return mPreviewSize;
    }
}
