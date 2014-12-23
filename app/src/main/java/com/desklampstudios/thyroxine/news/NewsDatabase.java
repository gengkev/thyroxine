package com.desklampstudios.thyroxine.news;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.desklampstudios.thyroxine.news.NewsContract.NewsEntries;

class NewsDatabase extends SQLiteOpenHelper {
    private static final String TAG = NewsDatabase.class.getSimpleName();

    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "thyroxine.db.news";

    // NewsEntry table
    interface Tables {
        static final String TABLE_NEWSENTRIES = "newsEntries";
    }

    public NewsDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_NEWSENTRIES_TABLE =
                "CREATE TABLE " + Tables.TABLE_NEWSENTRIES + " (" +
                        NewsEntries._ID + " INTEGER PRIMARY KEY, " +
                        NewsEntries.KEY_TITLE + " TEXT, " +
                        NewsEntries.KEY_DATE + " INTEGER NOT NULL, " +
                        NewsEntries.KEY_LINK + " TEXT, " +
                        NewsEntries.KEY_CONTENT + " TEXT, " +
                        NewsEntries.KEY_SNIPPET + " TEXT" +
                        ");";

        Log.d(TAG, "Creating tables: "+ SQL_CREATE_NEWSENTRIES_TABLE);
        db.execSQL(SQL_CREATE_NEWSENTRIES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.TABLE_NEWSENTRIES);
        this.onCreate(db);
    }
}