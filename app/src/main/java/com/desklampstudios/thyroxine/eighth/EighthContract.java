package com.desklampstudios.thyroxine.eighth;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class EighthContract {
    // Warning: for now, also declared in strings.xml
    public static final String CONTENT_AUTHORITY = "com.desklampstudios.thyroxine.eighth";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_BLOCKS = "blocks";
    private static final String PATH_ACTVS = "actvs";
    private static final String PATH_ACTVINSTANCES = "actvInstances";

    public static class Blocks implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BLOCKS).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.thyroxine.block";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.thyroxine.block";

        public static final String BLOCK_ID = "block_id";
        public static final String DATE = "block_date";
        public static final String TYPE = "block_type";
        public static final String LOCKED = "block_locked";

        public static Uri buildBlockUri(String blockId) {
            return CONTENT_URI.buildUpon().appendPath(blockId).build();
        }
        public static String getBlockId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Actvs implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ACTVS).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.thyroxine.actv";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.thyroxine.actv";

        public static final int NOT_SELECTED_AID = 999;

        public static final int FLAG_ALL = 127;
        public static final int FLAG_RESTRICTED = 1;
        //public static final int FLAG_PRESIGN = 2;
        //public static final int FLAG_ONEADAY = 4;
        //public static final int FLAG_BOTHBLOCKS = 8;
        public static final int FLAG_STICKY = 16;
        public static final int FLAG_SPECIAL = 32;
        //public static final int FLAG_CALENDAR = 64;

        public static final String ACTV_ID = "actv_id";
        public static final String NAME = "actv_name";
        public static final String DESCRIPTION = "actv_description";
        public static final String FLAGS = "actv_flags";

        public static Uri buildActvUri(String actvId) {
            return CONTENT_URI.buildUpon().appendPath(actvId).build();
        }
        public static String getActvId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class ActvInstances implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ACTVINSTANCES).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.thyroxine.actvInstance";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.thyroxine.actvInstance";

        public static final int FLAG_ALL = 7168;
        public static final int FLAG_ATTENDANCETAKEN = 1024;
        public static final int FLAG_CANCELLED = 2048;
        //public static final int FLAG_ROOMCHANGED = 4096;

        public static final String ACTV_ID = "actv_id";
        public static final String BLOCK_ID = "block_id";
        public static final String COMMENT = "actvInstance_comment";
        public static final String FLAGS = "actvInstance_flags";
        public static final String ROOMS_STR = "actvInstance_rooms_str";
        public static final String MEMBER_COUNT = "actvInstance_member_count";
        public static final String CAPACITY = "actvInstance_capacity";

        public static Uri buildActvInstanceUri(String actvId, String blockId) {
            return CONTENT_URI.buildUpon().appendPath(actvId).appendPath(blockId).build();
        }
        public static String getActvId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
        public static String getBlockId(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

}
