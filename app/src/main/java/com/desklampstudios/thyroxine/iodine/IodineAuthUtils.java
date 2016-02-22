package com.desklampstudios.thyroxine.iodine;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.desklampstudios.thyroxine.BuildConfig;
import com.desklampstudios.thyroxine.MainActivity;
import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.eighth.sync.EighthSyncAdapter;
import com.desklampstudios.thyroxine.news.sync.NewsSyncAdapter;

import java.io.IOException;
import java.util.Arrays;

public class IodineAuthUtils {
    private static final String TAG = IodineAuthUtils.class.getSimpleName();

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

    /**
     * Attempts to log out by removing the current Iodine account.
     * This method does not change state on the server.
     * After finishing, this method will call finish() on the passed activity.
     * @param activity The activity used as a context.
     */
    public static void attemptLogout(@NonNull final Activity activity) {
        final AccountManager am = AccountManager.get(activity);
        final Account account = getIodineAccount(activity);
        final AccountManagerCallback<Boolean> callback = new AccountManagerCallback<Boolean>() {
            @Override
            public void run(AccountManagerFuture<Boolean> future) {
                try {
                    Boolean result = future.getResult();
                    if (!result) {
                        throw new Exception("Result is false");
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
                    Account iodineAccount = getIodineAccount(activity);
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
        Account iodineAccount = getIodineAccount(context);
        if (iodineAccount != null) {
            // Configure sync with Iodine account
            EighthSyncAdapter.configureSync(iodineAccount);
            NewsSyncAdapter.configureSync(iodineAccount);
        }
    }

    public static <T> T withAuthTokenBlocking(Context context, Account account, AuthTokenOperation<T> operation)
            throws Exception {
        final AccountManager am = AccountManager.get(context);

        int numRetries = 0;
        while (true) {
            // Get auth token
            String authToken = am.blockingGetAuthToken(account,
                    IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN, true);
            Log.v(TAG, "Got auth token: " + authToken);

            // Perform the operation
            try {
                return operation.performOperation(authToken);
            }
            catch (IodineAuthException.NotLoggedInException e) {
                Log.d(TAG, "Not logged in, invalidating auth token", e);
                am.invalidateAuthToken(account.type, authToken);

                // Automatically retry sync, but only once
                if (numRetries < 1) {
                    Log.d(TAG, "Retrying sync with new auth token.");
                    numRetries++;
                } else {
                    Log.e(TAG, "Retried to get auth token already, quitting.");
                    throw e;
                }
            }
        }
    }

    public interface AuthTokenOperation<T> {
        T performOperation(String authToken) throws Exception;
    }
}
