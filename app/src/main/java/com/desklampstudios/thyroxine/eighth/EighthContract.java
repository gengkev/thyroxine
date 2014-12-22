package com.desklampstudios.thyroxine.eighth;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.BaseColumns;

public class EighthContract {
    // Warning: for now, also declared in strings.xml
    public static final String CONTENT_AUTHORITY = "com.desklampstudios.thyroxine.eighth";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_BLOCKS = "blocks";
    private static final String PATH_ACTVS = "actvs";
    private static final String PATH_ACTVINSTANCES = "actvInstances";
    private static final String PATH_SCHEDULE = "schedule";

    public static class Blocks implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BLOCKS).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.thyroxine.block";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.thyroxine.block";

        public static final String BLOCK_ID = "block_id";
        public static final String DATE = "block_date";
        public static final String TYPE = "block_type";
        public static final String LOCKED = "block_locked";

        public static Uri buildBlockUri(int blockId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(blockId))
                    .build();
        }
        public static int getBlockId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(1));
        }

        public static EighthBlock contentValuesToEighthBlock(ContentValues values) {
            return new EighthBlock(
                    values.getAsInteger(BLOCK_ID),
                    values.getAsString(DATE),
                    values.getAsString(TYPE),
                    values.getAsBoolean(LOCKED)
            );
        }
        public static ContentValues eighthBlockToContentValues(EighthBlock block) {
            ContentValues values = new ContentValues();
            values.put(BLOCK_ID, block.blockId);
            values.put(DATE, block.date);
            values.put(TYPE, block.type);
            values.put(LOCKED, block.locked);
            return values;
        }
    }

    public static class Actvs implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ACTVS).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.thyroxine.actv";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.thyroxine.actv";

        public static final String ACTV_ID = "actv_id";
        public static final String NAME = "actv_name";
        public static final String DESCRIPTION = "actv_description";
        public static final String FLAGS = "actv_flags";

        public static Uri buildActvUri(int actvId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(actvId))
                    .build();
        }
        public static int getActvId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(1));
        }
    }

    public static class ActvInstances implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ACTVINSTANCES).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.thyroxine.actvInstance";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.thyroxine.actvInstance";

        public static final String BLOCK_ID = Blocks.BLOCK_ID;
        public static final String ACTV_ID = Actvs.ACTV_ID;
        public static final String COMMENT = "actvInstance_comment";
        public static final String FLAGS = "actvInstance_flags";
        public static final String ROOMS_STR = "actvInstance_rooms_str";
        public static final String MEMBER_COUNT = "actvInstance_member_count";
        public static final String CAPACITY = "actvInstance_capacity";

        public static Uri buildActvInstanceUri(int blockId, int actvId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(blockId))
                    .appendPath(String.valueOf(actvId))
                    .build();
        }
        public static int getBlockId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(1));
        }
        public static int getActvId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(2));
        }
    }

    public static class Schedule implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SCHEDULE).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.thyroxine.schedule";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.thyroxine.schedule";

        public static final String BLOCK_ID = Blocks.BLOCK_ID;
        public static final String ACTV_ID = Actvs.ACTV_ID;

        public static Uri buildScheduleUri(int blockId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(blockId))
                    .build();
        }
        public static int getBlockId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(1));
        }
    }

}
