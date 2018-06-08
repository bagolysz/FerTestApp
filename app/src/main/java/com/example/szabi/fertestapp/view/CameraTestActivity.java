package com.example.szabi.fertestapp.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.szabi.fertestapp.R;
import com.example.szabi.fertestapp.model.face.Classification;
import com.example.szabi.fertestapp.model.face.Classifier;
import com.example.szabi.fertestapp.service.TensorFlowClassifierService;
import com.example.szabi.fertestapp.utils.ClassificationProcessingThread;
import com.example.szabi.fertestapp.utils.ClassificationUtils;
import com.example.szabi.fertestapp.utils.FixedSizeQueue;
import com.example.szabi.fertestapp.utils.ImageUtils;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.example.szabi.fertestapp.Configs.INPUT_HEIGHT;
import static com.example.szabi.fertestapp.Configs.INPUT_SIZE;
import static com.example.szabi.fertestapp.Configs.INPUT_WIDTH;

public class CameraTestActivity extends AppCompatActivity implements NotificationListener {
    private static final String TAG = "FerTestAppMainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int MINIMUM_PREVIEW_SIZE = 640;
    private static final int QUEUE_SIZE = 6;
    private static final int SINGLE_PREDICTION = 1;
    private static final int CONTINUOUS_PREDICTION = 2;

    private int predictionType;

    private Button btnPredict;
    private Button btnSelectPredictionType;
    private TextureView textureView;
    private TextView predictionLabel;
    private ImageView capturedImage;

    private Classifier classifier;
    private Bitmap rgbFrameBitmap;
    private Bitmap rgbRotatedBitmap;
    private Bitmap croppedBitmap;
    private Bitmap featuresBitmap;

    private Matrix frameToCropTransform;
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
    private FixedSizeQueue fixedSizeQueue;
    private ClassificationProcessingThread classificationProcessingThread;

    private FaceDetector faceDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_test);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Camera Test");

        predictionType = SINGLE_PREDICTION;

        predictionLabel = findViewById(R.id.lbl_prediction);
        capturedImage = findViewById(R.id.captured_image);
        textureView = findViewById(R.id.preview_image);
        textureView.setSurfaceTextureListener(textureListener);

        btnPredict = findViewById(R.id.btn_take_picture);
        btnPredict.setOnClickListener(v -> {
            capturePreview = true;
            btnPredict.setClickable(false);

            // use these lines, when loading images from drawables to test on well known images
            /*capturedImage.setImageResource(imageArray[index]);
            predictImage(imageArray[index]);
            index = (index + 1) % (imageArray.length);*/
        });

        btnSelectPredictionType = findViewById(R.id.btn_start_predict);
        btnSelectPredictionType.setOnClickListener(v -> {
            switch (predictionType) {
                case SINGLE_PREDICTION:
                    // change to continuous
                    predictionType = CONTINUOUS_PREDICTION;

                    classificationProcessingThread = new ClassificationProcessingThread(fixedSizeQueue, this);
                    classificationProcessingThread.start();

                    btnPredict.setClickable(false);
                    showToast("Switched to continuous mode");
                    break;
                case CONTINUOUS_PREDICTION:
                    // stop threads and switch to single prediction mode
                    predictionType = SINGLE_PREDICTION;
                    classificationProcessingThread.stopMe();
                    btnPredict.setClickable(true);
                    showToast("Switched to single mode");
                    break;
            }
        });

    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private void openCamera(int width, int height) {
        // find the front facing camera if it exists
        // request permission if not granted already
        // open the camera
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            //ActivityCompat.requestPermissions(CameraTestActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        assert manager != null;
        try {
            for (String camId : manager.getCameraIdList()) {
                CameraCharacteristics characs = manager.getCameraCharacteristics(camId);
                if (characs.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = camId;
                }
            }

            if (cameraId == null) {
                showToast("Sorry, no front facing camera available");
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

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
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
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewDimension.getWidth(), previewDimension.getHeight());
            Surface surface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            imageReader = ImageReader.newInstance(
                    previewDimension.getWidth(),
                    previewDimension.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(imageListener, backgroundHandler);
            Surface irSurface = imageReader.getSurface();
            captureRequestBuilder.addTarget(irSurface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, irSurface), new CameraCaptureSession.StateCallback() {
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

    // put into featuresBitmap the cropped regions from the face
    private void cropFeatures(Face thisFace) {
        // find landmarks
        PointF lMouth = null, rMouth = null, lEye = null, rEye = null, nose = null, lCheek = null, rCheek = null, bottom = null;
        for (Landmark l : thisFace.getLandmarks()) {
            switch (l.getType()) {
                case 10:
                    lEye = l.getPosition();
                    break;
                case 4:
                    rEye = l.getPosition();
                    break;
                case 6:
                    nose = l.getPosition();
                    break;
                case 11:
                    lMouth = l.getPosition();
                    break;
                case 5:
                    rMouth = l.getPosition();
                    break;
                case 7:
                    lCheek = l.getPosition();
                    break;
                case 1:
                    rCheek = l.getPosition();
                    break;
                case 0:
                    bottom = l.getPosition();
                    break;
            }
        }

        // find eyes and mouth regions
        float mouthCenterY = 0, eyeCenterY = 0, halfHeight = 0, halfWidth = 0;
        if (nose != null && (lEye != null || rEye != null) && (lMouth != null || rMouth != null)) {
            if (lEye != null && rEye != null) {
                eyeCenterY = (lEye.y + rEye.y) / 2;
            } else if (lEye != null) {
                eyeCenterY = lEye.y;
            } else {
                eyeCenterY = rEye.y;
            }
            eyeCenterY *= 0.96;

            if (lMouth != null && rMouth != null) {
                mouthCenterY = (lMouth.y + rMouth.y) / 2;
            } else if (lMouth != null) {
                mouthCenterY = lMouth.y;
            } else {
                mouthCenterY = rMouth.y;
            }

            halfHeight = mouthCenterY - nose.y;
            if (bottom != null) {
                if (bottom.y - nose.y > 2 * halfHeight) {
                    halfHeight = bottom.y - nose.y;
                }
            }

            if (lCheek != null && rCheek != null) {
                halfWidth = rCheek.x - lCheek.x;
            } else if (lEye != null && rEye != null) {
                halfWidth = (float) ((rEye.x - lEye.x) * 1.5);
            } else {
                halfWidth = (float) (rgbRotatedBitmap.getWidth() * 0.4);
            }

            if (halfWidth <= 0 || halfHeight <= 0) {
                return;
            }

            Canvas featCanvas = new Canvas(featuresBitmap);
            featCanvas.drawBitmap(rgbRotatedBitmap,
                    new Rect((int) (nose.x - halfWidth), (int) (eyeCenterY - halfHeight),
                            (int) (nose.x + halfWidth), (int) (eyeCenterY + halfHeight)),
                    new Rect(0, 0, featuresBitmap.getWidth(), featuresBitmap.getHeight() / 2),
                    null);
            featCanvas.drawBitmap(rgbRotatedBitmap,
                    new Rect((int) (nose.x - halfWidth), (int) (mouthCenterY - halfHeight),
                            (int) (nose.x + halfWidth), (int) (mouthCenterY + halfHeight)),
                    new Rect(0, featuresBitmap.getHeight() / 2, featuresBitmap.getWidth(), featuresBitmap.getHeight()),
                    null);
        }

    }

    // this method is called every time a new image from the camera is available
    // the camera image is transformed into a grayScale bitmap, a face is cropped from the image, if any
    // and the cropped bitmap is fed to the inference engine to obtain a classification
    ImageReader.OnImageAvailableListener imageListener = reader -> {
        Image image;

        image = reader.acquireLatestImage();
        if (image == null) {
            //Log.d(TAG, "No image to read");
            return;
        }

        if (computing) {
            image.close();
            return;
        }
        computing = true;
        //Log.d(TAG, "working on image");

        switch (predictionType) {
            case SINGLE_PREDICTION:
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

                            StringBuilder clazz = new StringBuilder();
                            for (Classification c : results) {
                                clazz.append(c.toString()).append("\n");
                            }
                            Log.d("RECOG", clazz.toString());

                            // print results
                            runOnUiThread(() -> {
                                capturedImage.setImageBitmap(croppedBitmap);
                                predictionLabel.setText(ClassificationUtils.argMax(results).toString() + " in " + endTime + " s");
                                btnPredict.setClickable(true);
                            });

                        } else {
                            runOnUiThread(() -> {
                                        showToast("No face detected");
                                        btnPredict.setClickable(true);
                                    }
                            );
                        }
                    } else {
                        Log.e(TAG, "Face detector is not operational");
                    }
                }

                break;

            case CONTINUOUS_PREDICTION:
                // fill rgbRotatedBitmap with latest acquired image in grayScale format
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
                        fixedSizeQueue.addElement(ClassificationUtils.argMax(results));
                    } else {
                        Log.d(TAG, "No face detected");
                    }
                } else {
                    Log.e(TAG, "Face detector is not operational");
                }

                break;
        }

        image.close();
        computing = false;
    };


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

    // prepare the necessary resources for face detection and prediction
    private void prepareResources() {
        try {
            classifier = new TensorFlowClassifierService(getAssets());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setProminentFaceOnly(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        if (!faceDetector.isOperational()) {
            Log.e(TAG, "Could not set up face detector");
        }

        final int screenOrientation = getWindowManager().getDefaultDisplay().getRotation();
        // when using emulator with laptop webcam different method applies
        sensorOrientation = Build.MANUFACTURER.contains("Genymotion") ? 180 : sensorOrientation + screenOrientation;

        rgbFrameBitmap = Bitmap.createBitmap(previewDimension.getWidth(), previewDimension.getHeight(), Bitmap.Config.ARGB_8888);
        rgbRotatedBitmap = Bitmap.createBitmap(previewDimension.getWidth(), previewDimension.getHeight(), Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
        featuresBitmap = Bitmap.createBitmap(INPUT_WIDTH, INPUT_HEIGHT, Bitmap.Config.ARGB_8888);

        rotationTransform = ImageUtils.getRotationMatrix(
                rgbFrameBitmap.getWidth(),
                rgbFrameBitmap.getHeight(),
                sensorOrientation);

        fixedSizeQueue = new FixedSizeQueue(QUEUE_SIZE);
    }

    private static Size chooseOptimalSize(final Size[] choices) {
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

    // OPEN-CLOSE UTILS
    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(CameraTestActivity.this, msg, Toast.LENGTH_LONG).show());
    }

    private void closeCamera() {
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

    protected void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        if (classificationProcessingThread != null) {
            classificationProcessingThread.stopMe();
            classificationProcessingThread = null;
        }

        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void notifyPredictionReady(String msg) {
        runOnUiThread(() -> predictionLabel.setText(msg));
    }


    // SECTION UNUSED
    // methods used for testing purpose on hard-coded images loaded from drawables
    private Bitmap decodeBitmapImage(int image) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bmOptions.inScaled = false;

        return BitmapFactory.decodeResource(getResources(), image,
                bmOptions);
    }

    void predictImage(int image) {
        final long startTime = System.currentTimeMillis();

        final List<Classification> results = classifier.classify(decodeBitmapImage(image));

        long processingTime = System.currentTimeMillis() - startTime;
        StringBuilder clazz = new StringBuilder();
        for (Classification c : results) {
            clazz.append(c.toString());
        }
        Log.d("RECOG", clazz.toString() + "Finished in " + processingTime + "ms");
    }

}
