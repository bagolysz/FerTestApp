package com.example.szabi.fertestapp.service;

import com.example.szabi.fertestapp.model.face.Classification;
import com.example.szabi.fertestapp.utils.FixedSizeQueue;
import com.example.szabi.fertestapp.view.NotificationListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.szabi.fertestapp.Configs.CONFIDENCE_THRESHOLD;
import static com.example.szabi.fertestapp.Configs.NO_FACE_MAX;

public class ClassificationProcessingThread extends Thread {

    private boolean runMe;
    private FixedSizeQueue queue;
    private int queueSize;
    private NotificationListener listener;

    public ClassificationProcessingThread(FixedSizeQueue queue, NotificationListener listener) {
        this.queue = queue;
        this.queueSize = queue.getQueueSize();
        this.listener = listener;
        runMe = true;
    }

    public void run() {
        while (runMe) {
            int noFaceCounter = 0;

            List<Classification> classifications = queue.getElements();
            HashMap<String, Float> hashMap = new HashMap<>();
            for (Classification c : classifications) {
                if (c.getLabel().equals("noFace")) {
                    noFaceCounter++;
                }

                if (!hashMap.containsKey(c.getLabel())) {
                    hashMap.put(c.getLabel(), c.getConfidence());
                } else {
                    hashMap.put(c.getLabel(), c.getConfidence() + hashMap.get(c.getLabel()));
                }
            }

            if (noFaceCounter >= NO_FACE_MAX) {
                listener.notifyPredictionReady("not enough faces");
            } else {
                String maxLabel = "";
                float maxValue = 0;
                for (Map.Entry<String, Float> pair : hashMap.entrySet()) {
                    if (pair.getValue() > maxValue) {
                        maxValue = pair.getValue();
                        maxLabel = pair.getKey();
                    }
                }

                maxValue /= queueSize;
                if (maxValue >= CONFIDENCE_THRESHOLD) {
                    listener.notifyPredictionReady("Max label: " + maxLabel + " with " + maxValue);
                } else {
                    listener.notifyPredictionReady("unknown");
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopMe() {
        runMe = false;
    }
}
