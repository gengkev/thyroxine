package com.desklampstudios.thyroxine.sync;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.SyncStats;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.desklampstudios.thyroxine.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SyncUtils {
    private static final String TAG = SyncUtils.class.getSimpleName();

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
