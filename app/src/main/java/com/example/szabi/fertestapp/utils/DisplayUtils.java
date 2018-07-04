package com.example.szabi.fertestapp.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DisplayUtils {

    private static final long ONE_DAY = 0;
    private static final long FEW_DAYS = 345600000;

    public static String convertToTimeString(long millis) {
        long currentMillis = System.currentTimeMillis();

        Calendar today = Calendar.getInstance();
        today.set(today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DATE),
                0, 0, 0);

        SimpleDateFormat dateFormat;
        if (today.getTimeInMillis() - millis < ONE_DAY) {
            dateFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        } else if (currentMillis - millis < FEW_DAYS) {
            dateFormat = new SimpleDateFormat("EEE, HH:mm", Locale.ENGLISH);
        } else {
            dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
        }

        return dateFormat.format(new Date(millis));
    }
}
