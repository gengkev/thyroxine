package com.desklampstudios.thyroxine.eighth.sync;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.desklampstudios.thyroxine.iodine.IodineAuthException;
import com.desklampstudios.thyroxine.iodine.IodineAuthUtils;
import com.desklampstudios.thyroxine.eighth.io.IodineEighthApi;
import com.desklampstudios.thyroxine.eighth.model.EighthBlockAndActv;

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

        List<EighthBlockAndActv> pairList;
        try {
            pairList = IodineAuthUtils.withAuthTokenBlocking(mActivity, mAccount,
                    new IodineAuthUtils.AuthTokenOperation<List<EighthBlockAndActv>>() {
                        @Override
                        public List<EighthBlockAndActv> performOperation(String authToken) throws Exception {
                            return IodineEighthApi.fetchActivities(mActivity, blockId, authToken);
                        }
                    });
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
