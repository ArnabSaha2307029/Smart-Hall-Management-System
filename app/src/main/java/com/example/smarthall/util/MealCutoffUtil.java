package com.example.smarthall.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MealCutoffUtil {

    public static Calendar getEarliestPermissibleDate() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (currentHour < 20) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, 2);
        }
        return calendar;
    }

    public static String getEarliestPermissibleDateString() {
        Calendar calendar = getEarliestPermissibleDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(calendar.getTime());
    }

    public static String formatDisplayDate(Calendar calendar) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(calendar.getTime());
    }

    public static String formatFirestoreDate(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
    }

    public static String formatFirestoreDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }
}
