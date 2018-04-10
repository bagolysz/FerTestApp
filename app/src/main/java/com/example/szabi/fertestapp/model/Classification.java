package com.example.szabi.fertestapp.model;

public class Classification {

    private String label;
    private float confidence;

    public Classification() {

    }

    public Classification(String label, float confidence) {
        this.label = label;
        this.confidence = confidence;
    }

    public void update(String label, float confidence) {
        this.label = label;
        this.confidence = confidence;
    }

    public void update(Classification c) {
        this.label = c.getLabel();
        this.confidence = c.getConfidence();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return label + " - " + confidence;
    }
}
