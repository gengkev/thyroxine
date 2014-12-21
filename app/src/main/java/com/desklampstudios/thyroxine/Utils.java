package com.desklampstudios.thyroxine;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncRequest;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    public static final DateFormat FEED_DATETIME_FORMAT =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    public static final DateFormat ISO_DATETIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);

    public static final DateFormat BASIC_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    public static final DateFormat DISPLAY_DATE_FORMAT =
            DateFormat.getDateInstance(DateFormat.FULL); // default locale OK
    public static final DateFormat DISPLAY_DATE_FORMAT_MEDIUM =
            DateFormat.getDateInstance(DateFormat.MEDIUM); // default locale OK

    public static String formatBasicDate(String str, DateFormat dateFormat) {
        Date date = new Date(0);
        try {
            date = BASIC_DATE_FORMAT.parse(str);
        } catch (ParseException e) {
            Log.e(TAG, "Parsing date failed: " + str);
        }
        return dateFormat.format(date);
    }

    public static String cleanHtml(String in) {
        return Html.fromHtml(in).toString().trim();
    }
    public static String getSnippet(String in, int length) {
        in = cleanHtml(in);
        in = in.replaceAll("\\s+", " ");
        if (length >= 0 && in.length() > length) {
            in = in.substring(0, length);
        }
        return in;
    }

    /**
     * Reads the full contents of an InputStream into a String.
     * Source: <a href="https://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner.html">
     *     Stupid Scanner tricks</a>
     *
     * @param is The input stream to be read.
     * @return The contents of the stream, decoded with UTF-8.
     */
    public static String readInputStream(InputStream is) {
        return new Scanner(is, "UTF-8").useDelimiter("\\A").next();
    }

    /**
     * Helper method to schedule periodic execution of a sync adapter.
     * flexTime is only used on KitKat and newer devices.
     */
    public static void configurePeriodicSync(Account account, String authority,
                                             int syncInterval, int flexTime) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder()
                    .syncPeriodic(syncInterval, flexTime)
                    .setSyncAdapter(account, authority).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, syncInterval);
        }
    }
}
