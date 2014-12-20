package com.desklampstudios.thyroxine.news;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class NewsDbHelper extends SQLiteOpenHelper {
    private static final String TAG = NewsDbHelper.class.getSimpleName();

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "thyroxine.db.news";

    // NewsEntry table
    static final String TABLE_NEWS = "news";
    static final String KEY_NEWS_ID = "_id";
    static final String KEY_NEWS_TITLE = "title";
    static final String KEY_NEWS_DATE = "date";
    static final String KEY_NEWS_LINK = "link";
    static final String KEY_NEWS_CONTENT = "content";
    static final String KEY_NEWS_SNIPPET = "content_snippet";

    public NewsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_NEWSENTRY_TABLE =
                "CREATE TABLE " + TABLE_NEWS + " (" +
                        KEY_NEWS_ID + " INTEGER PRIMARY KEY, " +
                        KEY_NEWS_TITLE + " TEXT, " +
                        KEY_NEWS_DATE + " INTEGER NOT NULL, " +
                        KEY_NEWS_LINK + " TEXT, " +
                        KEY_NEWS_CONTENT + " TEXT, " +
                        KEY_NEWS_SNIPPET + " TEXT" +
                        ");";

        Log.d(TAG, "Creating tables: "+ SQL_CREATE_NEWSENTRY_TABLE);
        db.execSQL(SQL_CREATE_NEWSENTRY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NEWS);
        this.onCreate(db);
    }

    // converting to and from not-really-pojos yooooo
    // this is so yooo i can't even :')
    static NewsEntry contentValuesToNewsEntry(ContentValues values) {
        return new NewsEntry(
                values.getAsString(KEY_NEWS_LINK),
                values.getAsString(KEY_NEWS_TITLE),
                values.getAsLong(KEY_NEWS_DATE),
                values.getAsString(KEY_NEWS_CONTENT),
                values.getAsString(KEY_NEWS_SNIPPET)
        );
    }
    static NewsEntry cursorRowToNewsEntry(Cursor cursor) {
        ContentValues values = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, values);
        return contentValuesToNewsEntry(values);
    }
    static ContentValues newsEntryToContentValues(NewsEntry entry) {
        ContentValues values = new ContentValues();
        values.put(KEY_NEWS_TITLE, entry.title);
        values.put(KEY_NEWS_DATE, entry.published);
        values.put(KEY_NEWS_LINK, entry.link);
        values.put(KEY_NEWS_CONTENT, entry.contentRaw);
        values.put(KEY_NEWS_SNIPPET, entry.contentSnippet);
        return values;
    }
}