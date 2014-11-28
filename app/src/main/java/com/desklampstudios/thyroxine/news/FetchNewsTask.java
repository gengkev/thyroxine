package com.desklampstudios.thyroxine.news;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.db.ThyroxineContract;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FetchNewsTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = FetchNewsTask.class.getSimpleName();
    private Exception exception = null;
    private final Context mContext;

    public FetchNewsTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        InputStream stream = null;
        IodineNewsFeedParser parser;
        List<IodineNewsEntry> entries = new ArrayList<IodineNewsEntry>();

        try {
            stream = IodineApiHelper.getPublicNewsFeed();
            parser = new IodineNewsFeedParser();
            parser.beginFeed(stream);

            IodineNewsEntry entry;
            while (!isCancelled()) {
                entry = parser.nextEntry();

                if (entry == null)
                    break;

                addNews(entry);
                entries.add(entry);
            }

        }
        catch (IOException e) {
            Log.e(TAG, "Connection error: " + e.toString());
            exception = e;
            return null;
        }
        catch (XmlPullParserException e) {
            Log.e(TAG, "XML error: " + e.toString());
            exception = e;
            return null;
        }
        finally {
            try {
                if (stream != null)
                    stream.close();
            }
            catch (IOException e) {
                Log.e(TAG, "IOException when closing stream: " + e);
            }
        }

        Log.v(TAG, "Entries: " + entries);

        return null;
    }

    private long addNews(IodineNewsEntry entry) {
        Log.v(TAG, "Inserting entry: " + entry);

        Cursor cursor = mContext.getContentResolver().query(
                ThyroxineContract.NewsEntry.CONTENT_URI,
                new String[]{ThyroxineContract.NewsEntry._ID},
                ThyroxineContract.NewsEntry.COLUMN_LINK + " = ?",
                new String[]{entry.link},
                null
        );

        if (cursor.moveToFirst()) {
            // found in database!
            Log.v(TAG, "Found in database");
            int index = cursor.getColumnIndex(ThyroxineContract.NewsEntry._ID);
            return cursor.getLong(index);
        }
        else {
            Log.v(TAG, "Not found in database, inserting");
            // didn't find in database
            Uri insertUri = mContext.getContentResolver().insert(
                    ThyroxineContract.NewsEntry.CONTENT_URI, entry.toContentValues());
            return ContentUris.parseId(insertUri);
        }
    }
}
