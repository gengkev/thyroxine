package com.desklampstudios.thyroxine.news.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
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

import com.desklampstudios.thyroxine.iodine.IodineAuthException;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.iodine.IodineAuthUtils;
import com.desklampstudios.thyroxine.news.io.IodineNewsApi;
import com.desklampstudios.thyroxine.news.model.NewsEntry;
import com.desklampstudios.thyroxine.news.provider.NewsContract;
import com.desklampstudios.thyroxine.SyncUtils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NewsSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = NewsSyncAdapter.class.getSimpleName();

    // Sync intervals
    private static final int SYNC_INTERVAL = 2 * 60 * 60; // 2 hours
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    private static final SyncUtils.MergeInterface<NewsEntry, Integer> MERGE_INTERFACE =
            new SyncUtils.MergeInterface<NewsEntry, Integer>() {
                @Override
                public ContentValues toContentValues(NewsEntry item) {
                    return NewsContract.NewsEntries.toContentValues(item);
                }
                @Override
                public NewsEntry fromContentValues(ContentValues values) {
                    return NewsContract.NewsEntries.fromContentValues(values);
                }
                @Override
                public Integer getId(NewsEntry item) {
                    return item.newsId;
                }
                @Override
                public Uri buildContentUri(Integer id) {
                    return NewsContract.NewsEntries.buildEntryUri(id);
                }
            };

    public NewsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              @NonNull ContentProviderClient provider, @NonNull SyncResult syncResult) {
        Log.d(TAG, "onPerformSync for account " + account);
        final AccountManager am = AccountManager.get(getContext());

        List<NewsEntry> newsList;

        try {
            newsList = IodineAuthUtils.withAuthTokenBlocking(getContext(), account,
                    new IodineAuthUtils.AuthTokenOperation<List<NewsEntry>>() {
                        @Override
                        public List<NewsEntry> performOperation(String authToken) throws Exception {
                            return IodineNewsApi.fetchNewsList(getContext(), authToken);
                        }
                    });
        } catch (IOException e) {
            Log.e(TAG, "Connection error", e);
            syncResult.stats.numIoExceptions++;
            return;
        } catch (OperationCanceledException e) {
            Log.e(TAG, "Operation canceled", e);
            syncResult.stats.numAuthExceptions++;
            return;
        } catch (AuthenticatorException e) {
            Log.e(TAG, "Authentication error", e);
            syncResult.stats.numAuthExceptions++;
            return;
        } catch (IodineAuthException e) {
            Log.e(TAG, "Iodine auth error", e);
            syncResult.stats.numAuthExceptions++;
            return;
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XML parsing error", e);
            syncResult.stats.numParseExceptions++;
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Log.v(TAG, "Got news list (" + newsList.size() + ") entries");

        // Part III. Update entries in database
        try {
            updateNewsData(newsList, provider, syncResult);
        } catch (RemoteException | SQLiteException | OperationApplicationException e) {
            Log.e(TAG, "Error updating database", e);
            syncResult.databaseError = true;
            return;
        }

        Log.v(TAG, "Updated database; done syncing");
    }

    private void updateNewsData(@NonNull List<NewsEntry> newsList,
                                @NonNull ContentProviderClient provider,
                                @NonNull SyncResult syncResult)
            throws RemoteException, OperationApplicationException, SQLiteException {

        Cursor queryCursor = provider.query(
                NewsContract.NewsEntries.CONTENT_URI,
                null, null, null, null);
        assert queryCursor != null;

        ArrayList<ContentProviderOperation> batch = SyncUtils.createMergeBatch(
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
     * Helper method to have the sync adapter sync immediately.
     * @param account The account to sync immediately
     * @param manual Whether the sync was manually initiated
     */
    public static void syncImmediately(Account account, boolean manual) {
        Log.d(TAG, "Immediate sync requested");
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, manual);
        ContentResolver.requestSync(account, NewsContract.CONTENT_AUTHORITY, bundle);
    }


    /**
     * Configures sync scheduling.
     * @param account The Iodine account to configure sync with
     */
    public static void configureSync(Account account) {
        final String authority = NewsContract.CONTENT_AUTHORITY;

        // Configure syncing periodically
        Utils.configurePeriodicSync(account, authority, SYNC_INTERVAL, SYNC_FLEXTIME);

        // Enable automatic sync
        ContentResolver.setSyncAutomatically(account, authority, true);
    }
}
