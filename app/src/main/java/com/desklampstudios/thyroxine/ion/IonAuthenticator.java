package com.desklampstudios.thyroxine.ion;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.desklampstudios.thyroxine.R;

import java.io.IOException;
import java.util.Arrays;

public class IonAuthenticator extends AbstractAccountAuthenticator {
    private static final String TAG = IonAuthenticator.class.getSimpleName();
    public static final String ACCOUNT_TYPE = "ion.tjhsst.edu";
    public static final String ION_OAUTH2_TOKEN = "ion-oauth2";

    private final Context mContext;
    private final Handler handler = new Handler();

    public IonAuthenticator(Context context) {
        super(context);
        this.mContext = context;
    }

    @NonNull
    private Intent createAuthenticateIntent(AccountAuthenticatorResponse response,
                                            String accountType, String authTokenType,
                                            boolean newAccount) {
        final Intent intent = new Intent(mContext, IonAuthenticatorActivity.class);
        intent.putExtra(IonAuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(IonAuthenticatorActivity.ARG_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(IonAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, newAccount);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        return intent;
    }


    @NonNull
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures,
                             Bundle options) throws NetworkErrorException {
        // Make sure that only one account exists
        AccountManager am = AccountManager.get(mContext);
        Account[] accounts = am.getAccountsByType(accountType);
        if (accounts.length > 0) {
            Log.d(TAG, "addAccount: only one account. accounts: " + Arrays.toString(accounts));

            final String message = mContext.getString(R.string.iodine_account_only_one);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
                }
            });

            final Bundle bundle = new Bundle();
            bundle.putInt(AccountManager.KEY_ERROR_CODE, 1);
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, message);
            return bundle;
        }

        final Intent intent = createAuthenticateIntent(response, accountType, authTokenType, true);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }


    @NonNull
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, @NonNull Account account,
                                    String authTokenType,
                                    Bundle options) throws NetworkErrorException {
        final Intent intent = createAuthenticateIntent(response, account.type, authTokenType, false);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }


    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) throws NetworkErrorException {
        if (!authTokenType.equals(ION_OAUTH2_TOKEN)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        // Check if an auth token already exists
        final AccountManager am = AccountManager.get(mContext);
        String authToken = am.peekAuthToken(account, authTokenType);
        String refreshToken = am.getPassword(account);

        // If no auth token, try to authenticate the user.
        if (authToken == null && refreshToken != null) {
            try {
                authToken = IonApiHelper.getAuthToken(refreshToken);
            } catch (IOException e) {
                // Network error, probably
                Log.e(TAG, "Getting auth token failed with network error", e);
                throw new NetworkErrorException(e);
            }
        }

        // If still no auth token, we couldn't obtain one automatically. (No password?)
        // We need to re-authenticate the user by displaying the AuthenticatorActivity again.
        if (authToken == null) {
            return updateCredentials(response, account, authTokenType, options);
        }

        // We got an auth token! Return it.
        else {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (ION_OAUTH2_TOKEN.equals(authTokenType)) {
            return mContext.getString(R.string.ion_oauth2_auth_token_label);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                                     Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
                              String[] features) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }
}
