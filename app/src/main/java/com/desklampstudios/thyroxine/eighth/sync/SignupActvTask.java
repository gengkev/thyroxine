package com.desklampstudios.thyroxine.eighth.sync;

import android.accounts.Account;
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

import com.desklampstudios.thyroxine.iodine.IodineAuthException;
import com.desklampstudios.thyroxine.iodine.IodineAuthUtils;
import com.desklampstudios.thyroxine.eighth.io.EighthSignupException;
import com.desklampstudios.thyroxine.eighth.io.IodineEighthApi;
import com.desklampstudios.thyroxine.eighth.provider.EighthContract;
import com.desklampstudios.thyroxine.eighth.model.EighthActv;
import com.desklampstudios.thyroxine.eighth.model.EighthActvInstance;

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

        try {
            IodineAuthUtils.withAuthTokenBlocking(mActivity, mAccount, new IodineAuthUtils.AuthTokenOperation<Void>() {
                @Override
                public Void performOperation(String authToken)
                        throws IodineAuthException, IOException, XmlPullParserException, EighthSignupException {
                    IodineEighthApi.doSignup(mActivity, blockId, actvId, authToken);
                    return null;
                }
            });
        } catch (EighthSignupException e) {
            Log.d(TAG, "Signup failed", e);
            mException = e;
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Connection error", e);
            mException = e;
            return null;
        } catch (OperationCanceledException e) {
            Log.e(TAG, "Operation canceled", e);
            mException = e;
            return null;
        } catch (AuthenticatorException e) {
            Log.e(TAG, "Authenticator error", e);
            mException = e;
            return null;
        } catch (IodineAuthException e) {
            Log.e(TAG, "Auth error", e);
            mException = e;
            return null;
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XML error", e);
            mException = e;
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Update database
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
