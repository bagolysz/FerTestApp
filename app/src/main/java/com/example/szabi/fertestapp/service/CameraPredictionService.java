package com.example.szabi.fertestapp.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;

import com.example.szabi.fertestapp.model.face.Classification;
import com.example.szabi.fertestapp.model.face.Classifier;
import com.example.szabi.fertestapp.model.face.TensorFlowClassifier;
import com.example.szabi.fertestapp.utils.ClassificationUtils;
import com.example.szabi.fertestapp.utils.ImageUtils;
import com.example.szabi.fertestapp.view.HomeActivity;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.szabi.fertestapp.Configs.INPUT_SIZE;

public class CameraPredictionService {

    private static final String TAG = "CameraPredictionService";
    private static final int MINIMUM_PREVIEW_SIZE = 640;

    private Classifier classifier;
    private Bitmap rgbFrameBitmap;
    private Bitmap rgbRotatedBitmap;
    private Bitmap croppedBitmap;

    private Matrix rotationTransform;

    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size previewDimension;
    private ImageReader imageReader;
    private Integer sensorOrientation;

    private boolean capturePreview = false;
    private boolean computing = false;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private FaceDetector faceDetector;

    private HomeActivity activity;

    public CameraPredictionService(HomeActivity activity) {
        this.activity = activity;
    }

    public void predictOne() {
        capturePreview = true;
    }


    // SECTION MANAGE HARDWARE CAMERA
    public void openCamera() {
        // find the front facing camera if it exists
        // request permission if not granted already
        // open the camera
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d(TAG, "opening camera");
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        assert manager != null;
        try {
            for (String camId : manager.getCameraIdList()) {
                CameraCharacteristics characs = manager.getCameraCharacteristics(camId);
                if (characs.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = camId;
                }
            }

            if (cameraId == null) {
                activity.showToast("Sorry, no front facing camera available");
                return;
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            previewDimension = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class));

            manager.openCamera(cameraId, stateCallback, backgroundHandler);

            new Thread(this::prepareResources).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Exit openCamera");
    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void createCameraPreview() {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            imageReader = ImageReader.newInstance(
                    previewDimension.getWidth(),
                    previewDimension.getHeight(),
                    ImageFormat.YUV_420_888, 5);
            imageReader.setOnImageAvailableListener(imageListener, backgroundHandler);
            Surface irSurface = imageReader.getSurface();
            captureRequestBuilder.addTarget(irSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(irSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }
                    cameraCaptureSession = session;

                    try {
                        cameraCaptureSession.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "createCaptureSession onConfigureFailed");
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // this method is called every time a new image from the camera is available
    // the camera image is transformed into a grayScale bitmap, a face is cropped from the image, if any
    // and the cropped bitmap is fed to the inference engine to obtain a classification
    private ImageReader.OnImageAvailableListener imageListener = reader -> {
        //Log.d(TAG, "Image listener activated");
        Image image;

        image = reader.acquireLatestImage();
        if (image == null) {
            Log.d(TAG, "No image to read");
            return;
        }

        if (computing) {
            image.close();
            return;
        }
        computing = true;
        //Log.d(TAG, "working on image");

        if (capturePreview) {
            capturePreview = false;

            // fill rgbFrameBitmap with latest acquired image in grayScale format
            getGrayScaleBitmapFromImage(image);
            if (faceDetector.isOperational()) {
                Frame frame = new Frame.Builder().setBitmap(rgbRotatedBitmap).build();
                SparseArray<Face> faces = faceDetector.detect(frame);

                if (faces.size() > 0) {
                    //interested only in the first face
                    Face thisFace = faces.valueAt(0);
                    cropFace(thisFace);
                    //cropFeatures(thisFace);

                    final long startTime = System.currentTimeMillis();
                    final List<Classification> results = classifier.classify(croppedBitmap);
                    double endTime = (System.currentTimeMillis() - startTime) / 1000.0;
                    //fixedSizeQueue.addElement(ClassificationUtils.argMax(results));

                    StringBuilder clazz = new StringBuilder();
                    for (Classification c : results) {
                        clazz.append(c.toString()).append("\n");
                    }
                    Log.d("RECOG", clazz.toString());


                    capturePreview = false;
                    activity.showToast(ClassificationUtils.argMax(results).toString() + " in " + endTime + " s");

                } else {
                    activity.showToast("No face detected.");
                    capturePreview = false;
                }
            } else {
                Log.e(TAG, "Face detector is not operational");
            }
        }

        image.close();
        computing = false;
    };
    // END SECTION MANAGE HARDWARE CAMERA


    // SECTION PREPROCESS IMAGE
    // put into croppedBitmap parameter the cropped and resized to INPUT_SIZE detected face
    private void cropFace(Face thisFace) {
        // starting X and face width, assuring that still inside the input image
        float xCenter = thisFace.getPosition().x + thisFace.getWidth() / 2;
        float xHalf = (float) (thisFace.getWidth() / 2.3);
        float x1 = xCenter - xHalf;
        float x2 = xCenter + xHalf;
        //float x1 = thisFace.getPosition().x > 0 ? thisFace.getPosition().x : 0;
        //float x2 = x1 + thisFace.getWidth() < rgbRotatedBitmap.getWidth() ? x1 + thisFace.getWidth() : rgbRotatedBitmap.getWidth();
        // starting Y and face height, going up and down from center of the face
        float yCenter = (float) (thisFace.getPosition().y + 1.2 * thisFace.getHeight() / 2);
        float y1 = yCenter - thisFace.getWidth() / 2;
        float y2 = yCenter + thisFace.getWidth() / 2;

        // assure that bounds are inside the original image
        y1 = y1 >= 0 ? y1 : 0;
        y2 = y2 < rgbRotatedBitmap.getHeight() ? y2 : rgbRotatedBitmap.getHeight();

        new Canvas(croppedBitmap).drawBitmap(
                rgbRotatedBitmap,
                new Rect((int) x1, (int) y1, (int) x2, (int) y2),
                new Rect(0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight()),
                null
        );
    }

    // put into rgbRotatedBitmap the grayscale captured image
    private void getGrayScaleBitmapFromImage(Image image) {
        Image.Plane Y = image.getPlanes()[0];
        // no need for U and V when creating grayScale

        int width = image.getWidth();
        int height = image.getHeight();

        // get buffer size
        int ySize = Y.getBuffer().remaining();
        byte[] yData = new byte[ySize];
        int[] rgbData = new int[ySize];
        Y.getBuffer().get(yData, 0, ySize);

        for (int i = 0; i < ySize; i++) {
            int y = yData[i] & 0xFF; //convert byte to int
            rgbData[i] = 0xFF000000 | (y << 16) | (y << 8) | y; // set the same value for every channel to create grayScale image
        }

        rgbFrameBitmap.setPixels(rgbData, 0, width, 0, 0, width, height);
        new Canvas(rgbRotatedBitmap).drawBitmap(rgbFrameBitmap, rotationTransform, null);
    }
    // END SECTION PREPROCESS IMAGE

    // SECTION UTILS
    // prepare the necessary resources for face detection and prediction
    private void prepareResources() {
        try {
            classifier = new TensorFlowClassifier(activity.getAssets());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        faceDetector = new FaceDetector.Builder(activity.getApplicationContext())
                .setTrackingEnabled(false)
                .setProminentFaceOnly(true)
                .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        if (!faceDetector.isOperational()) {
            Log.e(TAG, "Could not set up face detector");
        }

        final int screenOrientation = activity.getWindowManager().getDefaultDisplay().getRotation();
        // when using emulator with laptop webcam different method applies
        sensorOrientation = Build.MANUFACTURER.contains("Genymotion") ? 180 : sensorOrientation + screenOrientation;

        rgbFrameBitmap = Bitmap.createBitmap(previewDimension.getWidth(), previewDimension.getHeight(), Bitmap.Config.ARGB_8888);
        rgbRotatedBitmap = Bitmap.createBitmap(previewDimension.getWidth(), previewDimension.getHeight(), Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);

        rotationTransform = ImageUtils.getRotationMatrix(
                rgbFrameBitmap.getWidth(),
                rgbFrameBitmap.getHeight(),
                sensorOrientation);
    }

    private Size chooseOptimalSize(final Size[] choices) {
        // collect the supported resolutions that are at least as big as the preview Surface
        final List<Size> bigEnough = new ArrayList<>();
        for (final Size option : choices) {
            if (option.getHeight() >= MINIMUM_PREVIEW_SIZE && option.getWidth() >= MINIMUM_PREVIEW_SIZE) {
                bigEnough.add(option);
            }
        }
        // pick the smallest of those, assuming we found any
        return (bigEnough.size() > 0) ? Collections.min(bigEnough,
                (l, r) -> Long.signum((long) l.getWidth() * l.getHeight() - (long) r.getWidth() * r.getHeight())
        ) : choices[0];
    }

    public void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        if (faceDetector != null) {
            faceDetector.release();
            faceDetector = null;
        }
    }

    public void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    // END SECTION UTILS
}
