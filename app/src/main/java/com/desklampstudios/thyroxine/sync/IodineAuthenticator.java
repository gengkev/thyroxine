package com.desklampstudios.thyroxine.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.NetworkErrorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.desklampstudios.thyroxine.BuildConfig;
import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.MainActivity;
import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.eighth.EighthSyncAdapter;
import com.desklampstudios.thyroxine.news.NewsSyncAdapter;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;

// Some code from:
// http://udinic.wordpress.com/2013/04/24/write-your-own-android-authenticator/
// http://stackoverflow.com/questions/20130074/android-sdk-promt-toast-message-in-account-settings

public class IodineAuthenticator extends AbstractAccountAuthenticator {
    private static final String TAG = IodineAuthenticator.class.getSimpleName();
    public static final String ACCOUNT_TYPE = "iodine.thyroxine.desklampstudios.com";
    public static final String IODINE_COOKIE_AUTH_TOKEN = "iodine-cookie";

    private final Context mContext;
    private final Handler handler = new Handler();

    public IodineAuthenticator(Context context) {
        super(context);
        this.mContext = context;
    }

    @NonNull
    private Intent createAuthenticateIntent(AccountAuthenticatorResponse response,
                                             String accountType, String authTokenType,
                                             boolean newAccount) {
        final Intent intent = new Intent(mContext, IodineAuthenticatorActivity.class);
        intent.putExtra(IodineAuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(IodineAuthenticatorActivity.ARG_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(IodineAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, newAccount);
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

    @NonNull
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, @NonNull Account account,
                               @NonNull String authTokenType, Bundle options) throws NetworkErrorException {
        if (!authTokenType.equals(IODINE_COOKIE_AUTH_TOKEN)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        // Check if an auth token already exists
        final AccountManager am = AccountManager.get(mContext);
        String authToken = am.peekAuthToken(account, authTokenType);
        String password = am.getPassword(account);

        // If no auth token, try to authenticate the user.
        if (authToken == null && password != null) {
            try {
                authToken = IodineApiHelper.attemptLogin(account.name, password, mContext);
            }
            catch (IodineAuthException e) {
                // Do nothing. Username/password probably incorrect
                Log.e(TAG, "Getting auth token failed with IodineAuthException: " + e);
            }
            catch (IOException | XmlPullParserException e) {
                // Network error, probably
                Log.e(TAG, "Getting auth token failed with network error: " + e);
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
        if (IODINE_COOKIE_AUTH_TOKEN.equals(authTokenType)) {
            return mContext.getString(R.string.iodine_cookie_auth_token_label);
        }
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                                     Bundle options) throws NetworkErrorException {
        return null;
    }

    @NonNull
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
                              String[] features) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the first Iodine account stored in the account manager, or null if not logged in.
     *
     * <p>(Note that not logged in means that no account exists on the device -- not the same as
     * IodineAuthError.NotLoggedInException, which may indicate expired credentials.)
     *
     * @param context The context used to access the account service
     * @return The Iodine account, or null
     */
    @Nullable
    public static Account getIodineAccount(@NonNull Context context) {
        // Get an instance of the Android account manager
        AccountManager am = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // List all accounts of this type
        Account[] accounts = am.getAccountsByType(IodineAuthenticator.ACCOUNT_TYPE);

        if (accounts.length == 0) {
            Log.w(TAG, "getIodineAccount: no accounts found (not logged in)");
            return null;
        } else if (accounts.length > 1) {
            Log.w(TAG, "getIodineAccount: more than one account: " + Arrays.toString(accounts));
        }
        return accounts[0];
    }

    public static void attemptLogout(@NonNull final Activity activity) {
        final AccountManager am = AccountManager.get(activity);
        final AccountManagerCallback<Boolean> callback = new AccountManagerCallback<Boolean>() {
            @Override
            public void run(AccountManagerFuture<Boolean> future) {
                try {
                    Boolean result = future.getResult();
                    if (result == null || result.equals(Boolean.FALSE)) {
                        throw new Exception("result was " + result);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error trying to remove account", e);
                    String message = activity.getString(R.string.error_removing_account, e.toString());
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i(TAG, "Successfully removed account.");
                Toast.makeText(activity, R.string.sign_out_success, Toast.LENGTH_LONG).show();
            }
        };

        final Account account = IodineAuthenticator.getIodineAccount(activity);
        am.removeAccount(account, callback, null);
        activity.finish();
    }

    /**
     * Attempts to add an Iodine account.
     * @param activity The activity used to open the login activity and as a context.
     */
    public static void attemptAddAccount(@NonNull final Activity activity) {
        final AccountManager am = AccountManager.get(activity);
        final AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                Bundle result;
                try {
                    result = future.getResult();
                } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                    Log.e(TAG, "Error trying to add account", e);
                    String message = activity.getString(R.string.error_adding_account, e.toString());
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                    return;
                }

                Account newAccount = new Account(
                        result.getString(AccountManager.KEY_ACCOUNT_NAME),
                        result.getString(AccountManager.KEY_ACCOUNT_TYPE));

                // Check that this new account is correct
                if (BuildConfig.DEBUG) {
                    Account iodineAccount = IodineAuthenticator.getIodineAccount(activity);
                    if (iodineAccount == null || !iodineAccount.equals(newAccount))
                        throw new AssertionError();
                }

                // Start MainActivity
                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivity(intent);
            }
        };
        am.addAccount(IodineAuthenticator.ACCOUNT_TYPE,
                IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN,
                null, null, activity, callback, null);
        activity.finish();
    }

    static void onAccountCreated(Account newAccount) {
        // Configure sync with Iodine account
        EighthSyncAdapter.configureSync(newAccount);
        NewsSyncAdapter.configureSync(newAccount);

        // Request initial sync
        EighthSyncAdapter.syncImmediately(newAccount, false);
        NewsSyncAdapter.syncImmediately(newAccount, false);
    }

    /**
     * Makes sure synchronization is set up properly, retrieving the Iodine account
     * and configuring periodic synchronization with the SyncAdapters.
     * @param context Context used to get accounts
     */
    public static void configureSync(@NonNull Context context) {
        // Find Iodine account (may not exist)
        Account iodineAccount = IodineAuthenticator.getIodineAccount(context);
        if (iodineAccount != null) {
            // Configure sync with Iodine account
            EighthSyncAdapter.configureSync(iodineAccount);
            NewsSyncAdapter.configureSync(iodineAccount);
        }
    }
}

