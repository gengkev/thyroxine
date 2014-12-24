package com.desklampstudios.thyroxine.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.news.NewsSyncAdapter;

/*
 * Implement AbstractAccountAuthenticator and stub out all
 * of its methods
 */
public class StubAuthenticator extends AbstractAccountAuthenticator {
    private static final String TAG = StubAuthenticator.class.getSimpleName();

    private final Context mContext;
    private final Handler handler = new Handler();

    public StubAuthenticator(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures,
                             Bundle options) throws NetworkErrorException {
        Log.w(TAG, "addAccount: cannot create stub account");

        final String message = mContext.getString(R.string.stub_account_cannot_create);
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

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                                     Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
                                    String authTokenType,
                                    Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
                              String[] features) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the fake account for the SyncAdapter, or create one if necessary.
     * @param context The context used to access the account service
     * @return a fake account
     */
    public static Account getStubAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager am = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(context.getString(R.string.stub_sync_fake_account_name),
                context.getString(R.string.stub_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (am.getPassword(newAccount) == null) {

            // Add the account and account type, no password or user data
            if (!am.addAccountExplicitly(newAccount, "", null)) {
                Log.w(TAG, "addAccountExplicitly returned false");
                return null;
            }

            Log.d(TAG, "successfully added stub account");
        }
        return newAccount;
    }
}
