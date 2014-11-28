package com.desklampstudios.thyroxine.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class ThyroxineProvider extends ContentProvider {
    private static final int NEWS = 100;
    private static final int NEWS_ID = 101;

    private static final int ACTV = 300;
    private static final int ACTV_WITH_AID = 301;

    private static final int BLOCK = 500;
    private static final int BLOCK_WITH_DATE = 501;
    private static final int BLOCK_WITH_DATE_AND_TYPE = 502;

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private ThyroxineDbHelper mOpenHelper;

    /*
    private static final SQLiteQueryBuilder sActvByBlockQueryBuilder;

    static {
        sActvByBlockQueryBuilder = new SQLiteQueryBuilder();
        sActvByBlockQueryBuilder.setTables(
                ThyroxineContract.EighthActv.TABLE_NAME + " INNER JOIN " +
                ThyroxineContract.EighthBlock.TABLE_NAME +
                " ON " + ThyroxineContract.EighthActv.TABLE_NAME +
                "." + ThyroxineContract.EighthActv.COLUMN_BLOCK_KEY +
                " = " + ThyroxineContract.EighthBlock.TABLE_NAME +
                "." + ThyroxineContract.EighthBlock._ID
        );
    }
    private static final String sBlockSelection =
            ThyroxineContract.EighthBlock.TABLE_NAME +
                    "." + ThyroxineContract.EighthBlock._ID + " = ?";
    private static final String sBlockWithAidSelection =
            ThyroxineContract.EighthBlock.TABLE_NAME +
                    "." + ThyroxineContract.EighthBlock._ID + " = ? AND " +
                    ThyroxineContract.EighthActv.COLUMN_AID + " = ?";

    private Cursor getActvByBlock(Uri uri, String[] projection, String sortOrder) {
        int aid = ThyroxineContract.EighthActv.getAidFromUri(uri);
        int bid = ThyroxineContract.EighthActv.getBlockFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (bid == -1) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selection = sLocationSettingWithStartDateSelection;
            selectionArgs = new String[]{locationSetting, startDate};
        }

        return sActvByBlockQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }
    */

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ThyroxineContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, ThyroxineContract.PATH_NEWS, NEWS);
        matcher.addURI(authority, ThyroxineContract.PATH_NEWS + "/#", NEWS_ID);

        matcher.addURI(authority, ThyroxineContract.PATH_ACTV, ACTV);
        matcher.addURI(authority, ThyroxineContract.PATH_ACTV + "/#", ACTV_WITH_AID);

        matcher.addURI(authority, ThyroxineContract.PATH_BLOCK, BLOCK);
        matcher.addURI(authority, ThyroxineContract.PATH_BLOCK + "/*", BLOCK_WITH_DATE);
        matcher.addURI(authority, ThyroxineContract.PATH_BLOCK + "/*/*", BLOCK_WITH_DATE_AND_TYPE);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new ThyroxineDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "news/#"
            case NEWS_ID:
                retCursor = db.query(
                        ThyroxineContract.NewsEntry.TABLE_NAME,
                        projection,
                        ThyroxineContract.NewsEntry._ID + " = '?'",
                        new String[]{ContentUris.parseId(uri) + ""},
                        null,
                        null,
                        sortOrder
                );
                break;
            // "news"
            case NEWS:
                retCursor = db.query(
                        ThyroxineContract.NewsEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            // "actv/#"
            case ACTV_WITH_AID:
                retCursor = db.query(
                        ThyroxineContract.EighthActv.TABLE_NAME,
                        projection,
                        ThyroxineContract.EighthActv._ID + " = '?'",
                        new String[]{ThyroxineContract.EighthActv.getAidFromUri(uri) + ""},
                        null,
                        null,
                        sortOrder
                );
                break;
            // "actv"
            case ACTV:
                retCursor = db.query(
                        ThyroxineContract.EighthActv.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            // "block/*/*"
            case BLOCK_WITH_DATE_AND_TYPE:
                retCursor = db.query(
                        ThyroxineContract.EighthBlock.TABLE_NAME,
                        projection,
                        ThyroxineContract.EighthBlock.COLUMN_DATE + " = ? AND " +
                                ThyroxineContract.EighthBlock.COLUMN_TYPE + " = ?",
                        new String[]{ThyroxineContract.EighthBlock.getDateFromUri(uri),
                                ThyroxineContract.EighthBlock.getTypeFromUri(uri)},
                        null,
                        null,
                        sortOrder
                );
                break;
            // "block/*"
            case BLOCK_WITH_DATE:
                retCursor = db.query(
                        ThyroxineContract.EighthBlock.TABLE_NAME,
                        projection,
                        ThyroxineContract.EighthBlock.COLUMN_DATE + " = ?",
                        new String[]{ThyroxineContract.EighthBlock.getDateFromUri(uri)},
                        null,
                        null,
                        sortOrder
                );
                break;
            // "block"
            case BLOCK:
                retCursor = db.query(
                        ThyroxineContract.EighthBlock.TABLE_NAME,
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
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NEWS_ID:
                return ThyroxineContract.NewsEntry.CONTENT_ITEM_TYPE;
            case NEWS:
                return ThyroxineContract.NewsEntry.CONTENT_TYPE;
            case ACTV_WITH_AID:
                return ThyroxineContract.EighthActv.CONTENT_TYPE;
            case ACTV:
                return ThyroxineContract.EighthActv.CONTENT_TYPE;
            case BLOCK_WITH_DATE_AND_TYPE:
                return ThyroxineContract.EighthBlock.CONTENT_ITEM_TYPE;
            case BLOCK_WITH_DATE:
                return ThyroxineContract.EighthBlock.CONTENT_TYPE;
            case BLOCK:
                return ThyroxineContract.EighthBlock.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri returnUri;
        long _id;

        switch (sUriMatcher.match(uri)) {
            case NEWS:
                _id = db.insert(ThyroxineContract.NewsEntry.TABLE_NAME, null, values);
                if (_id <= 0) {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                returnUri = ThyroxineContract.NewsEntry.buildNewsUri(_id);
                break;
            case ACTV:
                _id = db.insert(ThyroxineContract.EighthActv.TABLE_NAME, null, values);
                if (_id <= 0) {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                returnUri = ThyroxineContract.EighthActv.buildActvUri(_id);
                break;
            case BLOCK:
                _id = db.insert(ThyroxineContract.EighthBlock.TABLE_NAME, null, values);
                if (_id <= 0) {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                returnUri = ThyroxineContract.EighthBlock.buildBlockUri(_id);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String whereClause, String[] whereArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsDeleted;

        switch (sUriMatcher.match(uri)) {
            case NEWS:
                rowsDeleted = db.delete(ThyroxineContract.NewsEntry.TABLE_NAME,
                        whereClause, whereArgs);
                break;
            case ACTV:
                rowsDeleted = db.delete(ThyroxineContract.EighthActv.TABLE_NAME,
                        whereClause, whereArgs);
                break;
            case BLOCK:
                rowsDeleted = db.delete(ThyroxineContract.EighthBlock.TABLE_NAME,
                        whereClause, whereArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (whereClause == null || rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String whereClause, String[] whereArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsUpdated;

        switch (sUriMatcher.match(uri)) {
            case NEWS:
                rowsUpdated = db.update(ThyroxineContract.NewsEntry.TABLE_NAME,
                        values, whereClause, whereArgs);
                break;
            case ACTV:
                rowsUpdated = db.update(ThyroxineContract.EighthActv.TABLE_NAME,
                        values, whereClause, whereArgs);
                break;
            case BLOCK:
                rowsUpdated = db.update(ThyroxineContract.EighthBlock.TABLE_NAME,
                        values, whereClause, whereArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}
