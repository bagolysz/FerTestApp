package com.example.szabi.fertestapp.utils;

import android.graphics.Matrix;

public class ImageUtils {

    public static Matrix getRotationMatrix(final int width, final int height, final int applyRotation) {
        final Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            // translate the center of the image to the origin, rotate then translate back
            matrix.postTranslate(-width / 2.0f, -height / 2.0f);
            matrix.postRotate(applyRotation);
            matrix.postTranslate(width / 2.0f, height / 2.0f);
        }
        return matrix;
    }

    public static Matrix getScaleMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final boolean maintainAspectRation) {
        final Matrix matrix = new Matrix();

        if (srcWidth != dstWidth && srcHeight != dstHeight) {
            final float scaleX = dstWidth / (float) srcWidth;
            final float scaleY = dstHeight / (float) srcHeight;

            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);
            if (maintainAspectRation) {
                float scaleFactor = Math.max(scaleX, scaleY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                matrix.postScale(scaleX, scaleY);
            }
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }
        return matrix;
    }

    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }

}
