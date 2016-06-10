package me.mrkirby153.kcuhc.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class UtilTime {

    public static final String DATE_FORMAT_NOW = "MM-dd-yyy HH:mm:ss";
    public static final String DATE_FORMAT_DAY = "MM-dd-yy";

    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    public static String date() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_DAY);
        return sdf.format(cal.getTime());
    }

    public enum TimeUnit {
        FIT,
        DAYS,
        HOURS,
        MINUTES,
        SECONDS,
        MILLISECONDS
    }

    public static double convert(int trim, long time, TimeUnit type) {
        if (type == TimeUnit.FIT) {
            if (time < 60000) type = TimeUnit.SECONDS;
            else if (time < 3600000) type = TimeUnit.MINUTES;
            else if (time < 86400000) type = TimeUnit.HOURS;
            else type = TimeUnit.DAYS;
        }

        if (type == TimeUnit.DAYS) return trim(trim, time / 86400000d);
        if (type == TimeUnit.HOURS) return trim(trim, time / 3600000d);
        if (type == TimeUnit.MINUTES) return trim(trim, time / 60000d);
        if (type == TimeUnit.SECONDS) return trim(trim, time / 1000d);
        else return time;
    }

    public static String format(int trim, long time, TimeUnit type) {
        if (time == -1) return "Permanent";

        if (type == TimeUnit.FIT) {
            if (time < 60000) type = TimeUnit.SECONDS;
            else if (time < 3600000) type = TimeUnit.MINUTES;
            else if (time < 86400000) type = TimeUnit.HOURS;
            else type = TimeUnit.DAYS;
        }

        String text;
        if (type == TimeUnit.DAYS) text = trim(trim, time / 86400000d) + " Days";
        else if (type == TimeUnit.HOURS) text = trim(trim, time / 3600000d) + " Hours";
        else if (type == TimeUnit.MINUTES) text = trim(trim, time / 60000d) + " Minutes";
        else if (type == TimeUnit.SECONDS) text = trim(trim, time / 1000d) + " Seconds";
        else text = trim(0, time) + " Millisecond";

        return text;
    }

    public static double trim(int degree, double d) {
        String format = "#.#";
        for (int i = 1; i < degree; i++) {
            format += "#";
        }
        DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
        DecimalFormat twoDForm = new DecimalFormat(format, symb);
        return Double.valueOf(twoDForm.format(d));
    }
}
