package com.desklampstudios.thyroxine;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.desklampstudios.thyroxine.directory.DirectoryInfo;
import com.desklampstudios.thyroxine.directory.DirectoryInfoParser;
import com.desklampstudios.thyroxine.eighth.ScheduleFragment;
import com.desklampstudios.thyroxine.news.NewsFragment;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends ActionBarActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    private static final int ITEM_NEWS = 0;
    private static final int ITEM_EIGHTH = 1;
    private static final int ITEM_BELL_SCHEDULE = 2;
    private static final int ITEM_LINKS = 3;
    private static final int ITEM_SETTINGS = 4;
    private static final int ITEM_SIGN_OUT = 5;

    private AccountHeader mAccountHeader;
    private Drawer mDrawer;

    private IDrawerItem[] mDrawerItems = {
            new PrimaryDrawerItem()
                    .withName(R.string.title_fragment_news)
                    .withIdentifier(ITEM_NEWS),
            new PrimaryDrawerItem()
                    .withName(R.string.title_fragment_eighth)
                    .withIdentifier(ITEM_EIGHTH),
            new PrimaryDrawerItem()
                    .withName(R.string.title_fragment_bell_schedule)
                    .withIdentifier(ITEM_BELL_SCHEDULE),
            new PrimaryDrawerItem()
                    .withName(R.string.title_fragment_links)
                    .withIdentifier(ITEM_LINKS),
            new DividerDrawerItem(),
            new PrimaryDrawerItem()
                    .withName(R.string.action_settings)
                    .withIdentifier(ITEM_SETTINGS),
            new PrimaryDrawerItem()
                    .withName(R.string.action_sign_out_short)
                    .withIdentifier(ITEM_SIGN_OUT)
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // use toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configure synchronization
        IodineAuthenticator.configureSync(this);

        // TODO: credit NASA (http://hubblesite.org/gallery/album/entire/pr2012010c/)
        // Create account header
        mAccountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.md_blue_grey_200)
                .withProfiles(new ArrayList<IProfile>())
                .withSelectionListEnabled(false)
                .withProfileImagesClickable(false)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean isCurrent) {
                        Log.i(TAG, "changed profile: " + profile);
                        return false;
                    }
                })
                .build();

        // Create navigation drawer
        mDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .addDrawerItems(mDrawerItems)
                .withAccountHeader(mAccountHeader)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> parent, View view, int position,
                                            long id, IDrawerItem drawerItem) {
                        int identifier = drawerItem.getIdentifier();
                        Log.i(TAG, "got identifier " + identifier);
                        if (identifier == ITEM_SIGN_OUT) {
                            IodineAuthenticator.attemptLogout(MainActivity.this);
                            return false;
                        } else {
                            loadItem(identifier);
                            return false;
                        }
                    }
                })
                .build();

        // select drawer position
        int drawerPosition = 0;
        if (savedInstanceState != null) {
            drawerPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
        }
        mDrawer.setSelection(drawerPosition);

        // load profile
        Account account = IodineAuthenticator.getIodineAccount(this);
        if (account != null) {
            new ProfileLoader(this, account).execute((Void) null);
        }
    }

    public void loadItem(int identifier) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        //Fragment oldFragment = fragmentManager.findFragmentById(R.id.container);

        Fragment fragment;
        String title;
        switch (identifier) {
            case ITEM_NEWS:
                title = getString(R.string.title_fragment_news);
                fragment = NewsFragment.newInstance();
                break;
            case ITEM_EIGHTH:
                title = getString(R.string.title_fragment_eighth);
                fragment = ScheduleFragment.newInstance();
                break;
            case ITEM_BELL_SCHEDULE:
                title = getString(R.string.title_fragment_bell_schedule);
                fragment = PlaceholderFragment.newInstance(title + " (placeholder)");
                break;
            case ITEM_LINKS:
                title = getString(R.string.title_fragment_links);
                fragment = PlaceholderFragment.newInstance(title + " (placeholder)");
                break;
            case ITEM_SETTINGS:
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

    public void setProfile(String name, String email, Drawable icon) {
        ArrayList<IProfile> profiles = new ArrayList<>();
        profiles.add(new ProfileDrawerItem().withName(name).withEmail(email).withIcon(icon));
        mAccountHeader.setProfiles(profiles);
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                IodineAuthenticator.attemptLogout(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    */

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // save drawer state
        outState.putInt(STATE_SELECTED_POSITION, mDrawer.getCurrentSelection());
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
                    icon = getUserIcon(info.iodineUid + "", authToken);
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
