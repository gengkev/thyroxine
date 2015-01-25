package com.desklampstudios.thyroxine.news;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.desklampstudios.thyroxine.external.SelectionBuilder;

import java.util.ArrayList;

import static com.desklampstudios.thyroxine.news.NewsContract.NewsEntries;
import static com.desklampstudios.thyroxine.news.NewsDatabase.Tables;

public class NewsProvider extends ContentProvider {
    private static final int NEWSENTRIES = 100;
    private static final int NEWSENTRIES_NEWS_ID = 102;

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private NewsDatabase mDbHelper;

    @NonNull
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = NewsContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, NewsContract.PATH_NEWSENTRIES, NEWSENTRIES);
        matcher.addURI(authority, NewsContract.PATH_NEWSENTRIES + "/#", NEWSENTRIES_NEWS_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new NewsDatabase(getContext());
        return true;
    }

    @NonNull
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NEWSENTRIES_NEWS_ID:
                return NewsEntries.CONTENT_ITEM_TYPE_NEWSENTRIES;
            case NEWSENTRIES:
                return NewsEntries.CONTENT_TYPE_NEWSENTRIES;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);

        Cursor cursor = builder
                .where(selection, selectionArgs)
                .query(db, projection, sortOrder);

        // make sure listeners are notified
        Context context = getContext();
        if (context != null) {
            cursor.setNotificationUri(context.getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public Uri insert(@NonNull Uri uri, @NonNull ContentValues values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case NEWSENTRIES: {
                db.insertOrThrow(Tables.TABLE_NEWSENTRIES, null, values);
                returnUri = NewsEntries.buildEntryUri(values.getAsInteger(NewsEntries.KEY_NEWS_ID));
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        // make sure listeners are notified
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);

        int rowsDeleted = builder
                .where(selection, selectionArgs)
                .delete(db);

        if (rowsDeleted != 0) {
            // make sure listeners are notified
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);

        int rowsUpdated = builder
                .where(selection, selectionArgs)
                .update(db, values);

        if (rowsUpdated != 0) {
            // make sure listeners are notified
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    /**
     * Apply the given set of {@link android.content.ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     *
     * This method was probably copied verbatim from the source code of the Google IO 2014 app.
     */
    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    // Used by query, update, delete
    @NonNull
    private SelectionBuilder buildSimpleSelection(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case NEWSENTRIES: {
                return builder.table(Tables.TABLE_NEWSENTRIES);
            }
            case NEWSENTRIES_NEWS_ID: {
                final int newsId = NewsEntries.getNewsId(uri);
                return builder.table(Tables.TABLE_NEWSENTRIES)
                        .where(NewsEntries.KEY_NEWS_ID + "=?", String.valueOf(newsId));
            }

            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }
}