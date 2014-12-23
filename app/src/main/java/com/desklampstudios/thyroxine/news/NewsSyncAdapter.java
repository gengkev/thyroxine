package com.desklampstudios.thyroxine.news;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.sync.StubAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class NewsSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = NewsSyncAdapter.class.getSimpleName();

    // Sync intervals
    private static final int SYNC_INTERVAL = 2 * 60 * 60; // 2 hours
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    public NewsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    // TODO: improve. syncResult?
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {

        InputStream stream = null;
        NewsFeedParser parser = null;
        //List<NewsEntry> entries = new ArrayList<NewsEntry>();

        try {
            stream = IodineApiHelper.getPublicNewsFeed();
            parser = new NewsFeedParser(getContext());
            parser.beginFeed(stream);

            NewsEntry entry = parser.nextEntry();
            while (entry != null) {
                try {
                    updateDatabase(entry, provider);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException: "+ e);
                    syncResult.databaseError = true;
                }
                //entries.add(entry);
                entry = parser.nextEntry();
            }

            Log.d(TAG, "Sync completed successfully!");
        } catch (IOException e) {
            Log.e(TAG, "Connection error: " + e.toString());
            syncResult.stats.numIoExceptions++;
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XML error: " + e.toString());
            syncResult.stats.numParseExceptions++;
        } finally {
            if (parser != null)
                parser.stopParse();
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException when closing stream: " + e);
            }
        }
    }

    /**
     * Processes an incoming NewsEntry object, fresh from the parser.
     * If the database already contains it, updates it if necessary;
     * otherwise, it is inserted into the database.
     * @param entry NewsEntry to update the database with.
     */
    private void updateDatabase(NewsEntry entry,
                                ContentProviderClient provider) throws RemoteException {
        // test if record exists
        Cursor c = provider.query(NewsContract.NewsEntries.CONTENT_URI,
                null, // columns
                NewsContract.NewsEntries.KEY_LINK + " = ?", // selection
                new String[]{ entry.link }, // selectionArgs
                null // orderBy
        );

        // Record exists in the database; update if necessary
        if (c.moveToFirst()) {
            Log.v(TAG, "NewsEntry with same link already exists (link " + entry.link + ")");
            ContentValues oldValues = Utils.cursorRowToContentValues(c);
            NewsEntry oldEntry = NewsContract.NewsEntries.contentValuesToNewsEntry(oldValues);

            // Record has changed; update it
            if (!entry.equals(oldEntry)) {
                Log.v(TAG, "NewsEntry not equal, needs to be updated: " + entry);
                ContentValues newValues = NewsContract.NewsEntries.newsEntryToContentValues(entry);

                int rowsAffected = provider.update(NewsContract.NewsEntries.CONTENT_URI,
                        newValues,
                        NewsContract.NewsEntries.KEY_LINK + "=?", // selection
                        new String[]{ entry.link } // selectionArgs
                );
                Log.v(TAG, rowsAffected + " rows updated.");
            }
        }
        // Record does not exist in the database; insert it
        else {
            Log.v(TAG, "New NewsEntry to be inserted (link " + entry.link + ")");
            ContentValues values = NewsContract.NewsEntries.newsEntryToContentValues(entry);

            Uri uri = provider.insert(NewsContract.NewsEntries.CONTENT_URI, values);
            Log.v(TAG, "Inserted new entry with uri: " + uri);
        }

        c.close();
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(StubAuthenticator.getStubAccount(context),
                context.getString(R.string.news_content_authority), bundle);
    }


    /**
     * Called from StubAuthenticator when a stub account is created.
     * @param newAccount The stub account that was created.
     * @param context The context used to get strings
     */
    public static void onAccountCreated(Account newAccount, Context context) {
        String authority = context.getString(R.string.news_content_authority);

        // Configure syncing periodically
        Utils.configurePeriodicSync(newAccount, authority, SYNC_INTERVAL, SYNC_FLEXTIME);

        // Configure syncing automatically
        ContentResolver.setSyncAutomatically(newAccount, authority, true);

        // Get things started with an initial sync
        Log.d(TAG, "Performing initial sync");
        NewsSyncAdapter.syncImmediately(context);
    }
}
