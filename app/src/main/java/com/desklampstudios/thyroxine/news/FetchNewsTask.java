package com.desklampstudios.thyroxine.news;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.desklampstudios.thyroxine.IodineApiHelper;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


class FetchNewsTask extends AsyncTask<Void, NewsEntry, List<NewsEntry>> {
    private static final String TAG = FetchNewsTask.class.getSimpleName();

    private Exception exception = null;
    private boolean loggedIn;
    private ContentResolver mResolver;

    public FetchNewsTask(boolean loggedIn, ContentResolver resolver) {
        this.loggedIn = loggedIn;
        this.mResolver = resolver;
    }

    @Override
    protected List<NewsEntry> doInBackground(Void... params) {
        InputStream stream = null;
        IodineNewsFeedParser parser;
        List<NewsEntry> entries = new ArrayList<NewsEntry>();

        try {
            if (loggedIn) {
                stream = IodineApiHelper.getPrivateNewsFeed();
            } else {
                stream = IodineApiHelper.getPublicNewsFeed();
            }
            parser = new IodineNewsFeedParser();
            parser.beginFeed(stream);

            NewsEntry entry;
            while (!isCancelled()) {
                entry = parser.nextEntry();

                if (entry == null)
                    break;

                publishProgress(entry);
                entries.add(entry);
            }

        } catch (IOException e) {
            Log.e(TAG, "Connection error: " + e.toString());
            exception = e;
            return null;
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XML error: " + e.toString());
            exception = e;
            return null;
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException when closing stream: " + e);
            }
        }

        return entries;
    }

    @Override
    protected void onProgressUpdate(NewsEntry... entries) {
        // test if record exists
        Cursor c = mResolver.query(NewsProvider.CONTENT_URI_NEWS,
                null, // columns
                NewsDbHelper.KEY_NEWS_LINK + " = ?", // selection
                new String[]{ entries[0].link }, // selectionArgs
                null // orderBy
        );

        if (c.moveToFirst()) { // already exists
            Log.v(TAG, "NewsEntry with same link already exists (link " + entries[0].link + ")");

            NewsEntry oldEntry = NewsDbHelper.cursorRowToNewsEntry(c);
            if (!entries[0].equals(oldEntry)) { // changed; pls update
                Log.v(TAG, "NewsEntry not equal, needs to be updated: " + entries[0]);
                ContentValues newValues = NewsDbHelper.newsEntryToContentValues(entries[0]);

                int rowsAffected = mResolver.update(NewsProvider.CONTENT_URI_NEWS,
                        newValues,
                        NewsDbHelper.KEY_NEWS_LINK + " = ?", // selection
                        new String[]{ entries[0].link } // selectionArgs
                );
                Log.v(TAG, rowsAffected + " rows updated.");
            }
        } else { // must insert
            Log.v(TAG, "New NewsEntry to be inserted (link " + entries[0].link + ")");
            ContentValues map = NewsDbHelper.newsEntryToContentValues(entries[0]);

            Uri uri = mResolver.insert(NewsProvider.CONTENT_URI_NEWS, map);
            Log.v(TAG, "Inserted new entry with uri: " + uri);
        }

        c.close();
    }

    @Override
    protected void onPostExecute(List<NewsEntry> entries) {
        if (exception != null) {
            Log.e(TAG, "Error getting feed: " + exception);
            return;
        }

        Log.i(TAG, "Got feed (" + entries.size() + " entries)");
    }

    @Override
    protected void onCancelled() {
    }
}
