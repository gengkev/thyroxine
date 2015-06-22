package com.desklampstudios.thyroxine.eighth.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.desklampstudios.thyroxine.BuildConfig;
import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.eighth.provider.EighthContract;
import com.desklampstudios.thyroxine.eighth.model.EighthBlock;

import java.util.ArrayList;
import java.util.List;

public class DayActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = DayActivity.class.getSimpleName();
    public static final String ARG_DATE = "date";
    public static final String ARG_BLOCK_ID = BlockFragment.ARG_BLOCK_ID;

    private static final int BLOCKS_LOADER = 0;
    private static final String[] BLOCKS_LOADER_PROJECTION = new String[]{
            EighthContract.Blocks.KEY_BLOCK_ID,
            EighthContract.Blocks.KEY_TYPE,
            EighthContract.Blocks.KEY_DATE,
            EighthContract.Blocks.KEY_LOCKED
    };

    private String mDate;
    private int mBlockId = -1;

    private List<EighthBlock> mBlocks;

    private View mHeaderView;
    private TabLayout mSlidingTabLayout;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day);

        // use toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // enable Up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // unpack values from intent
        if (getIntent() != null) {
            mDate = getIntent().getStringExtra(ARG_DATE);
            mBlockId = getIntent().getIntExtra(ARG_BLOCK_ID, -1);
        }
        if (BuildConfig.DEBUG && (mDate == null || mBlockId == -1)) {
            throw new AssertionError();
        }

        // set title as date
        String dateStr = Utils.DateFormats.FULL_DATE.formatBasicDate(this, mDate);
        setTitle(dateStr);

        // get references
        mHeaderView = findViewById(R.id.header);
        ViewCompat.setElevation(mHeaderView, getResources().getDimension(R.dimen.toolbar_elevation));

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mSlidingTabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setTabMode(TabLayout.MODE_FIXED);
        mSlidingTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);

        // initialize loader
        getSupportLoaderManager().initLoader(BLOCKS_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                this,
                EighthContract.Blocks.CONTENT_URI,
                BLOCKS_LOADER_PROJECTION, // columns
                EighthContract.Blocks.KEY_DATE + " = ?", // selection
                new String[] {mDate}, // selectionArgs
                EighthContract.Blocks.KEY_TYPE + " ASC" // orderBy
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "Cursor error");
            Toast.makeText(this, R.string.error_database, Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, cursor + " " + cursor.getCount());

        // add all blocks to mBlocks
        mBlocks = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ContentValues values = Utils.cursorRowToContentValues(cursor);
            EighthBlock block = EighthContract.Blocks.fromContentValues(values);
            mBlocks.add(block);
            cursor.moveToNext();
        }

        // Set the ViewPager's PagerAdapter so that it can display items
        mViewPager.setAdapter(new DayPagerAdapter(getSupportFragmentManager()));

        // Give the SlidingTabLayout the ViewPager, AFTER the ViewPager's PagerAdapter is set.
        mSlidingTabLayout.setupWithViewPager(mViewPager);

        // try to set correct item
        for (int i = 0; i < mBlocks.size(); i++) {
            if (mBlocks.get(i).blockId == mBlockId) {
                Log.d(TAG, "setting current position to " + i + ", " + mBlocks.get(i));
                mViewPager.setCurrentItem(i);
                break;
            }
        }
        Log.d(TAG, "could not find correct position");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.i(TAG, "onLoaderReset called *shrugs*");
    }

    // FragmentStatePagerAdapter that loads fragments and stuff
    class DayPagerAdapter extends FragmentStatePagerAdapter {
        private final String TAG = DayPagerAdapter.class.getSimpleName();

        public DayPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        /**
         * Return the {@link android.support.v4.app.Fragment} to be displayed at {@code position}.
         */
        @Override
        public Fragment getItem(int position) {
            Log.d(TAG, "selected item at position " + position);
            EighthBlock block = mBlocks.get(position);
            return BlockFragment.newInstance(block.blockId);
        }

        @Override
        public int getCount() {
            return mBlocks.size();
        }

        /**
         * Return the title of the item at {@code position}. This is important as what this method
         * returns is what is displayed in the {@link TabLayout}.
         */
        @Override
        public CharSequence getPageTitle(int position) {
            EighthBlock block = mBlocks.get(position);
            return getString(R.string.block_title, block.type);
        }
    }
}
