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

import com.desklampstudios.thyroxine.auth.IodineAuthException;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.news.io.IodineNewsApi;
import com.desklampstudios.thyroxine.news.model.NewsEntry;
import com.desklampstudios.thyroxine.news.provider.NewsContract;
import com.desklampstudios.thyroxine.auth.IodineAuthenticator;
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
        boolean authTokenRetry = false;
        while (true) {
            // Part I. Get auth token
            String authToken;
            try {
                authToken = am.blockingGetAuthToken(account,
                        IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN, true);
            } catch (IOException e) {
                Log.e(TAG, "Connection error", e);
                syncResult.stats.numIoExceptions++;
                return;
            } catch (OperationCanceledException | AuthenticatorException e) {
                Log.e(TAG, "Authentication error", e);
                syncResult.stats.numAuthExceptions++;
                return;
            }
            Log.v(TAG, "Got auth token: " + authToken);


            // Part II. Get news list
            try {
                newsList = IodineNewsApi.fetchNewsList(getContext(), authToken);
            } catch (IodineAuthException.NotLoggedInException e) {
                Log.d(TAG, "Not logged in, invalidating auth token", e);
                am.invalidateAuthToken(account.type, authToken);

                // Automatically retry sync, but only once
                if (!authTokenRetry) {
                    authTokenRetry = true;
                    Log.d(TAG, "Retrying sync with new auth token.");
                    continue;
                } else {
                    Log.e(TAG, "Retried to get auth token already, quitting.");
                    syncResult.stats.numAuthExceptions++;
                    return;
                }
            } catch (IodineAuthException e) {
                Log.e(TAG, "Iodine auth error", e);
                syncResult.stats.numAuthExceptions++;
                return;
            } catch (IOException e) {
                Log.e(TAG, "Connection error", e);
                syncResult.stats.numIoExceptions++;
                return;
            } catch (XmlPullParserException e) {
                Log.e(TAG, "XML parsing error", e);
                syncResult.stats.numParseExceptions++;
                return;
            }
            break;
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
