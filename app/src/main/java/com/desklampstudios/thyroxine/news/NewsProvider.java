package com.desklampstudios.thyroxine.news;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class NewsProvider extends ContentProvider {
    private static final int NEWS = 100;
    private static final int NEWS_ID = 101;

    // Warning: for now, also declared in strings.xml
    private static final String CONTENT_AUTHORITY = "com.desklampstudios.thyroxine.news";

    private static final String PATH_NEWS = "news";
    public static final Uri CONTENT_URI_NEWS = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + PATH_NEWS);

    public static final String CONTENT_TYPE_NEWS = ContentResolver.CURSOR_DIR_BASE_TYPE + "/newsEntries";
    public static final String CONTENT_ITEM_TYPE_NEWS = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/newsEntry";

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private NewsDbHelper mDbHelper;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = CONTENT_AUTHORITY;

        matcher.addURI(authority, PATH_NEWS, NEWS);
        matcher.addURI(authority, PATH_NEWS + "/#", NEWS_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new NewsDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "news/#"
            case NEWS_ID:
                retCursor = db.query(
                        NewsDbHelper.TABLE_NEWS,
                        projection,
                        NewsDbHelper.KEY_NEWS_ID + " = '?'",
                        new String[]{ContentUris.parseId(uri) + ""},
                        null,
                        null,
                        sortOrder
                );
                break;

            // "news"
            case NEWS:
                retCursor = db.query(
                        NewsDbHelper.TABLE_NEWS,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // make sure listeners are notified
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NEWS_ID:
                return CONTENT_ITEM_TYPE_NEWS;
            case NEWS:
                return CONTENT_TYPE_NEWS;
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
            case NEWS:
                id = db.insert(NewsDbHelper.TABLE_NEWS, null, values);
                if (id <= 0) {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                returnUri = ContentUris.withAppendedId(CONTENT_URI_NEWS, id);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
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
            case NEWS:
                rowsDeleted = db.delete(NewsDbHelper.TABLE_NEWS,
                        whereClause, whereArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
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
            case NEWS:
                rowsUpdated = db.update(NewsDbHelper.TABLE_NEWS,
                        values, whereClause, whereArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            // make sure listeners are notified
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}