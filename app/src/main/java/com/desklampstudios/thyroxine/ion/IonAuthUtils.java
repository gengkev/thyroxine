package com.desklampstudios.thyroxine.ion;

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
import com.desklampstudios.thyroxine.Main2Activity;
import com.desklampstudios.thyroxine.R;

import java.io.IOException;
import java.util.Arrays;

public class IonAuthUtils {
    private static final String TAG = IonAuthUtils.class.getName();

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
    public static Account getIonAccount(@NonNull Context context) {
        // Get an instance of the Android account manager
        AccountManager am = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // List all accounts of this type
        Account[] accounts = am.getAccountsByType(IonAuthenticator.ACCOUNT_TYPE);

        if (accounts.length == 0) {
            Log.w(TAG, "getIonAccount: no accounts found (not logged in)");
            return null;
        } else if (accounts.length > 1) {
            Log.w(TAG, "getIonAccount: more than one account: " + Arrays.toString(accounts));
        }
        return accounts[0];
    }

    /**
     * Attempts to add an Ion account.
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
                    Account account = getIonAccount(activity);
                    if (account == null || !account.equals(newAccount))
                        throw new AssertionError();
                }

                // Start MainActivity
                // TODO: what else to do?
                Intent intent = new Intent(activity, Main2Activity.class);
                activity.startActivity(intent);
            }
        };
        am.addAccount(IonAuthenticator.ACCOUNT_TYPE,
                IonAuthenticator.ION_OAUTH2_TOKEN,
                null, null, activity, callback, null);
        activity.finish();
    }

    static void onAccountCreated(Account newAccount) {
        // TODO: implement this
    }
}
