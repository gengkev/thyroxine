package com.desklampstudios.thyroxine.eighth;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.desklampstudios.thyroxine.eighth.EighthContract.ActvInstances;
import static com.desklampstudios.thyroxine.eighth.EighthContract.Actvs;
import static com.desklampstudios.thyroxine.eighth.EighthContract.Blocks;
import static com.desklampstudios.thyroxine.eighth.EighthContract.Schedule;

public class EighthDatabase extends SQLiteOpenHelper {
    private static final String TAG = EighthDatabase.class.getSimpleName();

    private static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "thyroxine.db.eighth";

    interface Tables {
        String BLOCKS = "blocks";
        String ACTVS = "actvs";
        String ACTVINSTANCES = "actvInstances";
        String SCHEDULE = "schedule";

        String BLOCKS_JOIN_SCHEDULE_ACTVS_ACTVINSTANCES = "blocks "
                + "LEFT OUTER JOIN schedule ON blocks.block_id=schedule.block_id "
                + "LEFT OUTER JOIN actvs ON schedule.actv_id=actvs.actv_id "
                + "LEFT OUTER JOIN actvInstances ON schedule.actv_id=actvInstances.actv_id "
                    + "AND blocks.block_id=actvInstances.block_id";
        String ACTVINSTANCES_JOIN_ACTVS_BLOCKS = "actvInstances "
                + "LEFT OUTER JOIN blocks ON actvInstances.block_id=blocks.block_id "
                + "LEFT OUTER JOIN actvs ON actvInstances.actv_id=actvs.actv_id";
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
                        Blocks.LOCKED + " INTEGER, " +

                        // Block ID is unique
                        "UNIQUE (" + Blocks.BLOCK_ID + ") ON CONFLICT REPLACE)";

        final String SQL_CREATE_ACTV_TABLE =
                "CREATE TABLE " + Tables.ACTVS + " (" +
                        Actvs._ID + " INTEGER PRIMARY KEY , " +
                        Actvs.ACTV_ID + " INTEGER NOT NULL, " +
                        Actvs.NAME + " TEXT NOT NULL, " +
                        Actvs.DESCRIPTION + " TEXT NOT NULL, " +
                        Actvs.FLAGS + " INTEGER NOT NULL, " +

                        // Actv ID is unique
                        "UNIQUE (" + Actvs.ACTV_ID + ") ON CONFLICT REPLACE)";

        final String SQL_CREATE_ACTVINSTANCE_TABLE =
                "CREATE TABLE " + Tables.ACTVINSTANCES + " (" +
                        ActvInstances._ID + " INTEGER PRIMARY KEY ," +
                        ActvInstances.ACTV_ID + " INTEGER NOT NULL, " +
                        ActvInstances.BLOCK_ID + " INTEGER NOT NULL, " +
                        ActvInstances.COMMENT + " TEXT NOT NULL, " +
                        ActvInstances.FLAGS + " INTEGER NOT NULL, " +
                        ActvInstances.ROOMS_STR + " TEXT, " +
                        ActvInstances.MEMBER_COUNT + " INTEGER, " +
                        ActvInstances.CAPACITY + " INTEGER, " +

                        // Only one AID/BID pair should exist at a time.
                        "UNIQUE (" + ActvInstances.BLOCK_ID + ", " + ActvInstances.ACTV_ID +
                        ") ON CONFLICT REPLACE)";

        final String SQL_CREATE_SCHEDULE_TABLE =
                "CREATE TABLE " + Tables.SCHEDULE + " (" +
                        Schedule._ID + " INTEGER PRIMARY KEY, " +
                        Schedule.BLOCK_ID + " INTEGER NOT NULL, " +
                        Schedule.ACTV_ID + " INTEGER NOT NULL, " +

                        // BID is unique
                        "UNIQUE (" + Schedule.BLOCK_ID + ") ON CONFLICT REPLACE)";

        db.execSQL(SQL_CREATE_BLOCK_TABLE);
        db.execSQL(SQL_CREATE_ACTV_TABLE);
        db.execSQL(SQL_CREATE_ACTVINSTANCE_TABLE);
        db.execSQL(SQL_CREATE_SCHEDULE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.BLOCKS);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.ACTVS);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.ACTVINSTANCES);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.SCHEDULE);
        this.onCreate(db);
    }

}