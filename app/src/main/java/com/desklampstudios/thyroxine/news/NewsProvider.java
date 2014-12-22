package com.desklampstudios.thyroxine.news;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import static com.desklampstudios.thyroxine.news.NewsContract.NewsEntries;
import static com.desklampstudios.thyroxine.news.NewsDatabase.Tables;

public class NewsProvider extends ContentProvider {
    private static final int NEWSENTRIES = 100;
    private static final int NEWSENTRIES_ID = 101;

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private NewsDatabase mDbHelper;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = NewsContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, NewsContract.PATH_NEWSENTRIES, NEWSENTRIES);
        matcher.addURI(authority, NewsContract.PATH_NEWSENTRIES + "/#", NEWSENTRIES_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new NewsDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case NEWSENTRIES_ID: {
                long entryId = NewsEntries.getEntryId(uri);
                retCursor = db.query(
                        Tables.TABLE_NEWSENTRIES,
                        projection,
                        NewsEntries._ID + "=?",
                        new String[]{ String.valueOf(entryId) },
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "news"
            case NEWSENTRIES: {
                retCursor = db.query(
                        Tables.TABLE_NEWSENTRIES,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        // make sure listeners are notified
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NEWSENTRIES_ID:
                return NewsEntries.CONTENT_ITEM_TYPE_NEWSENTRIES;
            case NEWSENTRIES:
                return NewsEntries.CONTENT_TYPE_NEWSENTRIES;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Uri returnUri;
        long id;

        switch (sUriMatcher.match(uri)) {
            case NEWSENTRIES: {
                id = db.insert(Tables.TABLE_NEWSENTRIES, null, values);
                if (id <= 0) {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                returnUri = NewsEntries.buildEntryUri(id);
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
    public int delete(Uri uri, String whereClause, String[] whereArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsDeleted;

        switch (sUriMatcher.match(uri)) {
            case NEWSENTRIES: {
                rowsDeleted = db.delete(Tables.TABLE_NEWSENTRIES,
                        whereClause, whereArgs);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        if (rowsDeleted != 0) {
            // make sure listeners are notified
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String whereClause, String[] whereArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsUpdated;

        switch (sUriMatcher.match(uri)) {
            case NEWSENTRIES: {
                rowsUpdated = db.update(NewsDatabase.Tables.TABLE_NEWSENTRIES,
                        values, whereClause, whereArgs);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        if (rowsUpdated != 0) {
            // make sure listeners are notified
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}