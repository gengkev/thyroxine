package com.desklampstudios.thyroxine.news;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.desklampstudios.thyroxine.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = NewsFragment.class.getSimpleName();
    public static final String EXTRA_NEWS_ID = "com.desklampstudios.thyroxine.news.id";
    public static final String ARG_LOGGED_IN = "loggedIn";
    private static final int NEWS_LOADER = 0;

    private CursorAdapter mAdapter;
    private ListView mListView;
    private SwipeRefreshLayout mSwipeLayout;

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

        mListView = (ListView) view.findViewById(R.id.news_listview);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
        setHasOptionsMenu(true);

        // start loader
        getLoaderManager().initLoader(NEWS_LOADER, null, this);
    }

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

    // Called when an item in the adapter is clicked
    private void openNewsDetailActivity(long id) {
        // Toast.makeText(getApplicationContext(), "Entry: " + entry, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(getActivity(), NewsDetailActivity.class);
        intent.putExtra(EXTRA_NEWS_ID, id);
        startActivity(intent);
    }

    // Starts retrieveNewsTask
    private void retrieveNews() {
        mSwipeLayout.setRefreshing(true);

        // Start sync
        NewsSyncAdapter.syncImmediately(getActivity());

        // TODO: actually detect end of sync with SyncObserver
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeLayout.setRefreshing(false);
            }
        }, 5000);
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
