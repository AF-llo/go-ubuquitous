package com.example.android.sunshine.wear.util;

import android.annotation.SuppressLint;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by lars on 09.03.17.
 */

public class FormatUtil {
    @SuppressLint("DefaultLocale")
    public static String formattedTime(Calendar calendar, boolean isAmbient) {
        if (calendar == null) {
            return "";
        }
        // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
        if (isAmbient) {
            return String.format("%d:%02d", calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE));
        } else {
            return String.format("%d:%02d:%02d", calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
        }
    }
    
    @SuppressLint("DefaultLocale")
    public static String formattedDate(Calendar calendar, boolean isAmbient) {
        if (calendar == null) {
            return "";
        }
        // Draw e.g. FRI, JUL 14 2015 in interactive mode or JUL 14 2015 in ambient mode
        if (isAmbient) {
            return String.format("%s %d %d",
                    calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.YEAR));
        } else {
            return String.format("%s, %s %d %d",
                    calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()),
                    calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.YEAR));
        }
    }
}
