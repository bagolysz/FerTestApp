package com.example.szabi.fertestapp.model.messages;

import com.example.szabi.fertestapp.model.face.LabelsType;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Feedback {

    private LabelsType predicted;
    private LabelsType actual;

    public Feedback() {

    }

    public Feedback(LabelsType predicted, LabelsType actual) {
        this.predicted = predicted;
        this.actual = actual;
    }

    public LabelsType getPredicted() {
        return predicted;
    }

    public void setPredicted(LabelsType predicted) {
        this.predicted = predicted;
    }

    public LabelsType getActual() {
        return actual;
    }

    public void setActual(LabelsType actual) {
        this.actual = actual;
    }
}
