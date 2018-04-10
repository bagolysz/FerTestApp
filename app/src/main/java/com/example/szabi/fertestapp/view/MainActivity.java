package com.example.szabi.fertestapp.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.szabi.fertestapp.R;
import com.example.szabi.fertestapp.model.Classification;
import com.example.szabi.fertestapp.model.Classifier;
import com.example.szabi.fertestapp.model.TensorFlowClassifier;
import com.example.szabi.fertestapp.utils.ClassificationUtils;
import com.example.szabi.fertestapp.utils.ImageUtils;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.example.szabi.fertestapp.Configs.INPUT_SIZE;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FerTestAppMainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private static final int MINIMUM_PREVIEW_SIZE = 224;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Button btnPredict;
    private TextureView textureView;
    private TextView predictionLabel;
    private ImageView capturedImage;

    private Classifier classifier;
    private Bitmap rgbFrameBitmap;
    private Bitmap rgbRotatedBitmap;
    private Bitmap croppedBitmap;

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

    private FaceDetector faceDetector;

    private long processingTime;

    //int[] imageArray = new int[]{R.drawable.test0, R.drawable.test1, R.drawable.test2, R.drawable.test3};
    //int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        predictionLabel = findViewById(R.id.lblPrediction);
        capturedImage = findViewById(R.id.capturedImage);
        textureView = findViewById(R.id.previewImage);
        textureView.setSurfaceTextureListener(textureListener);

        btnPredict = findViewById(R.id.btnTakePicture);
        btnPredict.setOnClickListener(v -> {
            capturePreview = true;
            btnPredict.setClickable(false);

            // use these lines, when loading images from drawables to test on well known images
            /*capturedImage.setImageResource(imageArray[index]);
            predictImage(imageArray[index]);
            index = (index + 1) % (imageArray.length);*/
        });

    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // start the camera from here
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
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }

        //configureTransform(width, height);
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
                Toast.makeText(MainActivity.this, "Sorry, no front facing camera available", Toast.LENGTH_LONG).show();
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
            // when camera device is ready, create preview
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
                    ImageFormat.YUV_420_888, 3);
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

    ImageReader.OnImageAvailableListener imageListener = reader -> {
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

        if (capturePreview) {
            Log.d(TAG, "working on image");
            // fill rgbFrameBitmap with latest acquired image in grayScale format
            getGrayScaleBitmapFromImage(image);
            //image.close();

            if (faceDetector.isOperational()) {
                Frame frame = new Frame.Builder().setBitmap(rgbRotatedBitmap).build();
                SparseArray<Face> faces = faceDetector.detect(frame);

                if (faces.size() > 0) {
                    //interested only in the first face
                    Face thisFace = faces.valueAt(0);
                    //float x1 = thisFace.getPosition().x > 0 ? thisFace.getPosition().x : 0;
                    //float y1 = thisFace.getPosition().y > 0 ? thisFace.getPosition().y : 0;
                    //float x2 = x1 + thisFace.getWidth() < rgbRotatedBitmap.getWidth() ? x1 + thisFace.getWidth() : rgbRotatedBitmap.getWidth();
                    //float y2 = y1 + thisFace.getHeight() < rgbRotatedBitmap.getHeight() ? y1 + thisFace.getHeight() : rgbRotatedBitmap.getHeight();

                    // starting X and face width, assuring that still inside the input image
                    float x1 = thisFace.getPosition().x > 0 ? thisFace.getPosition().x : 0;
                    float x2 = x1 + thisFace.getWidth() < rgbRotatedBitmap.getWidth() ? x1 + thisFace.getWidth() : rgbRotatedBitmap.getWidth();
                    // starting Y and face height, going up and down from center of the face
                    float yCenter = (float) (thisFace.getPosition().y + 1.25 * thisFace.getHeight() / 2);
                    float y1 = yCenter - thisFace.getWidth() / 2;
                    float y2 = yCenter + thisFace.getWidth() / 2;
                    // assure that bounds are inside the original image
                    y1 = y1 >= 0 ? y1 : 0;
                    y2 = y2 < rgbRotatedBitmap.getHeight() ? y2 : rgbRotatedBitmap.getHeight();

                    Bitmap tempBitmap = Bitmap.createBitmap(rgbRotatedBitmap,
                            (int) x1,
                            (int) y1,
                            (int) (x2 - x1),
                            (int) (y2 - y1)
                    );

                    frameToCropTransform = ImageUtils.getScaleMatrix(
                            tempBitmap.getWidth(),
                            tempBitmap.getHeight(),
                            INPUT_SIZE, INPUT_SIZE, true);

                    new Canvas(croppedBitmap).drawBitmap(tempBitmap, frameToCropTransform, null);

                    final long startTime = System.currentTimeMillis();
                    final List<Classification> results = classifier.classify(croppedBitmap);
                    double endTime = (System.currentTimeMillis() - startTime)/1000.0;

                    StringBuilder clazz = new StringBuilder();
                    for (Classification c : results) {
                        clazz.append(c.toString()).append("\n");
                    }
                    Log.d("RECOG", clazz.toString());


                    runOnUiThread(() -> {
                        capturedImage.setImageBitmap(croppedBitmap);
                        predictionLabel.setText(ClassificationUtils.argMax(results).toString() + " in " + endTime + " s");
                        capturePreview = false;
                        btnPredict.setClickable(true);
                    });
                } else {
                    runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "No face detected", Toast.LENGTH_SHORT).show();
                                capturePreview = false;
                                btnPredict.setClickable(true);
                            }
                    );
                }
            } else {
                Log.e(TAG, "Face detector is not operational");
            }
        }
        image.close();
        computing = false;
    };

    private void drawFaceRect() {
        /*Paint myRectPaint = new Paint();
                    myRectPaint.setStrokeWidth(5);
                    myRectPaint.setColor(Color.RED);
                    myRectPaint.setStyle(Paint.Style.STROKE);

                    Bitmap tempBitmap = Bitmap.createBitmap(rgbRotatedBitmap.getWidth(), rgbRotatedBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas tempCanvas = new Canvas(tempBitmap);
                    tempCanvas.drawBitmap(rgbRotatedBitmap, 0, 0, null);
                    tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);*/

        /*final long startTime = System.currentTimeMillis();

        preProcessImage(image);

        final List<Classification> results = classifier.classify(croppedBitmap);

        processingTime = System.currentTimeMillis() - startTime;
        StringBuilder clazz = new StringBuilder();
        for (Classification c : results) {
            clazz.append(c.toString());
        }
        Log.d("RECOG", clazz.toString() + "Finished in " + processingTime + "ms");

        runOnUiThread(() -> {
            predictionLabel.setText(ClassificationUtils.argMax(results).toString());
            if (capturePreview) {
                Paint myRectPaint = new Paint();
                myRectPaint.setStrokeWidth(5);
                myRectPaint.setColor(Color.RED);
                myRectPaint.setStyle(Paint.Style.STROKE);

                Bitmap tempBitmap = Bitmap.createBitmap(croppedBitmap.getWidth(), croppedBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas tempCanvas = new Canvas(tempBitmap);
                tempCanvas.drawBitmap(croppedBitmap, 0, 0, null);

                if (faceDetector.isOperational()) {
                    Frame frame = new Frame.Builder().setBitmap(croppedBitmap).build();
                    SparseArray<Face> faces = faceDetector.detect(frame);

                    for (int i = 0; i < faces.size(); i++) {
                        Face thisFace = faces.valueAt(i);
                        float x1 = thisFace.getPosition().x;
                        float y1 = thisFace.getPosition().y;
                        float x2 = x1 + thisFace.getWidth();
                        float y2 = y1 + thisFace.getHeight();
                        tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
                    }
                } else {
                    Log.e(TAG, "Could not set up face detector");
                }

                capturedImage.setImageBitmap(Bitmap.createBitmap(tempBitmap));
                capturePreview = false;
                btnPredict.setClickable(true);
            }
        });

        computing = false;*/
    }

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
            classifier = new TensorFlowClassifier(getAssets());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setProminentFaceOnly(true)
                .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        if (!faceDetector.isOperational()) {
            Log.e(TAG, "Could not set up face detector");
        }

        final int screenOrientation = getWindowManager().getDefaultDisplay().getRotation();
        sensorOrientation = sensorOrientation + screenOrientation;

        rgbFrameBitmap = Bitmap.createBitmap(previewDimension.getWidth(), previewDimension.getHeight(), Bitmap.Config.ARGB_8888);
        rgbRotatedBitmap = Bitmap.createBitmap(previewDimension.getWidth(), previewDimension.getHeight(), Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);

        rotationTransform = ImageUtils.getRotationMatrix(rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), sensorOrientation);
    }

    private static Size chooseOptimalSize(final Size[] choices) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        final List<Size> bigEnough = new ArrayList<>();
        for (final Size option : choices) {
            if (option.getHeight() >= MINIMUM_PREVIEW_SIZE && option.getWidth() >= MINIMUM_PREVIEW_SIZE) {
                bigEnough.add(option);
            }
        }
        // Pick the smallest of those, assuming we found any
        return (bigEnough.size() > 0) ? Collections.min(bigEnough,
                (l, r) -> Long.signum((long) l.getWidth() * l.getHeight() - (long) r.getWidth() * r.getHeight())
        ) : choices[0];
    }

    private void configureTransform(final int viewWidth, final int viewHeight) {
        if (textureView == null || previewDimension == null) {
            return;
        }
        int rotation = MainActivity.this.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewDimension.getWidth(), previewDimension.getHeight());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewDimension.getHeight(),
                    (float) viewWidth / previewDimension.getWidth()
            );
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }


    // OPEN-CLOSE UTILS
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
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // SYSTEM_CALLBACK SECTION
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(MainActivity.this, "Sorry, app can't be used without permission!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
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

        processingTime = System.currentTimeMillis() - startTime;
        StringBuilder clazz = new StringBuilder();
        for (Classification c : results) {
            clazz.append(c.toString());
        }
        Log.d("RECOG", clazz.toString() + "Finished in " + processingTime + "ms");
    }
}
