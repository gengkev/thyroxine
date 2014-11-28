package com.desklampstudios.thyroxine.news;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.desklampstudios.thyroxine.DividerItemDecoration;
import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.R;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewsFragment extends Fragment implements NewsListAdapter.EntryClickListener {
    private static final String TAG = NewsFragment.class.getSimpleName();
    public static final String EXTRA_ENTRY = "com.desklampstudios.thyroxine.ENTRY";
    public static final String ARG_LOGGED_IN = "loggedIn";
    private boolean loggedIn;

    private RetrieveNewsTask mRetrieveNewsTask;
    private RecyclerView mRecyclerView;
    private NewsListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public NewsFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NewsFragment.
     */
    public static NewsFragment newInstance(boolean loggedIn) {
        NewsFragment fragment = new NewsFragment();

        Bundle args = new Bundle();
        args.putBoolean(ARG_LOGGED_IN, loggedIn);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            loggedIn = getArguments().getBoolean(ARG_LOGGED_IN);
        }

        // create entries list
        mAdapter = new NewsListAdapter(new ArrayList<IodineNewsEntry>(), this);
        mAdapter.add(new IodineNewsEntry("https://iodine.tjhsst.edu/", "Empty", 0L, "No news items."));

        // load feed
        retrieveNews();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_news, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true); // changes in content don't change layout size

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // item decorations??
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(
                getActivity(), DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
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
    @Override
    public void onItemClick(IodineNewsEntry entry) {
        // Toast.makeText(getApplicationContext(), "Entry: " + entry, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(getActivity(), NewsDetailActivity.class);
        intent.putExtra(EXTRA_ENTRY, entry);
        startActivity(intent);
    }

    // Starts retrieveNewsTask
    public void retrieveNews() {
        if (mRetrieveNewsTask != null) {
            return;
        }

        Toast.makeText(getActivity(), "Loading...", Toast.LENGTH_SHORT).show();

        // Reset adapter
        mAdapter.clear();

        // Load stuff async
        mRetrieveNewsTask = new RetrieveNewsTask();
        mRetrieveNewsTask.execute();
    }


    private class RetrieveNewsTask extends AsyncTask<Void, IodineNewsEntry, List<IodineNewsEntry>> {
        private Exception exception = null;

        @Override
        protected List<IodineNewsEntry> doInBackground(Void... params) {
            InputStream stream = null;
            IodineNewsFeedParser parser;
            List<IodineNewsEntry> entries = new ArrayList<IodineNewsEntry>();

            try {
                if (loggedIn) {
                    stream = IodineApiHelper.getPrivateNewsFeed();
                } else {
                    stream = IodineApiHelper.getPublicNewsFeed();
                }
                parser = new IodineNewsFeedParser();
                parser.beginFeed(stream);

                IodineNewsEntry entry;
                while (!isCancelled()) {
                    entry = parser.nextEntry();

                    if (entry == null)
                        break;

                    publishProgress(entry);
                    entries.add(entry);
                }

            } catch (IOException e) {
                Log.e(TAG, "Connection error: " + e.toString());
                exception = e;
                return null;
            } catch (XmlPullParserException e) {
                Log.e(TAG, "XML error: " + e.toString());
                exception = e;
                return null;
            } finally {
                try {
                    if (stream != null) stream.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException when closing stream: " + e);
                }
            }

            return entries;
        }

        @Override
        protected void onProgressUpdate(IodineNewsEntry... entries) {
            mAdapter.add(entries[0]);
            // Log.i(TAG, "Adding entry: " + entries[0]);
        }

        @Override
        protected void onPostExecute(List<IodineNewsEntry> entries) {
            mRetrieveNewsTask = null;
            if (exception != null) {
                Log.e(TAG, "Error getting feed: " + exception);

                if (getActivity() != null) {
                    Toast.makeText(getActivity(),
                            "Error getting feed: " + exception,
                            Toast.LENGTH_LONG).show();
                }
                return;
            }

            Log.i(TAG, "Got feed (" + entries.size() + " entries)");

            if (getActivity() != null) {
                Toast.makeText(getActivity(),
                        "Got feed (" + entries.size() + " entries)",
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            mRetrieveNewsTask = null;
        }
    }
}
