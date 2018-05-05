package com.example.szabi.fertestapp.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DisplayUtils {

    public static String convertToTimeString(long millis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        return dateFormat.format(new Date(millis));
    }
}
