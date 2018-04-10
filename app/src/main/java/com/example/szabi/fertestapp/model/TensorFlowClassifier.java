package com.example.szabi.fertestapp.model;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.example.szabi.fertestapp.Configs.INPUT_NAME;
import static com.example.szabi.fertestapp.Configs.INPUT_SIZE;
import static com.example.szabi.fertestapp.Configs.LABEL_PATH;
import static com.example.szabi.fertestapp.Configs.MODEL_PATH;
import static com.example.szabi.fertestapp.Configs.OUTPUT_NAME;

public class TensorFlowClassifier implements Classifier {
    private int noOfClasses;
    private List<String> labels;
    private TensorFlowInferenceInterface inferenceInterface;

    public TensorFlowClassifier(AssetManager assetManager) throws IOException {
        labels = readLabels(assetManager, LABEL_PATH);
        inferenceInterface = new TensorFlowInferenceInterface(assetManager, "file:///android_asset/" + MODEL_PATH);
        noOfClasses = labels.size();
    }

/*    @Override
    public TensorFlowClassifier create(AssetManager assetManager) throws IOException {
        TensorFlowClassifier classifier = new TensorFlowClassifier();
        classifier.labels = readLabels(assetManager, LABEL_PATH);
        classifier.inferenceInterface = new TensorFlowInferenceInterface(assetManager, "file:///android_asset/" + MODEL_PATH);
        classifier.noOfClasses = classifier.labels.size();
        return classifier;
    }*/

    @Override
    public List<Classification> classify(final Bitmap bitmap) {
        final float[] tfOutput = new float[noOfClasses];

        inferenceInterface.feed(INPUT_NAME, grayScaleBitmap(bitmap), 1, INPUT_SIZE, INPUT_SIZE, 1);
        inferenceInterface.run(new String[]{OUTPUT_NAME});
        inferenceInterface.fetch(OUTPUT_NAME, tfOutput);

        List<Classification> classifications = new ArrayList<>();
        for (int i = 0; i < tfOutput.length; i++) {
            classifications.add(new Classification(labels.get(i), tfOutput[i]));
        }
        return classifications;
    }

    // todo: use a look-up table here in order to avoid the float multiplication
    private float[] grayScaleBitmap(final Bitmap bitmap) {
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        float[] grayValues = new float[INPUT_SIZE * INPUT_SIZE];
        int r, g, b;

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            //r = ((val >> 16) & 0xFF);
            //g = ((val >> 8) & 0xFF);
            //b = (val & 0xFF);
            //float preGray = (float) (0.2989 * r + 0.587 * g + 0.114 * b);
            float preGray = (val & 0xFF);

            grayValues[i] = preGray;
        }

        return grayValues;
    }

    private static List<String> readLabels(AssetManager assetManager, String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(assetManager.open(fileName)));

        String line;
        List<String> labels = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            labels.add(line);
        }

        br.close();
        return labels;
    }
}
