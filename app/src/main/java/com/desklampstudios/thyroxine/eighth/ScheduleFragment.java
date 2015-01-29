package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.external.SimpleSectionedListAdapter;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ScheduleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ScheduleFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SyncStatusObserver {
    private static final String TAG = ScheduleFragment.class.getSimpleName();
    private static final int BLOCKS_LOADER = 0;

    private static final String[] BLOCKS_LOADER_PROJECTION = new String[] {
            EighthContract.Blocks._ID,
            EighthContract.Blocks.KEY_BLOCK_ID,
            EighthContract.Blocks.KEY_TYPE,
            EighthContract.Blocks.KEY_DATE,
            EighthContract.Blocks.KEY_LOCKED,
            EighthContract.Actvs.KEY_NAME,
            EighthContract.Actvs.KEY_FLAGS,
            EighthContract.Actvs.KEY_ACTV_ID,
            EighthContract.ActvInstances.KEY_FLAGS,
            EighthContract.ActvInstances.KEY_ROOMS_STR
    };

    private SimpleSectionedListAdapter mSectionedAdapter;
    private BlocksListAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Nullable private Object mSyncObserverHandle; // obtained in onResume
    private boolean mSyncActive = false;
    private boolean mSyncPending = false;

    private SimpleSectionedListAdapter.Section[] mSections;

    public ScheduleFragment() {
    }

    @NonNull
    public static ScheduleFragment newInstance() {
        ScheduleFragment fragment = new ScheduleFragment();
        fragment.setArguments(Bundle.EMPTY);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create list adapter
        mAdapter = new BlocksListAdapter(getActivity(), null, 0);

        mSectionedAdapter = new SimpleSectionedListAdapter(
                getActivity(),
                R.layout.schedule_header,
                R.id.schedule_header_textview,
                R.id.schedule_header_divider,
                mAdapter);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_eighth_schedule, container, false);

        ListView listView = (ListView) view.findViewById(R.id.blocks_list);
        listView.setAdapter(mSectionedAdapter);
        listView.setDividerHeight(0);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int sPos, long l) {
                int pos = mSectionedAdapter.sectionedPositionToPosition(sPos);
                Cursor cursor = mAdapter.getCursor();

                if (cursor != null && cursor.moveToPosition(pos)) {
                    int blockId = cursor.getInt(cursor.getColumnIndex(
                            EighthContract.Blocks.KEY_BLOCK_ID));
                    onBlockClick(blockId);
                } else {
                    Log.w(TAG, "cursor invalid: " + cursor);
                }
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.primary);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                retrieveSchedule();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        checkLoginState();
        getLoaderManager().initLoader(BLOCKS_LOADER, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Watch for sync state changes
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, this);

        // initialize mSyncActive, mSyncPending
        final Account account = IodineAuthenticator.getIodineAccount(getActivity());
        mSyncActive = ContentResolver.isSyncActive(
                account, EighthContract.CONTENT_AUTHORITY);
        mSyncPending = ContentResolver.isSyncPending(
                account, EighthContract.CONTENT_AUTHORITY);

        Log.v(TAG, "onResume: syncActive=" + mSyncActive + ", syncPending=" + mSyncPending);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop watching sync state changes
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    /**
     * Watches for sync changes, attached/detached in onResume/onPause.
     * When the app is syncing, the swipe refresh layout is set to refreshing.
     */
    @Override
    public void onStatusChanged(int which) {
        final Activity activity = getActivity();
        final Account account = IodineAuthenticator.getIodineAccount(activity);

        final boolean syncActive = ContentResolver.isSyncActive(
                account, EighthContract.CONTENT_AUTHORITY);
        final boolean syncPending = ContentResolver.isSyncPending(
                account, EighthContract.CONTENT_AUTHORITY);

        // no change
        if (syncActive == mSyncActive && syncPending == mSyncPending) {
            return;
        }
        mSyncActive = syncActive;
        mSyncPending = syncPending;

        Log.v(TAG, "onStatusChanged: syncActive=" + syncActive + ", syncPending=" + syncPending);

        // Run on the UI thread in order to update the UI
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (account == null) {
                    mSwipeRefreshLayout.setRefreshing(false);
                    return;
                }
                mSwipeRefreshLayout.setRefreshing(syncActive);
            }
        });
    }

    // Called when an item in the adapter is clicked
    private void onBlockClick(int blockId) {
        //Toast.makeText(getActivity(), "Block: " + block, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(getActivity(), BlockActivity.class);
        intent.putExtra(BlockFragment.ARG_BLOCK_ID, blockId);
        startActivity(intent);
    }

    private Account checkLoginState() {
        Account account = IodineAuthenticator.getIodineAccount(getActivity());
        if (account == null) { // not logged in
            Toast.makeText(getActivity(), R.string.error_not_logged_in, Toast.LENGTH_SHORT).show();
            IodineAuthenticator.attemptAddAccount(getActivity());
        }
        return account;
    }

    // Fetches schedule
    private void retrieveSchedule() {
        Account account = checkLoginState();
        if (account != null) {
            // Request immediate sync
            EighthSyncAdapter.syncImmediately(account, true);
        }
    }

    @NonNull
    private SimpleSectionedListAdapter.Section[] createSections(@NonNull Cursor cursor) {
        List<SimpleSectionedListAdapter.Section> sections = new ArrayList<>();
        String previousBlockDate = "";
        String blockDate;

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            blockDate = cursor.getString(cursor.getColumnIndex(EighthContract.Blocks.KEY_DATE));
            if (!previousBlockDate.equals(blockDate)) {
                String dateStr = Utils.DateFormats.FULL_DATE.formatBasicDate(getActivity(), blockDate);
                sections.add(new SimpleSectionedListAdapter.Section(
                        cursor.getPosition(), dateStr));
            }
            previousBlockDate = blockDate;
            cursor.moveToNext();
        }

        sections.add(new SimpleSectionedListAdapter.Section(
                cursor.getCount(), null));

        SimpleSectionedListAdapter.Section[] sections1 =
                new SimpleSectionedListAdapter.Section[sections.size()];
        return sections.toArray(sections1);
    }

    @Nullable
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case BLOCKS_LOADER: {
                return new CursorLoader(
                        getActivity(),
                        EighthContract.Blocks.CONTENT_URI,
                        BLOCKS_LOADER_PROJECTION, // columns
                        null, // selection
                        null, // selectionArgs
                        EighthContract.Blocks.DEFAULT_SORT // orderBy
                );
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mSections = createSections(cursor);
        mSectionedAdapter.setSections(mSections);

        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mSections = new SimpleSectionedListAdapter.Section[0];
        mSectionedAdapter.setSections(mSections);

        mAdapter.swapCursor(null);
    }
}
