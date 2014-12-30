package com.desklampstudios.thyroxine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.external.AccountAuthenticatorActivity;

import java.util.regex.Pattern;

import static com.desklampstudios.thyroxine.IodineAuthException.InvalidPasswordException;
import static com.desklampstudios.thyroxine.IodineAuthException.InvalidUsernameException;

/**
 * A login screen that offers login via username/password.
 */
public class IodineAuthenticatorActivity extends AccountAuthenticatorActivity {
    private final static String TAG = IodineAuthenticatorActivity.class.getSimpleName();
    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TOKEN_TYPE = "AUTH_TOKEN_TYPE";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_NEW_ACCOUNT";
    private static final String PARAM_USER_PASS = "USER_PASS";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    @Nullable
    private UserLoginTask mAuthTask = null;

    // UI references
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iodine_authenticator);

        // use toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the login form
        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        // Ignore account / auth token types passed in intent
        // This activity handles only one combination
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        // Check for email suffix
        final String emailSuffix = getResources().getString(R.string.tjhsst_edu_suffix);
        if (username.endsWith(emailSuffix)) {
            username = username.substring(0, username.length() - emailSuffix.length());
            mUsernameView.setText(username);
            Toast.makeText(this, R.string.error_tjhsst_edu_suffix, Toast.LENGTH_SHORT).show();
        }

        // Check for a valid username
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            mUsernameView.requestFocus();
            return;
        } else if (!Pattern.matches("^[a-zA-Z0-9-_.]+$", username)) { // probably
            mUsernameView.setError(getString(R.string.error_invalid_username));
            mUsernameView.requestFocus();
            return;
        }

        // Check for a valid password
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            mPasswordView.requestFocus();
            return;
        }

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true);
        mAuthTask = new UserLoginTask(username, password);
        mAuthTask.execute((Void) null);

    }

    private void finishLogin(@NonNull Intent intent) {
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        String authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);

        final AccountManager am = AccountManager.get(IodineAuthenticatorActivity.this);
        final Account account = new Account(accountName, IodineAuthenticator.ACCOUNT_TYPE);

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            // Create the account on the device
            am.addAccountExplicitly(account, accountPassword, null);
        } else {
            // Change account password
            am.setPassword(account, accountPassword);
        }

        // Update the auth token (to prevent an extra round-trip)
        am.setAuthToken(account, IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN, authToken);

        // Set the result
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Intent> {
        private final String mUsername;
        private final String mPassword;

        private Exception mException;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Nullable
        @Override
        protected Intent doInBackground(Void... params) {
            String authToken;
            try {
                authToken = IodineApiHelper.attemptLogin(mUsername, mPassword, IodineAuthenticatorActivity.this);
                Log.d(TAG, "attemptLogin succeeded, authToken: " + authToken);
            } catch (Exception e) {
                mException = e;
                Log.w(TAG, "attemptLogin threw exception: " + e);
                return null;
            }

            final Intent res = new Intent();
            res.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
            res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, IodineAuthenticator.ACCOUNT_TYPE);
            res.putExtra(AccountManager.KEY_AUTHTOKEN, authToken);
            res.putExtra(PARAM_USER_PASS, mPassword);
            return res;
        }

        @Override
        protected void onPostExecute(@Nullable final Intent intent) {
            mAuthTask = null;
            showProgress(false);

            if (intent == null) {
                if (mException instanceof IodineAuthException) {
                    if (mException instanceof InvalidPasswordException) { // password incorrect
                        mPasswordView.setError(getString(R.string.error_incorrect_password));
                        mPasswordView.requestFocus();
                        return;
                    } else if (mException instanceof InvalidUsernameException) { // username incorrect
                        mUsernameView.setError(getString(R.string.error_incorrect_username));
                        mUsernameView.requestFocus();
                        return;
                    }

                    String message = getResources().getString(
                            R.string.iodine_auth_error, mException.toString());
                    Toast.makeText(IodineAuthenticatorActivity.this,
                            message, Toast.LENGTH_LONG).show();
                    return;
                } else {
                    String message = getResources().getString(
                            R.string.unexpected_error, mException.toString());
                    Toast.makeText(IodineAuthenticatorActivity.this,
                            message, Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // Success!
            finishLogin(intent);
            Log.d(TAG, "Closing IodineAuthenticatorActivity.");

            // Close the activity
            finish();
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}



