package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

class FetchScheduleTask extends AsyncTask<Account, Object, Void> {
    private static final String TAG = FetchScheduleTask.class.getSimpleName();
    private final Activity mActivity;
    @Nullable private Exception exception = null;

    public FetchScheduleTask(Activity activity) {
        mActivity = activity;
    }

    @Nullable
    @Override
    protected Void doInBackground(Account... params) {
        final Account account = params[0];
        final AccountManager am = AccountManager.get(mActivity);
        AccountManagerFuture<Bundle> future = am.getAuthToken(account,
                IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN, Bundle.EMPTY, mActivity, null, null);

        String authToken;
        try {
            Bundle bundle = future.getResult();
            authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            Log.v(TAG, "Got bundle: " + bundle);
        } catch (IOException e) {
            Log.e(TAG, "Connection error: " + e.toString());
            exception = e;
            return null;
        } catch (OperationCanceledException | AuthenticatorException e) {
            Log.e(TAG, "Authentication error: " + e.toString());
            exception = e;
            return null;
        }

        InputStream stream = null;
        EighthListBlocksParser parser = null;

        try {
            stream = IodineApiHelper.getBlockList(authToken);

            parser = new EighthListBlocksParser(mActivity);
            parser.beginListBlocks(stream);

            Pair<EighthBlock, Integer> pair;
            while (!isCancelled()) {
                pair = parser.nextBlock();

                if (pair == null)
                    break;

                publishProgress(pair.first, pair.second);
            }

        } catch (IodineAuthException.NotLoggedInException e) {
            Log.d(TAG, "Not logged in, invalidating auth token", e);
            am.invalidateAuthToken(account.type, authToken);
            exception = e;
            return null;
        } catch (IOException | IodineAuthException e) {
            Log.e(TAG, "Connection error: " + e.toString());
            exception = e;
            return null;
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XML error: " + e.toString());
            exception = e;
            return null;
        } finally {
            if (parser != null)
                parser.stopParse();
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException when closing stream: " + e);
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Object... args) {
        final ContentResolver resolver = mActivity.getContentResolver();

        final EighthBlock block = (EighthBlock) args[0];
        final int curActvId = (Integer) args[1];

        updateEighthBlock(block, resolver);
        updateSelectedActv(block.blockId, curActvId, resolver);
    }

    private void updateEighthBlock(@NonNull EighthBlock block, @NonNull ContentResolver resolver) {
        final ContentValues newValues = EighthContract.Blocks.toContentValues(block);

        // test if record exists
        Cursor c = resolver.query(
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
                resolver.update(
                        EighthContract.Blocks.buildBlockUri(block.blockId),
                        newValues, null, null);

                Log.d(TAG, "EighthBlock updated: " + block);
                Log.d(TAG, "Old EighthBlock: " + oldBlock);
            }
        } else { // must insert
            Uri uri = resolver.insert(EighthContract.Blocks.CONTENT_URI, newValues);
            Log.v(TAG, "Inserted new EighthBlock with uri: " + uri);
        }

        c.close();
    }

    private void updateSelectedActv(int blockId, int actvId, @NonNull ContentResolver resolver) {
        final ContentValues newValues = new ContentValues();
        newValues.put(EighthContract.Schedule.KEY_BLOCK_ID, blockId);
        newValues.put(EighthContract.Schedule.KEY_ACTV_ID, actvId);

        // test if record exists
        Cursor c = resolver.query(
                EighthContract.Schedule.buildScheduleUri(blockId),
                null, // projection
                null, null, null);

        if (c.moveToFirst()) { // already exists
            ContentValues oldValues = Utils.cursorRowToContentValues(c);
            int oldActvId = oldValues.getAsInteger(EighthContract.Schedule.KEY_ACTV_ID);

            // compare old value to new value
            if (actvId != oldActvId) {
                resolver.update(
                        EighthContract.Schedule.buildScheduleUri(blockId),
                        newValues, null, null);

                Log.d(TAG, "current activity updated: " + oldValues + " -> " + newValues);
            }
        } else {
            Uri uri = resolver.insert(EighthContract.Schedule.CONTENT_URI, newValues);
            Log.v(TAG, "current activity inserted: " + newValues + ", uri: " + uri);
        }

        c.close();
    }

    @Override
    protected void onPostExecute(Void param) {
        if (exception != null) {
            Log.e(TAG, "Error loading blocks", exception);
            return;
        }

        Log.i(TAG, "Got blocks");
    }
}
