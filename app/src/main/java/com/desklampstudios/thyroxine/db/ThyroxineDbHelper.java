package com.desklampstudios.thyroxine.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.desklampstudios.thyroxine.db.ThyroxineContract.*;

public class ThyroxineDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "thyroxine.db";

    public ThyroxineDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_NEWS_TABLE =
                "CREATE TABLE " + NewsEntry.TABLE_NAME + " (" +
                        NewsEntry._ID + " INTEGER PRIMARY KEY, " +
                        NewsEntry.COLUMN_TITLE + " TEXT, " +
                        NewsEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                        NewsEntry.COLUMN_LINK + " TEXT, " +
                        NewsEntry.COLUMN_CONTENT + " TEXT" +
                ");";

        final String SQL_CREATE_BLOCK_TABLE =
                "CREATE TABLE " + EighthBlock.TABLE_NAME + " (" +
                        EighthBlock._ID + " INTEGER PRIMARY KEY, " +
                        EighthBlock.COLUMN_DATE + " TEXT NOT NULL, " +
                        EighthBlock.COLUMN_TYPE + " TEXT NOT NULL, " +
                        EighthBlock.COLUMN_LOCKED + " INTEGER" +
                ");";

        final String SQL_CREATE_ACTV_TABLE =
                "CREATE TABLE " + EighthActv.TABLE_NAME + " (" +
                        EighthActv._ID + " INTEGER PRIMARY KEY , " +
                        EighthActv.COLUMN_AID + " INTEGER NOT NULL, " +
                        EighthActv.COLUMN_NAME + " TEXT, " +
                        EighthActv.COLUMN_DESCRIPTION + " TEXT, " +
                        EighthActv.COLUMN_COMMENT + " COMMENT, " +
                        EighthActv.COLUMN_FLAGS + " INTEGER, " +
                        EighthActv.COLUMN_ROOMS + " TEXT, " +
                        EighthActv.COLUMN_MEMBERS + " INTEGER, " +
                        EighthActv.COLUMN_CAPACITY + " INTEGER, " +

                        // Set up the block column as a foreign key to block table
                        "FOREIGN KEY (" + EighthActv.COLUMN_BLOCK_KEY + ") REFERENCES " +
                        EighthBlock.TABLE_NAME + " (" + EighthBlock._ID + "), " +

                        // Only one AID/BID pair should exist at a time.
                        "UNIQUE (" + EighthActv.COLUMN_AID + ", " +
                        EighthActv.COLUMN_BLOCK_KEY + ") ON CONFLICT REPLACE" +
                ");";

        db.execSQL(SQL_CREATE_NEWS_TABLE);
        db.execSQL(SQL_CREATE_BLOCK_TABLE);
        db.execSQL(SQL_CREATE_ACTV_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + NewsEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + EighthBlock.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + EighthActv.TABLE_NAME);
        this.onCreate(db);
    }
}
