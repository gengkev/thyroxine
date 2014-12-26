package com.desklampstudios.thyroxine.news;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.sync.StubAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class NewsSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = NewsSyncAdapter.class.getSimpleName();

    // Sync intervals
    private static final int SYNC_INTERVAL = 2 * 60 * 60; // 2 hours
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    private static final Utils.MergeInterface<NewsEntry, String> MERGE_INTERFACE =
            new Utils.MergeInterface<NewsEntry, String>() {
                @Override
                public ContentValues toContentValues(NewsEntry item) {
                    return NewsContract.NewsEntries.toContentValues(item);
                }
                @Override
                public NewsEntry fromContentValues(ContentValues values) {
                    return NewsContract.NewsEntries.fromContentValues(values);
                }
                @Override
                public String getId(NewsEntry item) {
                    return item.link;
                }
                @Override
                public Uri buildContentUri(String id) {
                    return NewsContract.NewsEntries.buildEntryUri(id);
                }
            };

    public NewsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    // TODO: improve. syncResult?
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "onPerformSync for account " + account);

        // Part I. Get news list
        List<NewsEntry> newsList;
        try {
            newsList = fetchNews();
        } catch (IOException e) {
            Log.e(TAG, "Connection error: " + e.toString());
            syncResult.stats.numIoExceptions++;
            return;
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XML error: " + e.toString());
            syncResult.stats.numParseExceptions++;
            return;
        }
        Log.v(TAG, "Got news list (" + newsList.size() + ") entries");


        // Part II. Update entries in database
        try {
            updateNewsData(newsList, provider, syncResult);
        } catch (RemoteException | SQLiteException | OperationApplicationException e) {
            Log.e(TAG, "Error updating database", e);
            syncResult.databaseError = true;
            return;
        }

        Log.v(TAG, "Updated database; done syncing");
    }

    private List<NewsEntry> fetchNews() throws IOException, XmlPullParserException {

        InputStream stream = null;
        NewsFeedParser parser = null;
        List<NewsEntry> entries = new ArrayList<>();
        NewsEntry entry;

        try {
            stream = IodineApiHelper.getPublicNewsFeed();
            parser = new NewsFeedParser(getContext());
            parser.beginFeed(stream);

            entry = parser.nextEntry();
            while (entry != null) {
                entries.add(entry);
                entry = parser.nextEntry();
            }
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

        return entries;
    }

    private void updateNewsData(@NonNull List<NewsEntry> newsList,
                                @NonNull ContentProviderClient provider,
                                @NonNull SyncResult syncResult)
            throws RemoteException, OperationApplicationException, SQLiteException {

        Cursor queryCursor = provider.query(
                NewsContract.NewsEntries.CONTENT_URI,
                null, null, null, null);
        assert queryCursor != null;

        ArrayList<ContentProviderOperation> batch = Utils.createMergeBatch(
                NewsEntry.class.getSimpleName(),
                newsList,
                queryCursor,
                NewsContract.NewsEntries.CONTENT_URI,
                MERGE_INTERFACE,
                syncResult.stats);

        ContentProviderResult[] results = provider.applyBatch(batch);
        Log.d(TAG, results.length + " operations performed.");
        // Log.d(TAG, "results: " + Arrays.toString(results));

        final ContentResolver resolver = getContext().getContentResolver();
        resolver.notifyChange(
                NewsContract.NewsEntries.CONTENT_URI,
                null, false);  // IMPORTANT: Do not sync to network
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
                NewsContract.CONTENT_AUTHORITY, bundle);
    }


    /**
     * Configures sync scheduling. Called from MainActivity.
     * @param newAccount The stub account that was created.
     */
    public static void configureSync(Account newAccount) {
        final String authority = NewsContract.CONTENT_AUTHORITY;

        // Configure syncing periodically
        Utils.configurePeriodicSync(newAccount, authority, SYNC_INTERVAL, SYNC_FLEXTIME);

        // Configure syncing automatically
        ContentResolver.setSyncAutomatically(newAccount, authority, true);
    }
}
