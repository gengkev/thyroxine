package com.desklampstudios.thyroxine;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncStats;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;

import com.desklampstudios.thyroxine.eighth.EighthSyncAdapter;
import com.desklampstudios.thyroxine.news.NewsSyncAdapter;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

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
    public static enum DateFormats {
        FULL_DATETIME, // Monday, January 1, 1970 12:00 AM
        FULL_DATE, // Monday, January 1, 1970
        MED_DAYMONTH, // Jan 1
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
            } else if (this == MED_DAYMONTH) {
                return DateUtils.formatDateTime(context, millis,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH);
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
     * Makes sure synchronization is set up properly, retrieving the stub and Iodine accounts
     * and configuring periodic synchronization with the SyncAdapters.
     * @param context Context used to get accounts
     */
    public static void configureSync(@NonNull Context context) {
        // Find Iodine account (may not exist)
        Account iodineAccount = IodineAuthenticator.getIodineAccount(context);
        if (iodineAccount != null) {
            // Configure Eighth sync with Iodine account
            EighthSyncAdapter.configureSync(iodineAccount);

            // Configure News sync with Iodine account
            NewsSyncAdapter.configureSync(iodineAccount);
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
                    .setSyncAdapter(account, authority)
                    .setExtras(Bundle.EMPTY)
                    .build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, syncInterval);
        }
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

    @NonNull
    public static <T, K> ArrayList<ContentProviderOperation> createMergeBatch(
            @NonNull String LOG_TYPE,
            @NonNull List<T> itemList,
            @NonNull Cursor queryCursor,
            @NonNull Uri BASE_CONTENT_URI,
            @NonNull MergeInterface<T, K> mergeInterface,
            @NonNull SyncStats syncStats)
            throws SQLiteException {

        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        final HashMap<K, T> entryMap = new HashMap<>();
        for (T item : itemList) {
            entryMap.put(mergeInterface.getId(item), item);
        }

        // Go through current database entries
        while (queryCursor.moveToNext()) {
            syncStats.numEntries++;

            // Get item from DB
            final ContentValues oldItemValues = Utils.cursorRowToContentValues(queryCursor);
            final T oldItem = mergeInterface.fromContentValues(oldItemValues);

            final K id = mergeInterface.getId(oldItem);
            final Uri itemUri = mergeInterface.buildContentUri(id);

            // Compare to new data
            T newItem = entryMap.get(id);
            if (newItem != null) {
                // Item exists in the new data; remove to prevent insert later.
                entryMap.remove(id);

                // Check if an update is necessary
                if (!oldItem.equals(newItem)) {
                    syncStats.numUpdates++;
                    Log.v(TAG, LOG_TYPE + " id=" + id + ", scheduling update");
                    ContentValues newValues = mergeInterface.toContentValues(newItem);

                    batch.add(ContentProviderOperation.newUpdate(itemUri)
                            .withValues(newValues).build());
                } else {
                    Log.v(TAG, LOG_TYPE + " id=" + id + ", no update necessary.");
                }
            } else {
                // Item doesn't exist in the new data; remove it from the database.
                syncStats.numDeletes++;
                Log.v(TAG, LOG_TYPE + " id=" + id + ", scheduling delete");
                batch.add(ContentProviderOperation.newDelete(itemUri).build());
            }
        }
        queryCursor.close();

        // Add new items (everything left in the map not found in the database)
        for (K id : entryMap.keySet()) {
            syncStats.numInserts++;
            Log.v(TAG, LOG_TYPE + " id=" + id + ", scheduling block insert");

            T newItem = entryMap.get(id);
            ContentValues newValues = mergeInterface.toContentValues(newItem);

            batch.add(ContentProviderOperation.newInsert(BASE_CONTENT_URI)
                    .withValues(newValues).build());
        }

        Log.d(TAG, LOG_TYPE + " merge solution ready; returning batch");
        return batch;
    }

    public interface MergeInterface<T, U> {
        public ContentValues toContentValues(T item);
        public T fromContentValues(ContentValues values);
        public U getId(T item);
        public Uri buildContentUri(U id);
    }
}
