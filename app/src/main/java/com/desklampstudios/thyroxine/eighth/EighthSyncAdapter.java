package com.desklampstudios.thyroxine.eighth;

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
import android.util.Pair;

import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;
import com.desklampstudios.thyroxine.sync.SyncUtils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.desklampstudios.thyroxine.eighth.EighthListBlocksParser.EighthBlockAndActv;

public class EighthSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = EighthSyncAdapter.class.getSimpleName();

    // Sync intervals
    private static final int SYNC_INTERVAL = 2 * 60 * 60; // 2 hours
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    private static final String[] BLOCK_PROJECTION = new String[] {
            EighthContract.Blocks._ID,
            EighthContract.Blocks.KEY_BLOCK_ID,
            EighthContract.Blocks.KEY_TYPE,
            EighthContract.Blocks.KEY_DATE,
            EighthContract.Blocks.KEY_LOCKED
    };
    private static final String[] SCHEDULE_PROJECTION = new String[] {
            EighthContract.Schedule._ID,
            EighthContract.Schedule.KEY_BLOCK_ID,
            EighthContract.Schedule.KEY_ACTV_ID
    };
    private static final String[] ACTVS_PROJECTION = new String[] {
            EighthContract.Actvs._ID,
            EighthContract.Actvs.KEY_ACTV_ID,
            EighthContract.Actvs.KEY_NAME,
            EighthContract.Actvs.KEY_FLAGS,
            EighthContract.Actvs.KEY_DESCRIPTION
    };
    private static final String[] ACTVINSTANCES_PROJECTION = new String[] {
            EighthContract.ActvInstances._ID,
            EighthContract.ActvInstances.KEY_ACTV_ID,
            EighthContract.ActvInstances.KEY_BLOCK_ID,
            EighthContract.ActvInstances.KEY_COMMENT,
            EighthContract.ActvInstances.KEY_ROOMS_STR,
            EighthContract.ActvInstances.KEY_FLAGS,
            EighthContract.ActvInstances.KEY_MEMBER_COUNT,
            EighthContract.ActvInstances.KEY_CAPACITY
    };

    private static final SyncUtils.MergeInterface<EighthBlock, Integer> BLOCKS_MERGE_INTERFACE =
            new SyncUtils.MergeInterface<EighthBlock, Integer>() {
                @Override
                public ContentValues toContentValues(EighthBlock item) {
                    return EighthContract.Blocks.toContentValues(item);
                }
                @Override
                public EighthBlock fromContentValues(ContentValues values) {
                    return EighthContract.Blocks.fromContentValues(values);
                }
                @Override
                public Integer getId(EighthBlock item) {
                    return item.blockId;
                }
                @Override
                public Uri buildContentUri(Integer id) {
                    return EighthContract.Blocks.buildBlockUri(id);
                }
            };

    private static final SyncUtils.MergeInterface<Pair<Integer, Integer>, Integer> SCHEDULE_MERGE_INTERFACE =
            new SyncUtils.MergeInterface<Pair<Integer, Integer>, Integer>() {
                @Override
                public ContentValues toContentValues(Pair<Integer, Integer> item) {
                    ContentValues values = new ContentValues();
                    values.put(EighthContract.Schedule.KEY_BLOCK_ID, item.first);
                    values.put(EighthContract.Schedule.KEY_ACTV_ID, item.second);
                    return values;
                }
                @Override
                public Pair<Integer, Integer> fromContentValues(ContentValues values) {
                    int blockId = values.getAsInteger(EighthContract.Schedule.KEY_BLOCK_ID);
                    int actvId = values.getAsInteger(EighthContract.Schedule.KEY_ACTV_ID);
                    return new Pair<>(blockId, actvId);
                }
                @Override
                public Integer getId(Pair<Integer, Integer> item) {
                    return item.first;
                }
                @Override
                public Uri buildContentUri(Integer id) {
                    return EighthContract.Schedule.buildScheduleUri(id);
                }
            };

    private static final SyncUtils.MergeInterface<EighthActv, Integer> ACTVS_MERGE_INTERFACE =
            new SyncUtils.MergeInterface<EighthActv, Integer>() {
                @Override
                public ContentValues toContentValues(EighthActv item) {
                    return EighthContract.Actvs.toContentValues(item);
                }
                @Override
                public EighthActv fromContentValues(ContentValues values) {
                    return EighthContract.Actvs.fromContentValues(values);
                }
                @Override
                public Integer getId(EighthActv item) {
                    return item.actvId;
                }
                @Override
                public Uri buildContentUri(Integer id) {
                    return EighthContract.Actvs.buildActvUri(id);
                }
            };

    private static final SyncUtils.MergeInterface<EighthActvInstance, Pair<Integer, Integer>> ACTVINSTANCES_MERGE_INTERFACE =
            new SyncUtils.MergeInterface<EighthActvInstance, Pair<Integer, Integer>>() {
                @Override
                public ContentValues toContentValues(EighthActvInstance item) {
                    return EighthContract.ActvInstances.toContentValues(item);
                }
                @Override
                public EighthActvInstance fromContentValues(ContentValues values) {
                    return EighthContract.ActvInstances.fromContentValues(values);
                }
                @Override
                public Pair<Integer, Integer> getId(EighthActvInstance item) {
                    return new Pair<>(item.blockId, item.actvId);
                }
                @Override
                public Uri buildContentUri(Pair<Integer, Integer> id) {
                    return EighthContract.ActvInstances.buildActvInstanceUri(id.first, id.second);
                }
            };

    public EighthSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(@NonNull Account account, @NonNull Bundle extras, String authority,
                              @NonNull ContentProviderClient provider, @NonNull SyncResult syncResult) {
        Log.d(TAG, "onPerformSync for account " + account);
        final AccountManager am = AccountManager.get(getContext());

        List<EighthBlockAndActv> schedule;
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


            // Part II. Get schedule (list of blocks)
            try {
                schedule = fetchSchedule(authToken);
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

        Log.v(TAG, "Got schedule (" + schedule.size() + " blocks)");

        // Part III. Update blocks in database
        ArrayList<EighthBlock> blockList = new ArrayList<>(schedule.size());
        ArrayList<Pair<Integer, Integer>> scheduleList = new ArrayList<>(schedule.size());
        ArrayList<EighthActv> actvList = new ArrayList<>(schedule.size());
        ArrayList<EighthActvInstance> actvInstanceList = new ArrayList<>(schedule.size());

        for (EighthBlockAndActv blockAndActv : schedule) {
            blockList.add(blockAndActv.block);
            scheduleList.add(new Pair<>(blockAndActv.block.blockId, blockAndActv.actv.actvId));
            actvList.add(blockAndActv.actv);
            actvInstanceList.add(blockAndActv.actvInstance);
        }

        try {
            updateEighthBlockData(blockList, provider, syncResult);
            updateActvData(actvList, provider, syncResult);
            updateSelectedActvData(scheduleList, provider, syncResult);
            updateActvInstanceData(actvInstanceList, provider, syncResult);
        } catch (RemoteException | SQLiteException | OperationApplicationException e) {
            Log.e(TAG, "Updating database failed", e);
            syncResult.databaseError = true;
            return;
        }

        Log.v(TAG, "Updated database; done syncing");
    }


    @NonNull
    private List<EighthBlockAndActv> fetchSchedule(String authToken)
            throws IodineAuthException, IOException, XmlPullParserException {

        InputStream stream = null;
        EighthListBlocksParser parser = null;

        try {
            stream = IodineApiHelper.getBlockList(getContext(), authToken);

            parser = new EighthListBlocksParser(getContext());
            parser.beginListBlocks(stream);

            return parser.parseBlocks();

        } finally {
            if (parser != null)
                parser.stopParse();
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException when closing stream", e);
            }
        }
    }

    // TODO: get rid of all this, it's ridiculous
    /**
     * This method was highly inspired by the one in BasicSyncAdapter.
     */
    private void updateEighthBlockData(@NonNull List<EighthBlock> blockList,
                                       @NonNull ContentProviderClient provider,
                                       @NonNull final SyncResult syncResult)
            throws RemoteException, OperationApplicationException, SQLiteException {

        Cursor queryCursor = provider.query(EighthContract.Blocks.CONTENT_URI,
                BLOCK_PROJECTION, null, null, null);
        assert queryCursor != null;

        ArrayList<ContentProviderOperation> batch = SyncUtils.createMergeBatch(
                EighthBlock.class.getSimpleName(),
                blockList,
                queryCursor,
                EighthContract.Blocks.CONTENT_URI,
                BLOCKS_MERGE_INTERFACE,
                syncResult.stats);

        ContentProviderResult[] results = provider.applyBatch(batch);
        Log.d(TAG, results.length + " operations performed.");
        // Log.d(TAG, "results: " + Arrays.toString(results));

        final ContentResolver resolver = getContext().getContentResolver();
        resolver.notifyChange(
                EighthContract.Blocks.CONTENT_URI,
                null, false); // IMPORTANT: Do not sync to network
    }

    /**
     * This method was highly inspired by the one in BasicSyncAdapter.
     */
    private void updateSelectedActvData(@NonNull List<Pair<Integer, Integer>> pairList,
                                        @NonNull ContentProviderClient provider,
                                        @NonNull final SyncResult syncResult)
            throws RemoteException, OperationApplicationException, SQLiteException {

        Cursor queryCursor = provider.query(EighthContract.Schedule.CONTENT_URI,
                SCHEDULE_PROJECTION, null, null, null);
        assert queryCursor != null;

        ArrayList<ContentProviderOperation> batch = SyncUtils.createMergeBatch(
                "Schedule",
                pairList,
                queryCursor,
                EighthContract.Schedule.CONTENT_URI,
                SCHEDULE_MERGE_INTERFACE,
                syncResult.stats);

        ContentProviderResult[] results = provider.applyBatch(batch);
        Log.d(TAG, results.length + " operations performed.");
        // Log.d(TAG, "results: " + Arrays.toString(results));

        final ContentResolver resolver = getContext().getContentResolver();
        resolver.notifyChange(
                EighthContract.Schedule.CONTENT_URI,
                null, false); // IMPORTANT: Do not sync to network
        resolver.notifyChange(
                EighthContract.Blocks.CONTENT_URI,
                null, false); // IMPORTANT: Do not sync to network
    }

    private void updateActvData(@NonNull List<EighthActv> actvList,
                                @NonNull ContentProviderClient provider,
                                @NonNull final SyncResult syncResult)
            throws RemoteException, OperationApplicationException, SQLiteException {

        Cursor queryCursor = provider.query(EighthContract.Actvs.CONTENT_URI,
                ACTVS_PROJECTION, null, null, null);
        assert queryCursor != null;

        ArrayList<ContentProviderOperation> batch = SyncUtils.createMergeBatch(
                "Actvs",
                actvList,
                queryCursor,
                EighthContract.Actvs.CONTENT_URI,
                ACTVS_MERGE_INTERFACE,
                syncResult.stats);

        ContentProviderResult[] results = provider.applyBatch(batch);
        Log.d(TAG, results.length + " operations performed.");
        // Log.d(TAG, "results: " + Arrays.toString(results));

        final ContentResolver resolver = getContext().getContentResolver();
        resolver.notifyChange(
                EighthContract.Actvs.CONTENT_URI,
                null, false); // IMPORTANT: Do not sync to network
        resolver.notifyChange(
                EighthContract.Blocks.CONTENT_URI,
                null, false); // IMPORTANT: Do not sync to network
    }

    private void updateActvInstanceData(@NonNull List<EighthActvInstance> actvInstanceList,
                                        @NonNull ContentProviderClient provider,
                                        @NonNull final SyncResult syncResult)
            throws RemoteException, OperationApplicationException, SQLiteException {

        Cursor queryCursor = provider.query(EighthContract.ActvInstances.CONTENT_URI,
                ACTVINSTANCES_PROJECTION, null, null, null);
        assert queryCursor != null;

        ArrayList<ContentProviderOperation> batch = SyncUtils.createMergeBatch(
                "ActvInstances",
                actvInstanceList,
                queryCursor,
                EighthContract.ActvInstances.CONTENT_URI,
                ACTVINSTANCES_MERGE_INTERFACE,
                syncResult.stats);

        ContentProviderResult[] results = provider.applyBatch(batch);
        Log.d(TAG, results.length + " operations performed.");
        // Log.d(TAG, "results: " + Arrays.toString(results));

        final ContentResolver resolver = getContext().getContentResolver();
        resolver.notifyChange(
                EighthContract.ActvInstances.CONTENT_URI,
                null, false); // IMPORTANT: Do not sync to network
        resolver.notifyChange(
                EighthContract.Blocks.CONTENT_URI,
                null, false); // IMPORTANT: Do not sync to network
    }

    /**
     * Helper method to have the sync adapter sync immediately.
     * @param account The account to sync immediately
     * @param manual Whether the sync was manually initiated
     */
    public static void syncImmediately(@NonNull Account account, boolean manual) {
        Log.d(TAG, "Immediate sync requested");
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, manual);
        ContentResolver.requestSync(account, EighthContract.CONTENT_AUTHORITY, bundle);
    }


    /**
     * Configures sync scheduling.
     * @param account The Iodine account to sync.
     */
    public static void configureSync(@NonNull Account account) {
        final String authority = EighthContract.CONTENT_AUTHORITY;

        // Configure syncing periodically
        Utils.configurePeriodicSync(account, authority, SYNC_INTERVAL, SYNC_FLEXTIME);

        // Enable automatic sync
        ContentResolver.setSyncAutomatically(account, authority, true);
    }
}
