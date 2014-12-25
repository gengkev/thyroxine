package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

class FetchBlockTask extends AsyncTask<Integer, Void, ArrayList<Pair<EighthActv, EighthActvInstance>>> {
    private static final String TAG = FetchBlockTask.class.getSimpleName();
    private final Activity mActivity;
    private final Account mAccount;
    private final ActvsResultListener mResultListener;
    @Nullable private Exception exception = null;

    public FetchBlockTask(Activity activity, Account account, ActvsResultListener listener) {
        this.mActivity = activity;
        this.mAccount = account;
        this.mResultListener = listener;
    }

    @Nullable
    @Override
    protected ArrayList<Pair<EighthActv, EighthActvInstance>> doInBackground(Integer... params) {
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
        EighthGetBlockParser parser = null;
        Pair<EighthBlock, Integer> blockPair;

        try {
            stream = IodineApiHelper.getBlock(blockId, authToken);

            parser = new EighthGetBlockParser(mActivity);
            blockPair = parser.beginGetBlock(stream);
            Log.d(TAG, "Block: " + blockPair.first);

            ArrayList<Pair<EighthActv, EighthActvInstance>> pairList = new ArrayList<>();
            Pair<EighthActv, EighthActvInstance> pair = parser.nextActivity();

            while (pair != null && !isCancelled()) {
                pairList.add(pair);
                pair = parser.nextActivity();
            }

            return pairList;

        } catch (IodineAuthException.NotLoggedInException e) {
            Log.d(TAG, "Not logged in, oh no!", e);
            am.invalidateAuthToken(mAccount.type, authToken);
            // TODO: try again automatically
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
    }

    @Override
    protected void onProgressUpdate(Void... args) {
    }

    /*
    @Override
    protected void onProgressUpdate(Object... args) {
        final ContentResolver resolver = mActivity.getContentResolver();

        final EighthActv actv = (EighthActv) args[0];
        final EighthActvInstance actvInstance = (EighthActvInstance) args[1];

        updateEighthActv(actv, resolver);
        updateEighthActvInstance(actvInstance, resolver);
    }

    // TODO: do in bulk, and not on the UI thread!
    private void updateEighthActv(@NonNull EighthActv actv, @NonNull ContentResolver resolver) {
        final ContentValues newValues = EighthContract.Actvs.toContentValues(actv);

        // actually, let's just insert it. derp
        Uri uri = resolver.insert(EighthContract.Actvs.CONTENT_URI, newValues);
        //Log.v(TAG, "Inserted EighthActv with uri: " + uri);
    }

    private void updateEighthActvInstance(@NonNull EighthActvInstance actvInstance,
                                          @NonNull ContentResolver resolver) {
        final ContentValues newValues = EighthContract.ActvInstances.toContentValues(actvInstance);

        // actually, let's just insert it. derp
        Uri uri = resolver.insert(EighthContract.ActvInstances.CONTENT_URI, newValues);
        //Log.v(TAG, "Inserted EighthActvInstance with uri: " + uri);
    }
    */

    @Override
    protected void onPostExecute(ArrayList<Pair<EighthActv, EighthActvInstance>> pairList) {
        if (exception != null) {
            if (exception instanceof IodineAuthException.NotLoggedInException) {
                Toast.makeText(mActivity, R.string.attempt_login_try_again, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mActivity, "An error occurred: " + exception, Toast.LENGTH_LONG).show();
            }
            return;
        }

        Log.i(TAG, "Got activities");
        mResultListener.onActvsResult(pairList);
    }

    public interface ActvsResultListener {
        public void onActvsResult(ArrayList<Pair<EighthActv, EighthActvInstance>> pairList);
    }
}
