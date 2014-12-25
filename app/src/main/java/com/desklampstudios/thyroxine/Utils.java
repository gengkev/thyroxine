package com.desklampstudios.thyroxine;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncRequest;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import com.desklampstudios.thyroxine.eighth.EighthSyncAdapter;
import com.desklampstudios.thyroxine.news.NewsSyncAdapter;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;
import com.desklampstudios.thyroxine.sync.StubAuthenticator;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    /**
     * Some date formats that are useful for parsing.
     * The locale MUST be explicitly set!
     * Also, warning that DateFormat objects aren't synchronized. This probably won't be a
     * problem in the near future.
     */
    public static class FixedDateFormats {
        public static final DateFormat NEWS_FEED =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        public static final DateFormat ISO =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        public static final DateFormat BASIC =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    }

    /**
     * Some date formats that are to be shown to the user.
     * The default locale is OK.
     */
    public static enum DateFormats {
        FULL_DATETIME, // Monday, January 1, 1970 12:00 AM
        FULL_DATE, // Monday, January 1, 1970
        MED_DAYMONTH, // Jan 1
        WEEKDAY; // Mon

        public DateFormat get() {
            Locale locale = Locale.getDefault();

            if (this == FULL_DATETIME) {
                return DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT, locale);
            } else if (this == FULL_DATE) {
                return DateFormat.getDateInstance(DateFormat.FULL);
            } else if (this == MED_DAYMONTH) {
                String str;
                if (Build.VERSION.SDK_INT >= 18) {
                    str = android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMM");
                } else if (locale.getLanguage().equals("fr")) { // TODO: remove hack
                    str = "d MMM";
                } else {
                    str = "MMM d";
                }
                return new SimpleDateFormat(str, locale);
            } else if (this == WEEKDAY) {
                return new SimpleDateFormat("EEE", locale);
            } else {
                throw new IllegalStateException();
            }
        }
        public String format(Date date) {
            return get().format(date);
        }
    }

    public static String formatBasicDate(String str, DateFormat dateFormat) {
        Date date = new Date(0);
        try {
            date = FixedDateFormats.BASIC.parse(str);
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

    public static <T> String join(Iterable<T> array, String sep) {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (T item : array) {
            if (first) {
                first = false;
            } else {
                out.append(sep);
            }
            out.append(String.valueOf(item));
        }
        return out.toString();
    }

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
    public static String readInputStream(InputStream is) {
        return new Scanner(is, "UTF-8").useDelimiter("\\A").next();
    }

    /**
     * Makes sure synchronization is set up properly, retrieving the stub and Iodine accounts
     * and configuring periodic synchronization with the SyncAdapters.
     * @param context Context used to get accounts
     */
    public static void configureSync(Context context) {
        // Make sure stub account exists
        Account stubAccount = StubAuthenticator.getStubAccount(context);
        // Configure News sync with stub account
        NewsSyncAdapter.configureSync(stubAccount);

        // Find Iodine account (may not exist)
        Account iodineAccount = IodineAuthenticator.getIodineAccount(context);
        if (iodineAccount != null) {
            // Configure Eighth sync with Iodine account
            EighthSyncAdapter.configureSync(iodineAccount);
        }
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

    public static ContentValues cursorRowToContentValues(Cursor cursor) {
        ContentValues values = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, values);
        return values;
    }
}
