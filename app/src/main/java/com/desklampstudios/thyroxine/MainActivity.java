package com.desklampstudios.thyroxine;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.desklampstudios.thyroxine.eighth.ScheduleFragment;
import com.desklampstudios.thyroxine.news.NewsFragment;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

public class MainActivity extends ActionBarActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private NavDrawerAdapter mDrawerAdapter;
    private int mDrawerSelectedPosition = 0;
    private String[] mNavTitles;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
        IodineAuthenticator.configureSync(this);

        // Navigation Drawer
        mNavTitles = getResources().getStringArray(R.array.nav_titles);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // create drawer toggle
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // create RecyclerView to display drawer items
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.left_drawer_list);

        mDrawerAdapter = new NavDrawerAdapter();
        recyclerView.setAdapter(mDrawerAdapter);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // select an item in drawer
        selectItem(mDrawerSelectedPosition, false);

        // set drawer padding
        // TODO: this is a hack
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int topPadding = getStatusBarHeight();
            RelativeLayout leftDrawer = (RelativeLayout) findViewById(R.id.left_drawer);
            leftDrawer.setPaddingRelative(0, topPadding, 0, 0);
        }
    }

    public int getStatusBarHeight() {
        int result = 25;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position, boolean force) {
        mDrawerSelectedPosition = position;
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment oldFragment = fragmentManager.findFragmentById(R.id.container);

        // Only swap fragment if necessary, or if one doesn't exist already
        if (force || oldFragment == null) {
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
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }

        // Highlight the selected item, update the title, and close the drawer
        mDrawerAdapter.setSelectedPosition(position);
        setTitle(mNavTitles[position]);
        mDrawerLayout.closeDrawer(Gravity.START);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // If the drawer toggle handles it, it will return true
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_login:
                IodineAuthenticator.attemptAddAccount(this);
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
        outState.putInt(STATE_SELECTED_POSITION, mDrawerSelectedPosition);
    }

    @Override
    public void onBackPressed() {
        // Close drawer on back button press
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
            mDrawerLayout.closeDrawer(Gravity.START);
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

    public class NavDrawerAdapter extends RecyclerView.Adapter<ViewHolder> {
        int mSelected = 0;

        public void setSelectedPosition(int selected) {
            int oldPosition = mSelected;
            mSelected = selected;
            notifyItemChanged(oldPosition);
            notifyItemChanged(selected);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.navdrawer_item, parent, false);

            final ViewHolder holder = new ViewHolder(v);
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = holder.getPosition();
                    selectItem(position, true);
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String title = mNavTitles[position];
            holder.mTextView.setText(title);
            holder.mView.setActivated(position == mSelected);
        }

        @Override
        public int getItemCount() {
            return mNavTitles.length;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @NonNull public final View mView;
        @NonNull public final TextView mTextView;
        public ViewHolder(@NonNull View v) {
            super(v);
            mView = v;
            mTextView = (TextView) v.findViewById(R.id.navdrawer_item_text);
        }
    }
}
