package com.example.szabi.fertestapp.model.face;

import android.graphics.Bitmap;

import com.example.szabi.fertestapp.model.face.Classification;

import java.util.List;

public interface Classifier {

    List<Classification> classify(final Bitmap image);
}
