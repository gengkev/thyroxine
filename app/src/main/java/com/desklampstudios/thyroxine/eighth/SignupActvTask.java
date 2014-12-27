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
import android.widget.Toast;

import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;


public class SignupActvTask extends AsyncTask<Integer, Void, Integer> {
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
    protected Integer doInBackground(Integer... params) {
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
            mException = e;
            return null;
        } catch (OperationCanceledException | AuthenticatorException e) {
            Log.e(TAG, "Authentication error: " + e.toString());
            mException = e;
            return null;
        }

        final int blockId = params[0];
        final int actvId = params[1];

        InputStream stream = null;
        EighthSignupActvParser parser = null;

        try {
            stream = IodineApiHelper.signupActivity(blockId, actvId, authToken);

            parser = new EighthSignupActvParser(mActivity);
            parser.beginSignupActivity(stream);
            return parser.nextResult();

        } catch (IodineAuthException.NotLoggedInException e) {
            Log.d(TAG, "Not logged in, oh no!", e);
            am.invalidateAuthToken(mAccount.type, authToken);
            // TODO: try again automatically
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
    protected void onPostExecute(Integer result) {
        if (mException != null || result == null) {
            mResultListener.onError(mException);
            return;
        }

        Log.i(TAG, "Got signup result: " + result);
        mResultListener.onSignupResult(result);
    }

    public interface SignupResultListener {
        public void onSignupResult(int result);
        public void onError(Exception e);
    }
}
