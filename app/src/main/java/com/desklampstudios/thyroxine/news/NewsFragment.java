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
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.sync.StubAuthenticator;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SyncStatusObserver {
    private static final String TAG = NewsFragment.class.getSimpleName();
    public static final String EXTRA_NEWS_ID = "com.desklampstudios.thyroxine.news.id";
    public static final String ARG_LOGGED_IN = "loggedIn";
    private static final int NEWS_LOADER = 0;

    private NewsListAdapter mAdapter;
    private SwipeRefreshLayout mSwipeLayout;

    private Object mSyncObserverHandle; // obtained in onResume

    public NewsFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NewsFragment.
     */
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
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
                    long id = cursor.getLong(cursor.getColumnIndex(NewsContract.NewsEntries._ID));
                    openNewsDetailActivity(id);
                }
            }
        });

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.primary);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
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

        // start loader
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
        final Account account = StubAuthenticator.getStubAccount(activity);

        final boolean syncActive = ContentResolver.isSyncActive(
                account, NewsContract.CONTENT_AUTHORITY);
        final boolean syncPending = ContentResolver.isSyncPending(
                account, NewsContract.CONTENT_AUTHORITY);

        Log.d(TAG, "onStatusChanged: syncActive=" + syncActive + ", syncPending=" + syncPending);

        // Run on the UI thread in order to update the UI
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (account == null) {
                    mSwipeLayout.setRefreshing(false);
                    return;
                }
                mSwipeLayout.setRefreshing(syncActive || syncPending);
            }
        });
    }

    // Called when an item in the adapter is clicked
    private void openNewsDetailActivity(long id) {
        // Toast.makeText(getApplicationContext(), "Entry: " + entry, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(getActivity(), NewsDetailActivity.class);
        intent.putExtra(EXTRA_NEWS_ID, id);
        startActivity(intent);
    }

    private void retrieveNews() {
        // Request immediate sync
        NewsSyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
        case NEWS_LOADER:
            return new CursorLoader(
                    getActivity(),
                    NewsContract.NewsEntries.CONTENT_URI,
                    new String[] { // columns
                            NewsContract.NewsEntries._ID,
                            NewsContract.NewsEntries.KEY_TITLE,
                            NewsContract.NewsEntries.KEY_DATE,
                            NewsContract.NewsEntries.KEY_SNIPPET
                    },
                    null, // selection
                    null, // selectionArgs
                    NewsContract.NewsEntries.KEY_DATE + " DESC" // orderBy
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
