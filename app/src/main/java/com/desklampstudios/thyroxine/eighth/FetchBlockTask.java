package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

class FetchBlockTask extends AsyncTask<Integer, Object, EighthBlock> {
    private static final String TAG = FetchBlockTask.class.getSimpleName();
    private final Activity mActivity;
    private final Account mAccount;
    private Exception exception = null;

    public FetchBlockTask(Activity activity, Account account) {
        this.mActivity = activity;
        this.mAccount = account;
    }

    @Override
    protected EighthBlock doInBackground(Integer... params) {
        final AccountManager am = AccountManager.get(mActivity);
        AccountManagerFuture<Bundle> future = am.getAuthToken(mAccount,
                IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN, Bundle.EMPTY, mActivity, null, null);

        String authToken;
        try {
            Bundle bundle = future.getResult();
            authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            Log.v(TAG, "Got bundle: " + bundle);
        } catch (IOException e) {
            Log.e(TAG, "Connection error: "+ e.toString());
            exception = e;
            return null;
        } catch (OperationCanceledException | AuthenticatorException e) {
            Log.e(TAG, "Authentication error: " + e.toString());
            exception = e;
            return null;
        }

        final int blockId = params[0];
        InputStream stream = null;
        IodineEighthParser parser = null;
        Pair<EighthBlock, Integer> blockPair;

        try {
            stream = IodineApiHelper.getBlock(blockId, authToken);

            parser = new IodineEighthParser(mActivity);
            blockPair = parser.beginGetBlock(stream);

            Pair<EighthActv, EighthActvInstance> pair;
            while (!isCancelled()) {
                pair = parser.nextActivity();

                if (pair == null)
                    break;

                publishProgress(pair.first, pair.second);
            }

        } catch (IodineAuthException.NotLoggedInException e) {
            Log.d(TAG, "Not logged in, oh no!", e);
            // TODO: invalidate auth token
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
                if (stream != null)
                    stream.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException when closing stream: " + e);
            }
        }

        return blockPair.first;
    }

    @Override
    protected void onProgressUpdate(Object... args) {
        final ContentResolver resolver = mActivity.getContentResolver();

        final EighthActv actv = (EighthActv) args[0];
        final EighthActvInstance actvInstance = (EighthActvInstance) args[1];

        updateEighthActv(actv, resolver);
        updateEighthActvInstance(actvInstance, resolver);
    }

    // TODO: do in bulk, and not on the UI thread!
    private void updateEighthActv(EighthActv actv, ContentResolver resolver) {
        final ContentValues newValues = EighthContract.Actvs.toContentValues(actv);

        // actually, let's just insert it. derp
        Uri uri = resolver.insert(EighthContract.Actvs.CONTENT_URI, newValues);
        //Log.v(TAG, "Inserted EighthActv with uri: " + uri);
    }

    private void updateEighthActvInstance(EighthActvInstance actvInstance, ContentResolver resolver) {
        final ContentValues newValues = EighthContract.ActvInstances.toContentValues(actvInstance);

        // actually, let's just insert it. derp
        Uri uri = resolver.insert(EighthContract.ActvInstances.CONTENT_URI, newValues);
        //Log.v(TAG, "Inserted EighthActvInstance with uri: " + uri);
    }

    @Override
    protected void onPostExecute(EighthBlock block) {
        if (exception != null) {
            Log.e(TAG, "GetBlockTask error: " + exception);
            return;
        }

        Log.i(TAG, "Got activities");
    }

    @Override
    protected void onCancelled() {
    }
}
