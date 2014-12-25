package com.desklampstudios.thyroxine;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.desklampstudios.thyroxine.eighth.EighthSyncAdapter;
import com.desklampstudios.thyroxine.eighth.ScheduleFragment;
import com.desklampstudios.thyroxine.news.NewsFragment;
import com.desklampstudios.thyroxine.news.NewsSyncAdapter;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;
import com.desklampstudios.thyroxine.sync.StubAuthenticator;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private View mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;

    private int mDrawerSelectedPosition = 0;
    private String[] mNavTitles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // use toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // load savedInstanceState
        if (savedInstanceState != null) {
            mDrawerSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
        }

        // Configure synchronization
        Utils.configureSync(this);

        // Navigation Drawer
        mNavTitles = getResources().getStringArray(R.array.nav_titles);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawer = findViewById(R.id.left_drawer);
        mDrawerList = (ListView) findViewById(R.id.left_drawer_list);

        // create drawer toggle
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // create ArrayAdapter to display drawer items, add click listener
        mDrawerList.setAdapter(new ArrayAdapter<>(
                getSupportActionBar().getThemedContext(),
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                mNavTitles));

        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        // select an item in drawer
        selectItem(mDrawerSelectedPosition);
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        mDrawerSelectedPosition = position;

        Fragment fragment;
        switch (position) {
            case 0:
                fragment = NewsFragment.newInstance();
                break;
            case 1:
                fragment = ScheduleFragment.newInstance();
                break;
            default:
                fragment = PlaceholderFragment.newInstance(
                        mNavTitles[position] + " (placeholder)");
                break;
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mNavTitles[position]);
        mDrawerLayout.closeDrawer(mDrawer);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If the drawer toggle handles it, it will return true
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_login:
                IodineAuthenticator.addAccount(this);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save drawer state
        outState.putInt(STATE_SELECTED_POSITION, mDrawerSelectedPosition);
    }

    @Override
    public void onBackPressed() {
        // Close drawer on back button press
        if (mDrawerLayout.isDrawerOpen(mDrawer)) {
            mDrawerLayout.closeDrawer(mDrawer);
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

        public static PlaceholderFragment newInstance(String title) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putString(ARG_TITLE, title);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);

            String title = getArguments().getString(ARG_TITLE);
            textView.setText(title);

            return rootView;
        }
    }
}
