package com.example.szabi.fertestapp.utils;

import com.example.szabi.fertestapp.model.face.Classification;
import com.example.szabi.fertestapp.view.NotificationListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            maxValue /= queueSize;

            listener.notify("Max label: " + maxLabel + " with " + maxValue);
            //Log.d("PRED", "Max label: " + maxLabel + " with " + maxValue);
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
