package com.example.szabi.fertestapp.model;

import android.graphics.Bitmap;

import java.util.List;

public interface Classifier {

    List<Classification> classify(final Bitmap image);
}
