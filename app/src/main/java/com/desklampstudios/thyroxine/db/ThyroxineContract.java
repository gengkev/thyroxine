package com.desklampstudios.thyroxine.db;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ThyroxineContract {
    public static final String CONTENT_AUTHORITY = "com.desklampstudios.thyroxine.provider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_NEWS = "news";
    public static final String PATH_ACTV = "actv";
    public static final String PATH_BLOCK = "block";

    public static final class NewsEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_NEWS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_NEWS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_NEWS;

        public static final String TABLE_NAME = "news";

        // Title of the post, stored as Text
        public static final String COLUMN_TITLE = "title";

        // Published date, stored as Integer (UTC time)
        public static final String COLUMN_DATE = "date";

        // URL to the post online, stored as Text
        public static final String COLUMN_LINK = "link";

        // Raw content of the post in HTML format, stored as Text
        public static final String COLUMN_CONTENT = "content";


        public static Uri buildNewsUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
        public static Uri buildNewsWithDate(String date) {
            return CONTENT_URI.buildUpon().appendQueryParameter(COLUMN_DATE, date).build();
        }
        public static String getDateFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static final class EighthActv implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ACTV).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_ACTV;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_ACTV;

        public static final String TABLE_NAME = "actv";

        // Activity ID (Integer)
        public static final String COLUMN_AID = "aid";
        // Name (Text)
        public static final String COLUMN_NAME = "name";
        // Description (Text)
        public static final String COLUMN_DESCRIPTION = "description";
        // Comment (Text)
        public static final String COLUMN_COMMENT = "comment";
        // Flags set (Integer)
        public static final String COLUMN_FLAGS = "flags";
        // Rooms string (Text)
        public static final String COLUMN_ROOMS = "rooms";
        // Member count (Integer)
        public static final String COLUMN_MEMBERS = "members";
        // Capacity (Integer)
        public static final String COLUMN_CAPACITY = "capacity";
        // Block (Reference to foreign key in Block table)
        public static final String COLUMN_BLOCK_KEY = "block_id";


        public static Uri buildActvUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
        public static Uri buildActvWithBlock(int bid) {
            return CONTENT_URI.buildUpon().appendQueryParameter(COLUMN_BLOCK_KEY, bid + "").build();
        }
        public static Uri buildActvAid(int aid) {
            return CONTENT_URI.buildUpon().appendPath(aid + "").build();
        }
        public static Uri buildActvAidWithBlock(int aid, int bid) {
            return CONTENT_URI.buildUpon().appendPath(aid + "")
                    .appendQueryParameter(COLUMN_BLOCK_KEY, bid + "").build();
        }
        public static int getAidFromUri(Uri uri) {
            try {
                return Integer.parseInt(uri.getPathSegments().get(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        public static int getBlockFromUri(Uri uri) {
            try {
                return Integer.parseInt(uri.getQueryParameter(COLUMN_BLOCK_KEY));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }

    public static final class EighthBlock implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BLOCK).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_BLOCK;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_BLOCK;

        public static final String TABLE_NAME = "block";

        // Block ID is the same as _ID

        // Date (Text with format yyyy-MM-dd)
        public static final String COLUMN_DATE = "date";
        // Type (Text)
        public static final String COLUMN_TYPE = "type";
        // Locked (Integer)
        public static final String COLUMN_LOCKED = "locked";


        public static Uri buildBlockUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
        public static Uri buildBlockDate(String date) {
            return CONTENT_URI.buildUpon().appendPath(date).build();
        }
        public static Uri buildBlockDateType(String date, String type) {
            return CONTENT_URI.buildUpon().appendPath(date).appendPath(type).build();
        }
        public static String getDateFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }
        public static String getTypeFromUri(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    public static String getDbDateString(Date date) {
        return new SimpleDateFormat("yyyyMMdd").format(date);
    }
}
