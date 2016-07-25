package com.desklampstudios.thyroxine.ion;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.external.AccountAuthenticatorActivity;

/**
 * A login screen that offers login via username/password.
 */
public class IonAuthenticatorActivity extends AccountAuthenticatorActivity {
    private static final String TAG = IonAuthenticatorActivity.class.getSimpleName();
    public static final String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public static final String ARG_AUTH_TOKEN_TYPE = "AUTH_TOKEN_TYPE";
    public static final String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_NEW_ACCOUNT";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    //@Nullable
    //private UserLoginTask mAuthTask = null;

    // UI references
    private View mProgressView;
    private WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ion_authenticator);

        // use toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mProgressView = findViewById(R.id.login_progress);

        mWebView = (WebView) findViewById(R.id.login_webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new OAuthWebViewClient());

        // Ignore account / auth token types passed in intent
        // This activity handles only one combination
    }

    private void handleOnComplete(Uri uri) {
        String query = uri.getQuery();
        if (query == null) {
            query = "";
        }

        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
        sanitizer.setAllowUnregisteredParamaters(true);
        sanitizer.setUnregisteredParameterValueSanitizer(UrlQuerySanitizer.getAllButNulLegal());
        sanitizer.parseQuery(query);

        String code = sanitizer.getValue("code");
        String state = sanitizer.getValue("state");
        String error = sanitizer.getValue("error");

        if (code == null || state == null) {
            Toast.makeText(this, "Invalid query string: " + query, Toast.LENGTH_LONG).show();
            Log.e(TAG, String.format("Invalid query string: %s (%s, %s, %s)", query, code, state, error));
            setResult(Activity.RESULT_CANCELED);
        }
        else {
            Intent data = new Intent();
            data.putExtra("code", code);
            data.putExtra("state", state);
            data.putExtra("error", error);
            setResult(Activity.RESULT_OK, data);
        }

        finish();
    }

    private void finishLogin(@NonNull Intent intent) {
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(AccountManager.KEY_PASSWORD);
        String authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);

        final AccountManager am = AccountManager.get(IonAuthenticatorActivity.this);
        final Account account = new Account(accountName, IonAuthenticator.ACCOUNT_TYPE);

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            // Create the account on the device
            am.addAccountExplicitly(account, accountPassword, null);
        } else {
            // Change account password
            am.setPassword(account, accountPassword);
        }

        // get things started
        IonAuthUtils.onAccountCreated(account);

        // Update the auth token (to prevent an extra round-trip)
        am.setAuthToken(account, IonAuthenticator.ION_OAUTH2_TOKEN, authToken);

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

            mWebView.setVisibility(show ? View.GONE : View.VISIBLE);
            mWebView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mWebView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mWebView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private class OAuthWebViewClient extends WebViewClient {
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            final Uri uri = Uri.parse(url);
            return handleUri(uri);
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            final Uri uri = request.getUrl();
            return handleUri(uri);
        }

        private boolean handleUri(Uri uri) {
            Uri redirectUri = Uri.parse(IonApiHelper.REDIRECT_URL);
            if (uri.getHost().equals(redirectUri.getHost()) &&
                    uri.getPath().equals(redirectUri.getPath())) {
                handleOnComplete(uri);
                return true;
            }
            return false;
        }
    }
}
