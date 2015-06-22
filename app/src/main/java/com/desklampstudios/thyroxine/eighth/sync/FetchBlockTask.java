package com.desklampstudios.thyroxine.eighth.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.desklampstudios.thyroxine.auth.IodineAuthException;
import com.desklampstudios.thyroxine.eighth.io.IodineEighthApi;
import com.desklampstudios.thyroxine.eighth.model.EighthBlockAndActv;
import com.desklampstudios.thyroxine.auth.IodineAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

public class FetchBlockTask extends AsyncTask<Integer, Void, List<EighthBlockAndActv>> {
    private static final String TAG = FetchBlockTask.class.getSimpleName();

    private final Activity mActivity;
    private final Account mAccount;
    private final ActvsResultListener mResultListener;

    @Nullable private Exception mException = null;

    public FetchBlockTask(Activity activity, Account account, ActvsResultListener listener) {
        this.mActivity = activity;
        this.mAccount = account;
        this.mResultListener = listener;
    }

    @Nullable
    @Override
    protected List<EighthBlockAndActv> doInBackground(Integer... params) {
        final int blockId = params[0];
        final AccountManager am = AccountManager.get(mActivity);

        List<EighthBlockAndActv> pairList;
        boolean authTokenRetry = false;
        while (true) {
            // Part I. Get auth token
            String authToken;
            try {
                authToken = am.blockingGetAuthToken(mAccount,
                        IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN, true);
            } catch (IOException e) {
                Log.e(TAG, "Connection error", e);
                mException = e;
                return null;
            } catch (OperationCanceledException | AuthenticatorException e) {
                Log.e(TAG, "Authentication error", e);
                mException = e;
                return null;
            }
            Log.v(TAG, "Got auth token: " + authToken);

            // Part II. Get block (list of activities)
            try {
                pairList = IodineEighthApi.fetchActivities(mActivity, blockId, authToken);
            } catch (IodineAuthException.NotLoggedInException e) {
                Log.d(TAG, "Not logged in, invalidating auth token", e);
                am.invalidateAuthToken(mAccount.type, authToken);
                mException = e;

                // Automatically retry, but only once
                if (!authTokenRetry) {
                    authTokenRetry = true;
                    Log.d(TAG, "Retrying fetch with new auth token.");
                    continue;
                } else {
                    Log.e(TAG, "Retried to get auth token already, quitting.");
                    return null;
                }
            } catch (IOException | IodineAuthException e) {
                Log.e(TAG, "Connection error", e);
                mException = e;
                return null;
            } catch (XmlPullParserException e) {
                Log.e(TAG, "XML error", e);
                mException = e;
                return null;
            }
            break;
        }

        return pairList;
    }

    @Override
    protected void onPostExecute(List<EighthBlockAndActv> pairList) {
        if (pairList == null) {
            mResultListener.onError(mException);
            return;
        }

        Log.i(TAG, "Got activities");
        mResultListener.onActvsResult(pairList);
    }

    public interface ActvsResultListener {
        void onActvsResult(List<EighthBlockAndActv> pairList);
        void onError(Exception exception);
    }
}
