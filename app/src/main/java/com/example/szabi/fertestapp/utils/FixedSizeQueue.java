package com.example.szabi.fertestapp.utils;

import com.example.szabi.fertestapp.model.face.Classification;

import java.util.LinkedList;
import java.util.List;

public class FixedSizeQueue {

    private int queueSize;
    private LinkedList<Classification> list;

    public FixedSizeQueue(int queueSize) {
        this.queueSize = queueSize;
        list = new LinkedList<>();
    }

    public synchronized void addElement(Classification c) {
        if (list.size() >= queueSize) {
            list.removeLast();
        }
        list.addFirst(c);
    }

    public synchronized List<Classification> getElements() {
        return list;
    }

}
