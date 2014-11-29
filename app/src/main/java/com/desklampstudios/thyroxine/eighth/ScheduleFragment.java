package com.desklampstudios.thyroxine.eighth;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
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
 * Use the {@link ScheduleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ScheduleFragment extends Fragment implements ScheduleAdapter.BlockClickListener {
    private static final String TAG = ScheduleFragment.class.getSimpleName();
    private static final String ARG_LOGGED_IN = "loggedIn";

    private boolean loggedIn;

    private RetrieveBlocksTask mRetrieveBlocksTask;
    private RecyclerView mRecyclerView;
    private ScheduleAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public ScheduleFragment() {
    }

    public static ScheduleFragment newInstance() {
        ScheduleFragment fragment = new ScheduleFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        if (getArguments() != null) {
            loggedIn = getArguments().getBoolean(ARG_LOGGED_IN);
        }
        */

        mAdapter = new ScheduleAdapter(new ArrayList<EighthBlock>(), this);
        mAdapter.add(new EighthBlock(-1, 0L, "Z"));

        // load blocks
        retrieveBlocks();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_eighth_schedule, container, false);

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

    // Called when an item in the adapter is clicked
    @Override
    public void onBlockClick(EighthBlock block) {
        //Toast.makeText(getActivity(), "Block: " + block, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(getActivity(), BlockActivity.class);
        intent.putExtra(BlockFragment.ARG_BID, block.bid);
        startActivity(intent);
    }

    // Starts RetrieveBlocksTask
    public void retrieveBlocks() {
        if (mRetrieveBlocksTask != null) {
            return;
        }

        Toast.makeText(getActivity(), "Loading...", Toast.LENGTH_SHORT).show();

        // Reset adapter
        mAdapter.clear();

        // Load stuff async
        mRetrieveBlocksTask = new RetrieveBlocksTask();
        mRetrieveBlocksTask.execute();
    }

    private class RetrieveBlocksTask extends AsyncTask<Void, EighthBlock, List<EighthBlock>> {
        private Exception exception = null;

        @Override
        protected List<EighthBlock> doInBackground(Void... params) {
            InputStream stream = null;
            IodineEighthParser parser;
            List<EighthBlock> blocks = new ArrayList<EighthBlock>();

            try {
                //if (loggedIn) {
                stream = IodineApiHelper.getBlockList();
                //} else {
                //    throw new IOException("Not logged in");
                //}

                //Log.i(TAG, IodineApiHelper.readInputStream(stream));

                parser = new IodineEighthParser();
                parser.beginListBlocks(stream);

                EighthBlock block;
                while (!isCancelled()) {
                    block = parser.nextBlock();

                    if (block == null)
                        break;

                    publishProgress(block);
                    blocks.add(block);
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

            return blocks;
        }

        @Override
        protected void onProgressUpdate(EighthBlock... entries) {
            mAdapter.add(entries[0]);
            // Log.i(TAG, "Adding entry: " + entries[0]);
        }

        @Override
        protected void onPostExecute(List<EighthBlock> entries) {
            mRetrieveBlocksTask = null;
            if (exception != null) {
                Log.e(TAG, "RetrieveBlocksTask error: " + exception);

                if (getActivity() != null) {
                    Toast.makeText(getActivity(),
                            "RetrieveBlocksTask error: " + exception,
                            Toast.LENGTH_LONG).show();
                }
                return;
            }

            Log.i(TAG, "Got blocks (" + entries.size() + " blocks)");

            if (getActivity() != null) {
                Toast.makeText(getActivity(),
                        "Got blocks (" + entries.size() + " blocks)",
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            mRetrieveBlocksTask = null;
        }
    }
}
