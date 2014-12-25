package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
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
import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class EighthSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = EighthSyncAdapter.class.getSimpleName();

    // Sync intervals
    private static final int SYNC_INTERVAL = 2 * 60 * 60; // 2 hours
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    public EighthSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "onPerformSync for account " + account);

        final AccountManager am = AccountManager.get(getContext());
        AccountManagerFuture<Bundle> future = am.getAuthToken(account,
                IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN, Bundle.EMPTY, true, null, null);

        String authToken;
        try {
            Bundle bundle = future.getResult();
            authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            Log.v(TAG, "Got bundle: " + bundle);
        } catch (IOException e) {
            Log.e(TAG, "Connection error: " + e.toString());
            syncResult.stats.numIoExceptions++;
            return;
        } catch (OperationCanceledException | AuthenticatorException e) {
            Log.e(TAG, "Authentication error: " + e.toString());
            syncResult.stats.numAuthExceptions++;
            return;
        }

        ArrayList<Pair<EighthBlock, Integer>> schedule;
        try {
            schedule = fetchSchedule(authToken, syncResult);
            Log.d(TAG, "got schedule (" + schedule.size() + " items)");
        }
        catch (IodineAuthException.NotLoggedInException e) {
            Log.d(TAG, "Not logged in, invalidating auth token", e);
            am.invalidateAuthToken(account.type, authToken);
            // TODO: retry
            return;
        }

        // TODO: make faster with transactions?
        // Update all blocks
        try {
            for (Pair<EighthBlock, Integer> pair : schedule) {
                updateEighthBlock(pair.first, provider);
                updateSelectedActv(pair.first.blockId, pair.second, provider);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
            syncResult.databaseError = true;
            return;
        } catch (SQLiteException e) {
            Log.e(TAG, "SQLiteException", e);
            syncResult.databaseError = true;
            return;
        }

        Log.d(TAG, "updated database; done syncing");
    }

    private ArrayList<Pair<EighthBlock, Integer>> fetchSchedule(String authToken, SyncResult syncResult)
            throws IodineAuthException.NotLoggedInException {

        InputStream stream = null;
        EighthListBlocksParser parser = null;

        try {
            stream = IodineApiHelper.getBlockList(authToken);

            parser = new EighthListBlocksParser(getContext());
            parser.beginListBlocks(stream);

            ArrayList<Pair<EighthBlock, Integer>> pairList = new ArrayList<>();

            Pair<EighthBlock, Integer> pair = parser.nextBlock();
            while (pair != null) {
                pairList.add(pair);
                pair = parser.nextBlock();
            }

            return pairList;
        }
        catch (IOException e) {
            Log.e(TAG, "Connection error", e);
            syncResult.stats.numIoExceptions++;
            return null;
        }
        catch (IodineAuthException e) {
            if (e instanceof IodineAuthException.NotLoggedInException) {
                throw (IodineAuthException.NotLoggedInException)e;
            }
            Log.e(TAG, "Iodine auth error", e);
            syncResult.stats.numAuthExceptions++;
            return null;
        }
        catch (XmlPullParserException e) {
            Log.e(TAG, "XML parsing error", e);
            syncResult.stats.numParseExceptions++;
            return null;
        }
        finally {
            if (parser != null)
                parser.stopParse();
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException when closing stream: " + e);
            }
        }
    }

    /**
     * Updates a block in the database.
     * @param block Block to update
     * @param provider Content provider client to access the database with
     * @throws RemoteException
     */
    private void updateEighthBlock(@NonNull EighthBlock block,
                                   @NonNull ContentProviderClient provider) throws RemoteException {

        final ContentValues newValues = EighthContract.Blocks.toContentValues(block);

        // test if record exists
        Cursor c = provider.query(
                EighthContract.Blocks.buildBlockUri(block.blockId),
                new String[] { // projection
                        EighthContract.Blocks._ID,
                        EighthContract.Blocks.KEY_BLOCK_ID,
                        EighthContract.Blocks.KEY_TYPE,
                        EighthContract.Blocks.KEY_DATE,
                        EighthContract.Blocks.KEY_LOCKED
                }, null, null, null);

        if (c.moveToFirst()) { // already exists
            Log.v(TAG, "EighthBlock with same blockId already exists (bid " + block.blockId + ")");

            ContentValues oldValues = Utils.cursorRowToContentValues(c);
            EighthBlock oldBlock = EighthContract.Blocks.fromContentValues(oldValues);

            // Compare old values to new values
            if (!block.equals(oldBlock)) {
                provider.update(
                        EighthContract.Blocks.buildBlockUri(block.blockId),
                        newValues, null, null);

                Log.d(TAG, "EighthBlock updated: " + block);
                Log.d(TAG, "Old EighthBlock: " + oldBlock);
            }
        } else { // must insert
            Uri uri = provider.insert(EighthContract.Blocks.CONTENT_URI, newValues);
            Log.v(TAG, "Inserted new EighthBlock with uri: " + uri);
        }

        c.close();
    }

    /**
     * Updates the user's selected activity for a given block in the database
     * @param blockId ID of block to update
     * @param actvId ID of selected activity for the block
     * @param provider Content provider client to access the database with
     * @throws RemoteException
     */
    private void updateSelectedActv(int blockId, int actvId,
                                    @NonNull ContentProviderClient provider) throws RemoteException {

        final ContentValues newValues = new ContentValues();
        newValues.put(EighthContract.Schedule.KEY_BLOCK_ID, blockId);
        newValues.put(EighthContract.Schedule.KEY_ACTV_ID, actvId);

        // test if record exists
        Cursor c = provider.query(
                EighthContract.Schedule.buildScheduleUri(blockId),
                null, // projection
                null, null, null);

        if (c.moveToFirst()) { // already exists
            ContentValues oldValues = Utils.cursorRowToContentValues(c);
            int oldActvId = oldValues.getAsInteger(EighthContract.Schedule.KEY_ACTV_ID);

            // compare old value to new value
            if (actvId != oldActvId) {
                provider.update(
                        EighthContract.Schedule.buildScheduleUri(blockId),
                        newValues, null, null);

                Log.d(TAG, "current activity updated: " + oldValues + " -> " + newValues);
            }
        } else {
            Uri uri = provider.insert(EighthContract.Schedule.CONTENT_URI, newValues);
            Log.v(TAG, "current activity inserted: " + newValues + ", uri: " + uri);
        }

        c.close();
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
