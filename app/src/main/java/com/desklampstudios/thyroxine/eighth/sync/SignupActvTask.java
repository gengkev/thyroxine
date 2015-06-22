package com.desklampstudios.thyroxine.eighth.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.desklampstudios.thyroxine.auth.IodineAuthException;
import com.desklampstudios.thyroxine.eighth.io.EighthSignupException;
import com.desklampstudios.thyroxine.eighth.io.IodineEighthApi;
import com.desklampstudios.thyroxine.eighth.provider.EighthContract;
import com.desklampstudios.thyroxine.eighth.model.EighthActv;
import com.desklampstudios.thyroxine.eighth.model.EighthActvInstance;
import com.desklampstudios.thyroxine.auth.IodineAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;


public class SignupActvTask extends AsyncTask<Object, Void, Void> {
    private static final String TAG = SignupActvTask.class.getSimpleName();

    private final Activity mActivity;
    private final Account mAccount;
    private final SignupResultListener mResultListener;

    @Nullable private Exception mException = null;

    public SignupActvTask(Activity activity, Account account, SignupResultListener listener) {
        mActivity = activity;
        mAccount = account;
        mResultListener = listener;
    }

    @Nullable
    @Override
    protected Void doInBackground(Object... params) {
        final int blockId = (Integer) params[0];
        final EighthActv actv = (EighthActv) params[1];
        final EighthActvInstance actvInstance = (EighthActvInstance) params[2];
        final int actvId = actv.actvId;
        final AccountManager am = AccountManager.get(mActivity);

        boolean authTokenRetry = false;
        while (true) {
            String authToken;
            try {
                authToken = am.blockingGetAuthToken(mAccount,
                        IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN, true);
            } catch (IOException e) {
                Log.e(TAG, "Connection error: " + e.toString());
                mException = e;
                return null;
            } catch (@NonNull OperationCanceledException | AuthenticatorException e) {
                Log.e(TAG, "Authentication error: " + e.toString());
                mException = e;
                return null;
            }
            Log.v(TAG, "Got auth token: " + authToken);

            try {
                IodineEighthApi.doSignup(mActivity, blockId, actvId, authToken);
            } catch (IodineAuthException.NotLoggedInException e) {
                Log.d(TAG, "Not logged in, oh no!", e);
                am.invalidateAuthToken(mAccount.type, authToken);

                // Automatically retry, but only once
                if (!authTokenRetry) {
                    authTokenRetry = true;
                    Log.d(TAG, "Retrying fetch with new auth token.");
                    continue;
                } else {
                    Log.e(TAG, "Retried to get auth token already, quitting.");
                    return null;
                }
            } catch (EighthSignupException e) {
                mException = e;
                return null;
            } catch (IOException | IodineAuthException e) {
                Log.e(TAG, "Connection error: " + e.toString());
                mException = e;
                return null;
            } catch (XmlPullParserException e) {
                Log.e(TAG, "XML error: " + e.toString());
                mException = e;
                return null;
            }
            break;
        }

        // Part III. Update database
        updateDatabase(blockId, actv, actvInstance);

        return null;
    }

    // TODO: make less hacky
    private void updateDatabase(int blockId, @NonNull EighthActv actv, @NonNull EighthActvInstance actvInstance) {
        // push to db, or something
        final ContentResolver resolver = mActivity.getContentResolver();

        // Update schedule
        ContentValues scheduleValues = new ContentValues();
        scheduleValues.put(EighthContract.Schedule.KEY_BLOCK_ID, blockId);
        scheduleValues.put(EighthContract.Schedule.KEY_ACTV_ID, actv.actvId);
        Uri scheduleUri = resolver.insert(EighthContract.Schedule.CONTENT_URI, scheduleValues);
        Log.d(TAG, "updated schedule: inserted with uri " + scheduleUri);

        // Update EighthActv
        ContentValues actvValues = EighthContract.Actvs.toContentValues(actv);
        Uri actvUri = resolver.insert(EighthContract.Actvs.CONTENT_URI, actvValues);
        Log.d(TAG, "updated actv: inserted with uri " + actvUri);

        // Update EighthActvInstance
        ContentValues actvInstanceValues = EighthContract.ActvInstances.toContentValues(actvInstance);
        Uri actvInstanceUri = resolver.insert(EighthContract.ActvInstances.CONTENT_URI, actvInstanceValues);
        Log.d(TAG, "updated actvInstance: inserted with uri " + actvInstanceUri);

        // notify changes
        resolver.notifyChange(EighthContract.Blocks.buildBlockUri(blockId), null, false);
    }

    @Override
    protected void onPostExecute(@Nullable Void result) {
        if (mException != null) {
            mResultListener.onError(mException);
            return;
        }

        Log.i(TAG, "Got signup result: " + result);
        mResultListener.onSignupResult();
    }

    public interface SignupResultListener {
        void onSignupResult();
        void onError(@NonNull Exception e);
    }
}
