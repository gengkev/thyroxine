package com.desklampstudios.thyroxine.eighth;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

class EighthContract {
    // Warning: for now, also declared in strings.xml
    public static final String CONTENT_AUTHORITY = "com.desklampstudios.thyroxine.eighth";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_BLOCKS = "blocks";
    private static final String PATH_ACTVS = "actvs";
    private static final String PATH_ACTVINSTANCES = "actvInstances";
    private static final String PATH_SCHEDULE = "schedule";

    public static class Blocks implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_BLOCKS).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.thyroxine.block";
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.thyroxine.block";

        public static final String KEY_BLOCK_ID = "block_id";
        public static final String KEY_DATE = "block_date";
        public static final String KEY_TYPE = "block_type";
        public static final String KEY_LOCKED = "block_locked";

        /** Default "ORDER BY" clause */
        public static final String DEFAULT_SORT = KEY_DATE + " ASC, " + KEY_TYPE + " ASC";

        public static Uri buildBlockUri(int blockId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(blockId))
                    .build();
        }
        public static Uri buildBlockWithActvInstancesUri(int blockId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(blockId))
                    .appendPath(PATH_ACTVINSTANCES)
                    .build();
        }
        public static int getBlockId(@NonNull Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(1));
        }

        @NonNull
        public static EighthBlock fromContentValues(@NonNull ContentValues values) {
            return new EighthBlock(
                    values.getAsInteger(KEY_BLOCK_ID),
                    values.getAsString(KEY_DATE),
                    values.getAsString(KEY_TYPE),
                    values.getAsBoolean(KEY_LOCKED)
            );
        }
        @NonNull
        public static ContentValues toContentValues(@NonNull EighthBlock block) {
            ContentValues values = new ContentValues();
            values.put(KEY_BLOCK_ID, block.blockId);
            values.put(KEY_DATE, block.date);
            values.put(KEY_TYPE, block.type);
            values.put(KEY_LOCKED, block.locked);
            return values;
        }
    }

    public static class Actvs implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_ACTVS).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.thyroxine.actv";
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.thyroxine.actv";

        public static final String KEY_ACTV_ID = "actv_id";
        public static final String KEY_NAME = "actv_name";
        public static final String KEY_DESCRIPTION = "actv_description";
        public static final String KEY_FLAGS = "actv_flags";

        /** Default "ORDER BY" clause */
        public static final String DEFAULT_SORT = KEY_NAME + " ASC";

        public static Uri buildActvUri(int actvId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(actvId))
                    .build();
        }
        public static Uri buildActvWithActvInstancesUri(int actvId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(actvId))
                    .appendPath(PATH_ACTVINSTANCES)
                    .build();
        }
        public static int getActvId(@NonNull Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(1));
        }

        @NonNull
        public static EighthActv fromContentValues(@NonNull ContentValues values) {
            return new EighthActv(
                    values.getAsInteger(KEY_ACTV_ID),
                    values.getAsString(KEY_NAME),
                    values.getAsString(KEY_DESCRIPTION),
                    values.getAsLong(KEY_FLAGS)
            );
        }
        @NonNull
        public static ContentValues toContentValues(@NonNull EighthActv actv) {
            ContentValues values = new ContentValues();
            values.put(KEY_ACTV_ID, actv.actvId);
            values.put(KEY_NAME, actv.name);
            values.put(KEY_DESCRIPTION, actv.description);
            values.put(KEY_FLAGS, actv.flags);
            return values;
        }
    }

    public static class ActvInstances implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_ACTVINSTANCES).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.thyroxine.actvInstance";
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.thyroxine.actvInstance";

        public static final String KEY_BLOCK_ID = Blocks.KEY_BLOCK_ID;
        public static final String KEY_ACTV_ID = Actvs.KEY_ACTV_ID;
        public static final String KEY_COMMENT = "actvInstance_comment";
        public static final String KEY_FLAGS = "actvInstance_flags";
        public static final String KEY_ROOMS_STR = "actvInstance_rooms_str";
        public static final String KEY_MEMBER_COUNT = "actvInstance_member_count";
        public static final String KEY_CAPACITY = "actvInstance_capacity";

        public static Uri buildActvInstanceUri(int blockId, int actvId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(blockId))
                    .appendPath(String.valueOf(actvId))
                    .build();
        }
        public static int getBlockId(@NonNull Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(1));
        }
        public static int getActvId(@NonNull Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(2));
        }

        @NonNull
        public static EighthActvInstance fromContentValues(@NonNull ContentValues values) {
            return new EighthActvInstance(
                    values.getAsInteger(KEY_ACTV_ID),
                    values.getAsInteger(KEY_BLOCK_ID),
                    values.getAsString(KEY_COMMENT),
                    values.getAsLong(KEY_FLAGS),
                    values.getAsString(KEY_ROOMS_STR),
                    values.getAsInteger(KEY_MEMBER_COUNT),
                    values.getAsInteger(KEY_CAPACITY)
            );
        }
        @NonNull
        public static ContentValues toContentValues(@NonNull EighthActvInstance actvInstance) {
            ContentValues values = new ContentValues();
            values.put(KEY_ACTV_ID, actvInstance.actvId);
            values.put(KEY_BLOCK_ID, actvInstance.blockId);
            values.put(KEY_COMMENT, actvInstance.comment);
            values.put(KEY_FLAGS, actvInstance.flags);
            values.put(KEY_ROOMS_STR, actvInstance.roomsStr);
            values.put(KEY_MEMBER_COUNT, actvInstance.memberCount);
            values.put(KEY_CAPACITY, actvInstance.capacity);
            return values;
        }
    }

    public static class Schedule implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_SCHEDULE).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.thyroxine.schedule";
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.thyroxine.schedule";

        public static final String KEY_BLOCK_ID = Blocks.KEY_BLOCK_ID;
        public static final String KEY_ACTV_ID = Actvs.KEY_ACTV_ID;

        public static Uri buildScheduleUri(int blockId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(blockId))
                    .build();
        }
        public static int getBlockId(@NonNull Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(1));
        }
    }

}
