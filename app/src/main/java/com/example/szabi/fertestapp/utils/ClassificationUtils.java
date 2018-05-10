package com.example.szabi.fertestapp.utils;

import com.example.szabi.fertestapp.model.face.Classification;
import com.example.szabi.fertestapp.model.face.LabelsType;

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

    public static LabelsType toLabel(Classification classification) {
        switch (classification.getLabel()) {
            case "fear":
                return LabelsType.FEAR;
            case "angry":
                return LabelsType.ANGRY;
            case "disgust":
                return LabelsType.DISGUST;
            case "happy":
                return LabelsType.HAPPY;
            case "neutral":
                return LabelsType.NEUTRAL;
            case "sad":
                return LabelsType.SAD;
            case "surprised":
                return LabelsType.SURPRISED;

            default:
                return LabelsType.UNKNOWN;
        }
    }
}
