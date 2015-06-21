package com.desklampstudios.thyroxine;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncRequest;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

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
                    .setSyncAdapter(account, authority)
                    .setExtras(Bundle.EMPTY)
                    .build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, syncInterval);
        }
    }

    /**
     * Some date formats that are useful for parsing.
     * The locale MUST be explicitly set!
     * Warning: these are mutable and not threadsafe. Do not mutate them :-|
     */
    public static class FixedDateFormats {
        public static final DateFormat NEWS_FEED =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        public static final DateFormat NEWS_LIST =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        public static final DateFormat ISO =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        public static final DateFormat BASIC =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        /**
         * Time zone of the Iodine servers, probably.
         */
        public static final TimeZone IODINE_TIME_ZONE = TimeZone.getTimeZone("America/New_York");

        // Set time zones for date formats without time zones
        static {
            NEWS_LIST.setTimeZone(IODINE_TIME_ZONE);
        }
    }

    /**
     * Some date formats that are to be shown to the user.
     * The default locale is preferred, for localization.
     * All timezones here are relative to the user so they should be OK.
     */
    public enum DateFormats {
        FULL_DATETIME, // Monday, January 1, 1970 12:00 AM
        FULL_DATE, // Monday, January 1, 1970
        FULL_DATE_NO_WEEKDAY, // January 1, 1970
        MED_DAYMONTH, // Jan 1
        FULL_WEEKDAY, // Monday
        WEEKDAY; // Mon

        public String format(Context context, long millis) {
            if (this == FULL_DATETIME) {
                return DateUtils.formatDateTime(context, millis,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY |
                                DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME);
            } else if (this == FULL_DATE) {
                return DateUtils.formatDateTime(context, millis,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY |
                                DateUtils.FORMAT_SHOW_YEAR);
            } else if (this == FULL_DATE_NO_WEEKDAY) {
                return DateUtils.formatDateTime(context, millis,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);
            } else if (this == MED_DAYMONTH) {
                return DateUtils.formatDateTime(context, millis,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH);
            } else if (this == FULL_WEEKDAY) {
                DateFormat format = new SimpleDateFormat("EEEE", Locale.getDefault());
                return format.format(new Date(millis));
            } else if (this == WEEKDAY) {
                DateFormat format = new SimpleDateFormat("EEE", Locale.getDefault());
                return format.format(new Date(millis));
            } else {
                throw new IllegalStateException();
            }
        }

        public String formatBasicDate(Context context, String str) {
            Date date = new Date(0);
            try {
                date = FixedDateFormats.BASIC.parse(str);
            } catch (ParseException e) {
                Log.e(TAG, "Parsing date failed: " + str);
            }
            return format(context, date.getTime());
        }
    }

    @NonNull
    public static String cleanHtml(String in) {
        return Html.fromHtml(in).toString().trim();
    }

    @NonNull
    public static String getSnippet(String in, int length) {
        in = cleanHtml(in);
        in = in.replaceAll("\\s+", " ");
        if (length >= 0 && in.length() > length) {
            in = in.substring(0, length);
        }
        return in;
    }

    @NonNull
    public static <E> String join(@NonNull Iterable<E> array, String sep) {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (E item : array) {
            if (first) {
                first = false;
            } else {
                out.append(sep);
            }
            out.append(String.valueOf(item));
        }
        return out.toString();
    }

    @NonNull
    public static String colorToHtmlHex(int color) {
        String str = Integer.toHexString(color);
        while (str.length() < 8) {
            str = "0" + str;
        }
        return "#" + str.substring(2, 8);
    }

    /**
     * Reads the full contents of an InputStream into a String.
     * Source: <a href="https://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner.html">
     *     Stupid Scanner tricks</a>
     *
     * @param is The input stream to be read.
     * @return The contents of the stream, decoded with UTF-8.
     */
    public static String readInputStream(@NonNull InputStream is) {
        return new Scanner(is, "UTF-8").useDelimiter("\\A").next();
    }

    /**
     * Read the entire contents of a cursor row and store them in a ContentValues.
     * This version performs number conversions immediately using the cursor, rather than storing
     *     the values as strings in the ContentValues and using that to convert them, thus
     *     indirectly fixing http://stackoverflow.com/q/13095277/689161
     *
     * @param cursor the cursor to read from.
     * @return the {@link ContentValues} to put the row into.
     */
    @NonNull
    public static ContentValues cursorRowToContentValues(@NonNull Cursor cursor) {
        ContentValues values = new ContentValues();
        String[] columns = cursor.getColumnNames();
        int length = columns.length;
        for (int i = 0; i < length; i++) {
            switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_NULL:
                    values.putNull(columns[i]);
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    values.put(columns[i], cursor.getLong(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    values.put(columns[i], cursor.getDouble(i));
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    values.put(columns[i], cursor.getString(i));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    values.put(columns[i], cursor.getBlob(i));
                    break;
            }
        }
        return values;
    }

}
