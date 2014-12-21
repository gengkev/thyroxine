package com.desklampstudios.thyroxine.eighth;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.desklampstudios.thyroxine.eighth.EighthContract.*;

public class EighthDatabase extends SQLiteOpenHelper {
    private static final String TAG = EighthDatabase.class.getSimpleName();

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "thyroxine.db.eighth";

    interface Tables {
        String BLOCKS = "blocks";
        String ACTVS = "actvs";
        String ACTVINSTANCES = "actvInstances";

        String ACTVINSTANCES_JOIN_ACTVS_BLOCKS = "actvInstances "
                + "LEFT OUTER JOIN actvs ON actvInstances.actv_id=actvs.actv_id "
                + "LEFT OUTER JOIN blocks ON blockInstances.actv_id=blocks.actv_id";
    }


    public EighthDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_BLOCK_TABLE =
                "CREATE TABLE " + Tables.BLOCKS + " (" +
                        Blocks._ID + " INTEGER PRIMARY KEY, " +
                        Blocks.BLOCK_ID + " INTEGER NOT NULL, " +
                        Blocks.DATE + " TEXT NOT NULL, " +
                        Blocks.TYPE + " TEXT NOT NULL, " +
                        Blocks.LOCKED + " INTEGER" +

                        // Block ID is unique
                        "UNIQUE (" + Blocks.BLOCK_ID + ") ON CONFLICT REPLACE)";

        final String SQL_CREATE_ACTV_TABLE =
                "CREATE TABLE " + Tables.ACTVS + " (" +
                        Actvs._ID + " INTEGER PRIMARY KEY , " +
                        Actvs.ACTV_ID + " INTEGER NOT NULL, " +
                        Actvs.NAME + " TEXT, " +
                        Actvs.DESCRIPTION + " TEXT, " +
                        Actvs.FLAGS + " INTEGER, " +

                        // Actv ID is unique
                        "UNIQUE (" + Actvs.ACTV_ID + ") ON CONFLICT REPLACE)";

        final String SQL_CREATE_ACTVINSTANCE_TABLE =
                "CREATE TABLE " + Tables.ACTVINSTANCES + " (" +
                        ActvInstances._ID + " INTEGER PRIMARY KEY , " +
                        ActvInstances.ACTV_ID + " INTEGER NOT NULL, " +
                        ActvInstances.BLOCK_ID + " INTEGER NOT NULL, " +
                        ActvInstances.COMMENT + " TEXT, " +
                        ActvInstances.FLAGS + " INTEGER, " +
                        ActvInstances.ROOMS_STR + " TEXT, " +
                        ActvInstances.MEMBER_COUNT + " INTEGER, " +
                        ActvInstances.CAPACITY + " INTEGER, " +

                        // Set up the actv_id column as a foreign key to actv table
                        "FOREIGN KEY (" + ActvInstances.ACTV_ID + ") REFERENCES " +
                        Tables.ACTVS + " (" + Actvs._ID + "), " +

                        // Set up the block_id column as a foreign key to block table
                        "FOREIGN KEY (" + ActvInstances.BLOCK_ID + ") REFERENCES " +
                        Tables.BLOCKS + " (" + Blocks._ID + "), " +

                        // Only one AID/BID pair should exist at a time.
                        "UNIQUE (" + ActvInstances.ACTV_ID + ", " + ActvInstances.BLOCK_ID + ") ON CONFLICT REPLACE)";

        db.execSQL(SQL_CREATE_BLOCK_TABLE);
        db.execSQL(SQL_CREATE_ACTV_TABLE);
        db.execSQL(SQL_CREATE_ACTVINSTANCE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.BLOCKS);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.ACTVS);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.ACTVINSTANCES);
        this.onCreate(db);
    }

}