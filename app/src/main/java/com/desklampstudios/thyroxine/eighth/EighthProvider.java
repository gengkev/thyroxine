package com.desklampstudios.thyroxine.eighth;

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

import static com.desklampstudios.thyroxine.eighth.EighthContract.ActvInstances;
import static com.desklampstudios.thyroxine.eighth.EighthContract.Actvs;
import static com.desklampstudios.thyroxine.eighth.EighthContract.Blocks;
import static com.desklampstudios.thyroxine.eighth.EighthContract.Schedule;
import static com.desklampstudios.thyroxine.eighth.EighthDatabase.Tables;

public class EighthProvider extends ContentProvider {
    private static final int ACTVS = 100;
    private static final int ACTVS_ID = 101;
    private static final int ACTVS_ID_ACTVINSTANCES = 102;

    private static final int BLOCKS = 200;
    //private static final int BLOCKS_BETWEEN = 201;
    private static final int BLOCKS_ID = 202;
    private static final int BLOCKS_ID_ACTVINSTANCES = 203;

    private static final int ACTVINSTANCES = 300;
    private static final int ACTVINSTANCES_BLOCK_ACTV_ID = 301;

    private static final int SCHEDULE = 400;
    private static final int SCHEDULE_BLOCK_ID = 401;

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private EighthDatabase mDbHelper;

    @NonNull
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = EighthContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "actvs", ACTVS);
        matcher.addURI(authority, "actvs/*", ACTVS_ID);
        matcher.addURI(authority, "actvs/*/actvInstances", ACTVS_ID_ACTVINSTANCES);

        matcher.addURI(authority, "blocks", BLOCKS);
        matcher.addURI(authority, "blocks/*", BLOCKS_ID);
        matcher.addURI(authority, "blocks/*/actvInstances", BLOCKS_ID_ACTVINSTANCES);

        matcher.addURI(authority, "actvInstances", ACTVINSTANCES);
        matcher.addURI(authority, "actvInstances/*/*", ACTVINSTANCES_BLOCK_ACTV_ID);

        matcher.addURI(authority, "schedule", SCHEDULE);
        matcher.addURI(authority, "schedule/*", SCHEDULE_BLOCK_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new EighthDatabase(getContext());
        return true;
    }

    @NonNull
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ACTVS:
                return Actvs.CONTENT_TYPE;
            case ACTVS_ID:
                return Actvs.CONTENT_ITEM_TYPE;
            case ACTVS_ID_ACTVINSTANCES:
                return ActvInstances.CONTENT_ITEM_TYPE;

            case BLOCKS:
                return Blocks.CONTENT_TYPE;
            case BLOCKS_ID:
                return Blocks.CONTENT_ITEM_TYPE;
            case BLOCKS_ID_ACTVINSTANCES:
                return ActvInstances.CONTENT_TYPE;

            case ACTVINSTANCES:
                return ActvInstances.CONTENT_TYPE;
            case ACTVINSTANCES_BLOCK_ACTV_ID:
                return ActvInstances.CONTENT_ITEM_TYPE;

            case SCHEDULE:
                return Schedule.CONTENT_TYPE;
            case SCHEDULE_BLOCK_ID:
                return Schedule.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        final SelectionBuilder builder = buildExpandedSelection(uri);

        Cursor cursor = builder
                .where(selection, selectionArgs)
                .query(db, projection, sortOrder);

        // Note: Notification URI must be manually set here for loaders to correctly
        // register ContentObservers.
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
            case ACTVS: {
                db.insertOrThrow(Tables.ACTVS, null, values);
                returnUri = Actvs.buildActvUri(values.getAsInteger(Actvs.KEY_ACTV_ID));
                break;
            }
            case BLOCKS: {
                db.insertOrThrow(Tables.BLOCKS, null, values);
                returnUri = Blocks.buildBlockUri(values.getAsInteger(Blocks.KEY_BLOCK_ID));
                break;
            }
            case ACTVINSTANCES: {
                db.insertOrThrow(Tables.ACTVINSTANCES, null, values);
                returnUri = ActvInstances.buildActvInstanceUri(
                        values.getAsInteger(ActvInstances.KEY_BLOCK_ID),
                        values.getAsInteger(ActvInstances.KEY_ACTV_ID));
                break;
            }
            case SCHEDULE: {
                db.insertOrThrow(Tables.SCHEDULE, null, values);
                returnUri = Schedule.buildScheduleUri(values.getAsInteger(Schedule.KEY_BLOCK_ID));
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown insert uri: " + uri);
            }
        }

        // TODO: notify changes for other urls with joins and stuff
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
     * Apply the given set of {@link ContentProviderOperation}, executing inside
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

    // Used by remove, update
    @NonNull
    private SelectionBuilder buildSimpleSelection(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case ACTVS: {
                return builder.table(Tables.ACTVS);
            }
            case ACTVS_ID: {
                final int actvId = Actvs.getActvId(uri);
                return builder.table(Tables.ACTVS)
                        .where(Actvs.KEY_ACTV_ID + "=?", String.valueOf(actvId));
            }

            case BLOCKS: {
                return builder.table(Tables.BLOCKS);
            }
            case BLOCKS_ID: {
                final int blockId = Blocks.getBlockId(uri);
                return builder.table(Tables.BLOCKS)
                        .where(Blocks.KEY_BLOCK_ID + "=?", String.valueOf(blockId));
            }

            case ACTVINSTANCES: {
                return builder.table(Tables.ACTVINSTANCES);
            }
            case ACTVINSTANCES_BLOCK_ACTV_ID: {
                final int blockId = ActvInstances.getBlockId(uri);
                final int actvId = ActvInstances.getActvId(uri);
                return builder.table(Tables.ACTVINSTANCES)
                        .where(ActvInstances.KEY_BLOCK_ID + "=?", String.valueOf(blockId))
                        .where(ActvInstances.KEY_ACTV_ID + "=?", String.valueOf(actvId));
            }

            case SCHEDULE: {
                return builder.table(Tables.SCHEDULE);
            }
            case SCHEDULE_BLOCK_ID: {
                final int blockId = Schedule.getBlockId(uri);
                return builder.table(Tables.SCHEDULE)
                        .where(ActvInstances.KEY_BLOCK_ID + "=?", String.valueOf(blockId));
            }

            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    // Used by query
    @NonNull
    private SelectionBuilder buildExpandedSelection(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case ACTVS: {
                return builder.table(Tables.ACTVS);
            }
            case ACTVS_ID: {
                final int actvId = Actvs.getActvId(uri);
                return builder.table(Tables.ACTVS)
                        .where(Actvs.KEY_ACTV_ID + "=?", String.valueOf(actvId));
            }
            case ACTVS_ID_ACTVINSTANCES: {
                final int actvId = Actvs.getActvId(uri);
                return builder.table(Tables.ACTVINSTANCES_JOIN_ACTVS_BLOCKS)
                        .mapToTable(ActvInstances._ID, Tables.ACTVINSTANCES)
                        .mapToTable(ActvInstances.KEY_BLOCK_ID, Tables.ACTVINSTANCES)
                        .mapToTable(ActvInstances.KEY_ACTV_ID, Tables.ACTVINSTANCES)
                        .where(Tables.ACTVINSTANCES + "." + ActvInstances.KEY_ACTV_ID + "=?", String.valueOf(actvId));
            }

            case BLOCKS: {
                return builder.table(Tables.BLOCKS_JOIN_SCHEDULE_ACTVS_ACTVINSTANCES)
                        .mapToTable(Blocks._ID, Tables.BLOCKS)
                        .mapToTable(Blocks.KEY_BLOCK_ID, Tables.BLOCKS)
                        .mapToTable(Schedule.KEY_ACTV_ID, Tables.SCHEDULE);
            }
            case BLOCKS_ID: {
                final int blockId = Blocks.getBlockId(uri);
                return builder.table(Tables.BLOCKS_JOIN_SCHEDULE_ACTVS_ACTVINSTANCES)
                        .mapToTable(Blocks._ID, Tables.BLOCKS)
                        .mapToTable(Blocks.KEY_BLOCK_ID, Tables.BLOCKS)
                        .mapToTable(Schedule.KEY_ACTV_ID, Tables.SCHEDULE)
                        .where(Tables.BLOCKS + "." + Blocks.KEY_BLOCK_ID + "=?", String.valueOf(blockId));
            }
            case BLOCKS_ID_ACTVINSTANCES: {
                final int blockId = Blocks.getBlockId(uri);
                return builder.table(Tables.ACTVINSTANCES_JOIN_ACTVS_BLOCKS)
                        .mapToTable(ActvInstances._ID, Tables.ACTVINSTANCES)
                        .mapToTable(ActvInstances.KEY_BLOCK_ID, Tables.ACTVINSTANCES)
                        .mapToTable(ActvInstances.KEY_ACTV_ID, Tables.ACTVINSTANCES)
                        .where(Tables.ACTVINSTANCES + "." + ActvInstances.KEY_BLOCK_ID + "=?", String.valueOf(blockId));
            }

            case ACTVINSTANCES: {
                return builder.table(Tables.ACTVINSTANCES_JOIN_ACTVS_BLOCKS)
                        .mapToTable(ActvInstances._ID, Tables.ACTVINSTANCES)
                        .mapToTable(ActvInstances.KEY_ACTV_ID, Tables.ACTVINSTANCES)
                        .mapToTable(ActvInstances.KEY_BLOCK_ID, Tables.ACTVINSTANCES);
            }
            case ACTVINSTANCES_BLOCK_ACTV_ID: {
                final int blockId = ActvInstances.getBlockId(uri);
                final int actvId = ActvInstances.getActvId(uri);
                return builder.table(Tables.ACTVINSTANCES_JOIN_ACTVS_BLOCKS)
                        .mapToTable(ActvInstances._ID, Tables.ACTVINSTANCES)
                        .mapToTable(ActvInstances.KEY_ACTV_ID, Tables.ACTVINSTANCES)
                        .mapToTable(ActvInstances.KEY_BLOCK_ID, Tables.ACTVINSTANCES)
                        .where(ActvInstances.KEY_BLOCK_ID + "=?", String.valueOf(blockId))
                        .where(ActvInstances.KEY_ACTV_ID + "=?", String.valueOf(actvId));
            }

            case SCHEDULE: {
                return builder.table(Tables.SCHEDULE);
            }
            case SCHEDULE_BLOCK_ID: {
                final int blockId = Schedule.getBlockId(uri);
                return builder.table(Tables.SCHEDULE)
                        .where(ActvInstances.KEY_BLOCK_ID + "=?", String.valueOf(blockId));
            }

            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }
}
