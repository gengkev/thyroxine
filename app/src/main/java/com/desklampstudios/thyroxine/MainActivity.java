package com.desklampstudios.thyroxine;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.desklampstudios.thyroxine.directory.DirectoryInfo;
import com.desklampstudios.thyroxine.directory.DirectoryInfoParser;
import com.desklampstudios.thyroxine.eighth.ScheduleFragment;
import com.desklampstudios.thyroxine.news.NewsFragment;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mDrawerNavigationView;
    private View mDrawerHeaderView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // use toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configure synchronization
        IodineAuthenticator.configureSync(this);

        // Navigation drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerNavigationView = (NavigationView) findViewById(R.id.drawer_navigation);
        mDrawerHeaderView = findViewById(R.id.drawer_header);

        // Create drawer toggle
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // listen for navigation changes
        mDrawerNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                if (item.getItemId() == R.id.navigation_item_sign_out) {
                    IodineAuthenticator.attemptLogout(MainActivity.this);
                    return false;
                }
                selectItem(item.getItemId());
                item.setChecked(true);
                mDrawerLayout.closeDrawer(mDrawerNavigationView);
                return true;
            }
        });

        // select drawer position
        int drawerPosition = R.id.navigation_item_news;
        //if (savedInstanceState != null) {
        //    drawerPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION, drawerPosition);
        //}
        selectItem(drawerPosition);

        // load profile
        Account account = IodineAuthenticator.getIodineAccount(this);
        if (account != null) {
            new ProfileLoader(this, account).execute((Void) null);
        }
    }

    private void selectItem(int id) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        Fragment fragment;
        String title;
        switch (id) {
            case R.id.navigation_item_news:
                title = getString(R.string.title_fragment_news);
                fragment = NewsFragment.newInstance();
                break;
            case R.id.navigation_item_eighth:
                title = getString(R.string.title_fragment_eighth);
                fragment = ScheduleFragment.newInstance();
                break;
            case R.id.navigation_item_bell_schedule:
                title = getString(R.string.title_fragment_bell_schedule);
                fragment = PlaceholderFragment.newInstance(title + " (placeholder)");
                break;
            case R.id.navigation_item_links:
                title = getString(R.string.title_fragment_links);
                fragment = PlaceholderFragment.newInstance(title + " (placeholder)");
                break;
            case R.id.navigation_item_settings:
                title = getString(R.string.action_settings);
                fragment = PlaceholderFragment.newInstance(title + " (placeholder)");
                break;
            default:
                throw new IllegalArgumentException();
        }

        // Insert the fragment by replacing any existing fragment
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();

        // Update title
        setTitle(title);
    }

    private void setProfile(String name, String email, Drawable icon) {
        TextView nameView = (TextView) mDrawerHeaderView.findViewById(R.id.drawer_header_name);
        nameView.setText(name);

        TextView emailView = (TextView) mDrawerHeaderView.findViewById(R.id.drawer_header_email);
        emailView.setText(email);

        // TODO: show icon
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Allow the drawer toggle to attempt to handle it
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // save drawer state
        // outState.putInt(STATE_SELECTED_POSITION, mDrawer.getCurrentSelection());
    }

    @Override
    public void onBackPressed() {
        // Close drawer on back button press
        if (mDrawerLayout.isDrawerOpen(mDrawerNavigationView)) {
            mDrawerLayout.closeDrawer(mDrawerNavigationView);
            return;
        }
        super.onBackPressed();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_TITLE = "title";

        public PlaceholderFragment() {
        }

        @NonNull
        public static PlaceholderFragment newInstance(String title) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putString(ARG_TITLE, title);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_placeholder, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);

            String title = getArguments().getString(ARG_TITLE);
            textView.setText(title);

            return rootView;
        }
    }

    public class ProfileLoader extends AsyncTask<Void, Void, Void> {
        @Nullable private Exception mException = null;

        private final Activity mActivity;
        private final Account mAccount;

        private DirectoryInfo info;
        private Bitmap icon;

        public ProfileLoader(Activity activity, Account account) {
            mActivity = activity;
            mAccount = account;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final AccountManager am = AccountManager.get(mActivity);

            boolean authTokenRetry = false;
            while (true) {
                String authToken;
                try {
                    authToken = am.blockingGetAuthToken(mAccount,
                            IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN, true);
                } catch (IOException e) {
                    Log.e(TAG, "Connection error", e);
                    mException = e;
                    return null;
                } catch (@NonNull OperationCanceledException | AuthenticatorException e) {
                    Log.e(TAG, "Authentication error", e);
                    mException = e;
                    return null;
                }
                Log.v(TAG, "Got auth token: " + authToken);

                try {
                    info = getDirectoryInfo(authToken);
                    icon = getUserIcon(String.valueOf(info.iodineUid), authToken);
                } catch (IodineAuthException.NotLoggedInException e) {
                    Log.d(TAG, "Not logged in, oh no!", e);
                    am.invalidateAuthToken(mAccount.type, authToken);

                    // Automatically retry, but only once
                    if (!authTokenRetry) {
                        authTokenRetry = true;
                        Log.d(TAG, "Retrying fetch with new auth token.");
                        continue;
                    } else {
                        Log.e(TAG, "Retried to get auth token already, quitting.");
                        return null;
                    }
                } catch (IOException | IodineAuthException e) {
                    Log.e(TAG, "Connection error", e);
                    mException = e;
                    return null;
                } catch (XmlPullParserException e) {
                    Log.e(TAG, "XML parsing error", e);
                    mException = e;
                    return null;
                }
                break;
            }

            return null;
        }

        private DirectoryInfo getDirectoryInfo(String authToken)
                throws XmlPullParserException, IOException, IodineAuthException {

            InputStream stream = null;
            DirectoryInfoParser parser = null;
            try {
                stream = IodineApiHelper.getDirectoryInfo(mActivity, "", authToken);

                parser = new DirectoryInfoParser(mActivity);
                parser.beginInfo(stream);

                return parser.parseDirectoryInfo();

            } finally {
                if (parser != null)
                    parser.stopParse();
                try {
                    if (stream != null)
                        stream.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException when closing stream", e);
                }
            }
        }

        private Bitmap getUserIcon(String uid, String authToken)
                throws XmlPullParserException, IOException, IodineAuthException{
            InputStream stream = null;
            try {
                stream = IodineApiHelper.getUserIcon(mActivity, uid, authToken);

                return BitmapFactory.decodeStream(stream);

            } finally {
                try {
                    if (stream != null)
                        stream.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException when closing stream", e);
                }
            }
        }

        @Override
        protected void onPostExecute(@Nullable Void result) {
            if (mException != null) {
                Log.e(TAG, "exception", mException);
                return;
            }

            Log.d(TAG, "Got profile result: " + info);
            Log.d(TAG, "Got bitmap icon: " + icon);

            Drawable drawable = new BitmapDrawable(mActivity.getResources(), icon);
            setProfile(info.name.getCommonName(), info.tjhsstId, drawable);
        }
    }
}
