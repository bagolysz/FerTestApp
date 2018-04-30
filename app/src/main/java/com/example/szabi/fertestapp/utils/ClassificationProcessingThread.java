package com.example.szabi.fertestapp.utils;

import android.app.Activity;
import android.util.Log;

import com.example.szabi.fertestapp.model.Classification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassificationProcessingThread extends Thread {

    private boolean runMe;
    private FixedSizeQueue queue;
    private Activity activity;

    public ClassificationProcessingThread(Activity activity, FixedSizeQueue queue) {
        this.activity = activity;
        this.queue = queue;
        runMe = true;
    }

    public void run() {
        while (runMe) {
            List<Classification> classifications = queue.getElements();
            HashMap<String, Float> hashMap = new HashMap<>();
            for (Classification c : classifications) {
                if (!hashMap.containsKey(c.getLabel())) {
                    hashMap.put(c.getLabel(), c.getConfidence());
                } else {
                    hashMap.put(c.getLabel(), c.getConfidence() + hashMap.get(c.getLabel()));
                }
            }

            String maxLabel = "";
            float maxValue = 0;
            for (Map.Entry<String, Float> pair : hashMap.entrySet()) {
                if (pair.getValue() > maxValue) {
                    maxValue = pair.getValue();
                    maxLabel = pair.getKey();
                }
            }

            Log.d("PRED", "Max label: " + maxLabel + " with " + maxValue);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopMe() {
        runMe = false;
    }
}
