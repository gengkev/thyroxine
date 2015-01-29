package com.desklampstudios.thyroxine.news;

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
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SyncStatusObserver {
    private static final String TAG = NewsFragment.class.getSimpleName();
    public static final String EXTRA_NEWS_ID = "com.desklampstudios.thyroxine.news.KEY_NEWS_ID";
    private static final int NEWS_LOADER = 0;

    private static final String[] NEWS_PROJECTION = new String[] {
            NewsContract.NewsEntries._ID,
            NewsContract.NewsEntries.KEY_TITLE,
            NewsContract.NewsEntries.KEY_PUBLISHED,
            NewsContract.NewsEntries.KEY_NEWS_ID,
            //NewsContract.NewsEntries.KEY_CONTENT,
            NewsContract.NewsEntries.KEY_CONTENT_SNIPPET
    };

    private NewsListAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Nullable private Object mSyncObserverHandle; // obtained in onResume
    private boolean mSyncActive = false;
    private boolean mSyncPending = false;

    public NewsFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NewsFragment.
     */
    @NonNull
    public static NewsFragment newInstance() {
        NewsFragment fragment = new NewsFragment();
        fragment.setArguments(Bundle.EMPTY);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create entries list
        mAdapter = new NewsListAdapter(getActivity(), null, 0);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_news, container, false);

        ListView listView = (ListView) view.findViewById(R.id.news_listview);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                Cursor cursor = mAdapter.getCursor();

                if (cursor != null && cursor.moveToPosition(pos)) {
                    int newsId = cursor.getInt(
                            cursor.getColumnIndex(NewsContract.NewsEntries.KEY_NEWS_ID));
                    openNewsDetailActivity(newsId);
                }
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.primary);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                retrieveNews();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // setHasOptionsMenu(true);
        checkLoginState();
        getLoaderManager().initLoader(NEWS_LOADER, null, this);
    }

    /*
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.news, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh_news:
                retrieveNews();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

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
                account, NewsContract.CONTENT_AUTHORITY);
        mSyncPending = ContentResolver.isSyncPending(
                account, NewsContract.CONTENT_AUTHORITY);

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
                account, NewsContract.CONTENT_AUTHORITY);
        final boolean syncPending = ContentResolver.isSyncPending(
                account, NewsContract.CONTENT_AUTHORITY);

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
    private void openNewsDetailActivity(int newsId) {
        // Toast.makeText(getApplicationContext(), "Entry: " + entry, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(getActivity(), NewsDetailActivity.class);
        intent.putExtra(EXTRA_NEWS_ID, newsId);
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

    private void retrieveNews() {
        Account account = checkLoginState();
        if (account != null) {
            // Request immediate sync
            NewsSyncAdapter.syncImmediately(account, true);
        }
    }

    @Nullable
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case NEWS_LOADER:
                return new CursorLoader(
                        getActivity(),
                        NewsContract.NewsEntries.CONTENT_URI,
                        NEWS_PROJECTION,
                        null, // selection
                        null, // selectionArgs
                        NewsContract.NewsEntries.DEFAULT_SORT // orderBy
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }
}
