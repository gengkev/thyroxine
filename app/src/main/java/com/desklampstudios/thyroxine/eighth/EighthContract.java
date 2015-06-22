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

        @NonNull
        public static Uri buildBlockUri(int blockId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(blockId))
                    .build();
        }
        @NonNull
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
            return new EighthBlock.Builder()
                    .blockId(values.getAsInteger(KEY_BLOCK_ID))
                    .date(values.getAsString(KEY_DATE))
                    .type(values.getAsString(KEY_TYPE))
                    .locked(values.getAsBoolean(KEY_LOCKED))
                    .build();
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

        @NonNull
        public static Uri buildActvUri(int actvId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(actvId))
                    .build();
        }
        @NonNull
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
            return new EighthActv.Builder()
                    .actvId(values.getAsInteger(KEY_ACTV_ID))
                    .name(values.getAsString(KEY_NAME))
                    .description(values.getAsString(KEY_DESCRIPTION))
                    .flags(values.getAsLong(KEY_FLAGS))
                    .build();
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
        public static final String KEY_SPONSORS_STR = "actvInstance_sponsors_str";
        public static final String KEY_MEMBER_COUNT = "actvInstance_member_count";
        public static final String KEY_CAPACITY = "actvInstance_capacity";

        @NonNull
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
            return new EighthActvInstance.Builder()
                    .actvId(values.getAsInteger(KEY_ACTV_ID))
                    .blockId(values.getAsInteger(KEY_BLOCK_ID))
                    .comment(values.getAsString(KEY_COMMENT))
                    .flags(values.getAsLong(KEY_FLAGS))
                    .roomsStr(values.getAsString(KEY_ROOMS_STR))
                    .sponsorsStr(values.getAsString(KEY_SPONSORS_STR))
                    .memberCount(values.getAsInteger(KEY_MEMBER_COUNT))
                    .capacity(values.getAsInteger(KEY_CAPACITY))
                    .build();
        }
        @NonNull
        public static ContentValues toContentValues(@NonNull EighthActvInstance actvInstance) {
            ContentValues values = new ContentValues();
            values.put(KEY_ACTV_ID, actvInstance.actvId);
            values.put(KEY_BLOCK_ID, actvInstance.blockId);
            values.put(KEY_COMMENT, actvInstance.comment);
            values.put(KEY_FLAGS, actvInstance.flags);
            values.put(KEY_ROOMS_STR, actvInstance.roomsStr);
            values.put(KEY_SPONSORS_STR, actvInstance.sponsorsStr);
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

        @NonNull
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
