package com.example.szabi.fertestapp.utils;

import com.example.szabi.fertestapp.model.Classification;

import java.util.Comparator;
import java.util.List;

public class ClassificationUtils {

    public static Classification argMax(List<Classification> classifications) {
        Classification max = new Classification();
        int size = classifications.size();
        if (size > 0) {
            max.update(classifications.get(0));
            for (int i = 1; i < size; i++) {
                if (classifications.get(i).getConfidence() > max.getConfidence()) {
                    max.update(classifications.get(i));
                }
            }
        }
        return max;
    }
}
