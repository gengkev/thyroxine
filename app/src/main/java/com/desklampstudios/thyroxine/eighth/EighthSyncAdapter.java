package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
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
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class EighthSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = EighthSyncAdapter.class.getSimpleName();
    private static final String KEY_AUTHTOKEN_RETRY = "authTokenRetry";

    // Sync intervals
    private static final int SYNC_INTERVAL = 2 * 60 * 60; // 2 hours
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    public static final String[] BLOCK_PROJECTION = new String[] {
            EighthContract.Blocks._ID,
            EighthContract.Blocks.KEY_BLOCK_ID,
            EighthContract.Blocks.KEY_TYPE,
            EighthContract.Blocks.KEY_DATE,
            EighthContract.Blocks.KEY_LOCKED
    };
    public static final String[] SCHEDULE_PROJECTION = new String[] {
            EighthContract.Schedule._ID,
            EighthContract.Schedule.KEY_BLOCK_ID,
            EighthContract.Schedule.KEY_ACTV_ID
    };

    public EighthSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "onPerformSync for account " + account);
        final AccountManager am = AccountManager.get(getContext());

        // Part I. Get auth token
        String authToken;
        try {
            authToken = am.blockingGetAuthToken(account,
                    IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN, true);
        } catch (IOException e) {
            Log.e(TAG, "Connection error: " + e.toString());
            syncResult.stats.numIoExceptions++;
            return;
        } catch (OperationCanceledException | AuthenticatorException e) {
            Log.e(TAG, "Authentication error: " + e.toString());
            syncResult.stats.numAuthExceptions++;
            return;
        }
        Log.v(TAG, "Got auth token: " + authToken);


        // Part II. Get schedule (list of blocks)
        List<Pair<EighthBlock, Integer>> schedule;
        try {
            schedule = fetchSchedule(authToken);
        } catch (IodineAuthException.NotLoggedInException e) {
            Log.d(TAG, "Not logged in, invalidating auth token", e);
            am.invalidateAuthToken(account.type, authToken);

            // Automatically retry sync, but only once
            if (!extras.getBoolean(KEY_AUTHTOKEN_RETRY, false)) {
                extras.putBoolean(KEY_AUTHTOKEN_RETRY, true);
                Log.d(TAG, "Retrying sync once, recursively. extras: " + extras);
                onPerformSync(account, extras, authority, provider, syncResult);
            } else {
                Log.d(TAG, "Retry token found; will not retry sync again.");
                syncResult.stats.numAuthExceptions++;
            }
            return;
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
        Log.v(TAG, "Got schedule (" + schedule.size() + " blocks)");


        // Part III. Update blocks in database
        ArrayList<EighthBlock> blockList = new ArrayList<>(schedule.size());
        ArrayList<Pair<Integer, Integer>> selectedActvList = new ArrayList<>(schedule.size());
        for (Pair<EighthBlock, Integer> pair : schedule) {
            blockList.add(pair.first);
            selectedActvList.add(new Pair<>(pair.first.blockId, pair.second));
        }

        try {
            updateEighthBlockData(blockList, provider, syncResult);
            updateSelectedActvData(selectedActvList, provider, syncResult);
        } catch (RemoteException | SQLiteException | OperationApplicationException e) {
            Log.e(TAG, "Updating database failed", e);
            syncResult.databaseError = true;
            return;
        }
        Log.v(TAG, "Updated database; done syncing");
    }


    private List<Pair<EighthBlock, Integer>> fetchSchedule(String authToken)
            throws IodineAuthException, IOException, XmlPullParserException {

        InputStream stream = null;
        EighthListBlocksParser parser = null;
        List<Pair<EighthBlock, Integer>> pairList = new ArrayList<>();
        Pair<EighthBlock, Integer> pair;

        try {
            stream = IodineApiHelper.getBlockList(authToken);

            parser = new EighthListBlocksParser(getContext());
            parser.beginListBlocks(stream);

            pair = parser.nextBlock();
            while (pair != null) {
                pairList.add(pair);
                pair = parser.nextBlock();
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

        return pairList;
    }

    /**
     * This method was highly inspired by the one in BasicSyncAdapter.
     */
    private void updateEighthBlockData(@NonNull List<EighthBlock> blockList,
                                       @NonNull ContentProviderClient provider,
                                       @NonNull final SyncResult syncResult)
            throws RemoteException, OperationApplicationException, SQLiteException {

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        HashMap<Integer, EighthBlock> entryMap = new HashMap<>();
        for (EighthBlock block : blockList) {
            entryMap.put(block.blockId, block);
        }

        Cursor c = provider.query(EighthContract.Blocks.CONTENT_URI,
                BLOCK_PROJECTION, null, null, null);
        assert c != null;

        while (c.moveToNext()) {
            syncResult.stats.numEntries++;
            final int blockId = c.getInt(c.getColumnIndex(EighthContract.Blocks.KEY_BLOCK_ID));
            final Uri blockUri = EighthContract.Blocks.buildBlockUri(blockId);

            // Get current database entries
            final ContentValues oldBlockValues = Utils.cursorRowToContentValues(c);
            final EighthBlock oldBlock = EighthContract.Blocks.fromContentValues(oldBlockValues);

            // Compare to new data
            EighthBlock newBlock = entryMap.get(blockId);
            if (newBlock != null) {
                // Item exists in the new data; remove to prevent insert later.
                entryMap.remove(blockId);

                // Check if an update is necessary
                if (!oldBlock.equals(newBlock)) {
                    syncResult.stats.numUpdates++;
                    Log.v(TAG, "blockId=" + blockId + ", scheduling block update");
                    ContentValues newValues = EighthContract.Blocks.toContentValues(newBlock);

                    batch.add(ContentProviderOperation.newUpdate(blockUri)
                            .withValues(newValues).build());
                } else {
                    Log.v(TAG, "blockId=" + blockId + ", no block update necessary.");
                }
            } else {
                // Item doesn't exist in the new data; remove it from the database.
                syncResult.stats.numDeletes++;
                Log.v(TAG, "blockId=" + blockId + ", scheduling block delete");
                batch.add(ContentProviderOperation.newDelete(blockUri).build());
            }
        }
        c.close();

        // Add new items (everything left in the map not found in the database)
        for (int blockId : entryMap.keySet()) {
            syncResult.stats.numInserts++;
            Log.v(TAG, "blockId=" + blockId + ", scheduling block insert");

            EighthBlock newBlock = entryMap.get(blockId);
            ContentValues newValues = EighthContract.Blocks.toContentValues(newBlock);

            batch.add(ContentProviderOperation.newInsert(EighthContract.Blocks.CONTENT_URI)
                    .withValues(newValues).build());
        }

        Log.d(TAG, "Merge solution ready; applying batch update");
        provider.applyBatch(batch);

        final ContentResolver resolver = getContext().getContentResolver();
        resolver.notifyChange(
                EighthContract.Blocks.CONTENT_URI,
                null, false); // IMPORTANT: Do not sync to network
    }

    /**
     * This method was highly inspired by the one in BasicSyncAdapter.
     */
    private void updateSelectedActvData(@NonNull List<Pair<Integer, Integer>> pairs,
                                        @NonNull ContentProviderClient provider,
                                        @NonNull final SyncResult syncResult)
            throws RemoteException, OperationApplicationException, SQLiteException {

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        HashMap<Integer, Pair<Integer, Integer>> entryMap = new HashMap<>();
        for (Pair<Integer, Integer> pair : pairs) {
            entryMap.put(pair.first, pair);
        }

        Cursor c = provider.query(EighthContract.Schedule.CONTENT_URI,
                SCHEDULE_PROJECTION, null, null, null);
        assert c != null;

        while (c.moveToNext()) {
            syncResult.stats.numEntries++;
            final int blockId = c.getInt(c.getColumnIndex(EighthContract.Schedule.KEY_BLOCK_ID));
            final Uri pairUri = EighthContract.Schedule.buildScheduleUri(blockId);

            // Get current database entries
            final int oldActvId = c.getInt(c.getColumnIndex(EighthContract.Schedule.KEY_ACTV_ID));

            // Compare to new data
            Pair<Integer, Integer> newPair = entryMap.get(blockId);
            if (newPair != null) {
                // Item exists in the new data; remove to prevent insert later.
                entryMap.remove(blockId);

                // Check if an update is necessary
                if (oldActvId != newPair.second) {
                    syncResult.stats.numUpdates++;
                    Log.v(TAG, "blockId=" + blockId + ", scheduling selectedActv update " +
                                    "(" + oldActvId + " != " + newPair.second + ")");
                    ContentValues newValues = new ContentValues();
                    newValues.put(EighthContract.Schedule.KEY_BLOCK_ID, blockId);
                    newValues.put(EighthContract.Schedule.KEY_ACTV_ID, newPair.second);
                    Log.v(TAG, "newValues=" + newValues);

                    batch.add(ContentProviderOperation.newUpdate(pairUri)
                            .withValues(newValues).build());
                } else {
                    Log.v(TAG, "blockId=" + blockId + ", no selectedActv update necessary.");
                }
            } else {
                // Item doesn't exist in the new data; remove it from the database.
                syncResult.stats.numDeletes++;
                Log.v(TAG, "blockId=" + blockId + ", scheduling selectedActv delete");
                batch.add(ContentProviderOperation.newDelete(pairUri).build());
            }
        }
        c.close();

        // Add new items (everything left in the map not found in the database)
        for (int blockId : entryMap.keySet()) {
            syncResult.stats.numInserts++;
            Log.v(TAG, "blockId=" + blockId + ", scheduling selectedActv insert");

            Pair<Integer, Integer> newPair = entryMap.get(blockId);
            ContentValues newValues = new ContentValues();
            newValues.put(EighthContract.Schedule.KEY_BLOCK_ID, blockId);
            newValues.put(EighthContract.Schedule.KEY_ACTV_ID, newPair.second);

            batch.add(ContentProviderOperation.newInsert(EighthContract.Schedule.CONTENT_URI)
                    .withValues(newValues).build());
        }

        Log.d(TAG, "Merge solution ready; applying batch update");
        ContentProviderResult[] results = provider.applyBatch(batch);
        Log.d(TAG, Arrays.toString(results));

        final ContentResolver resolver = getContext().getContentResolver();
        resolver.notifyChange(
                EighthContract.Schedule.CONTENT_URI,
                null, false); // IMPORTANT: Do not sync to network
        resolver.notifyChange(
                EighthContract.Blocks.CONTENT_URI,
                null, false); // IMPORTANT: Do not sync to network
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Log.d(TAG, "Immediate sync requested");
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(IodineAuthenticator.getIodineAccount(context),
                context.getString(R.string.eighth_content_authority), bundle);
    }


    /**
     * Configures sync scheduling. Called from MainActivity.
     * @param newAccount The stub account that was created.
     */
    public static void configureSync(Account newAccount) {
        final String authority = EighthContract.CONTENT_AUTHORITY;

        // Configure syncing periodically
        Utils.configurePeriodicSync(newAccount, authority, SYNC_INTERVAL, SYNC_FLEXTIME);

        // Configure syncing automatically
        ContentResolver.setSyncAutomatically(newAccount, authority, true);
    }
}
