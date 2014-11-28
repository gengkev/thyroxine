package com.desklampstudios.thyroxine.eighth;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
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
 * Use the {@link EighthFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class EighthFragment extends Fragment implements EighthListAdapter.BlockClickListener {
    private static final String TAG = EighthFragment.class.getSimpleName();
    private static final String ARG_LOGGED_IN = "loggedIn";

    private boolean loggedIn;

    private RetrieveBlocksTask mRetrieveBlocksTask;
    private RecyclerView mRecyclerView;
    private EighthListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public EighthFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment EighthFragment.
     */
    public static EighthFragment newInstance(boolean param1) {
        EighthFragment fragment = new EighthFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_LOGGED_IN, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            loggedIn = getArguments().getBoolean(ARG_LOGGED_IN);
        }

        mAdapter = new EighthListAdapter(new ArrayList<IodineEighthBlock>(), this);
        mAdapter.add(new IodineEighthBlock(-1, 0L, "Z"));

        // load blocks
        retrieveBlocks();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_eighth, container, false);

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
    public void onBlockClick(IodineEighthBlock block) {
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

    private class RetrieveBlocksTask extends AsyncTask<Void, IodineEighthBlock, List<IodineEighthBlock>> {
        private Exception exception = null;

        @Override
        protected List<IodineEighthBlock> doInBackground(Void... params) {
            InputStream stream = null;
            IodineEighthParser parser;
            List<IodineEighthBlock> blocks = new ArrayList<IodineEighthBlock>();

            try {
                if (loggedIn) {
                    stream = IodineApiHelper.getBlockList();
                } else {
                    throw new IOException("Not logged in");
                }

                //Log.i(TAG, IodineApiHelper.readInputStream(stream));

                parser = new IodineEighthParser();
                parser.beginListBlocks(stream);

                IodineEighthBlock block;
                while (!isCancelled()) {
                    block = parser.nextBlock();

                    if (block == null)
                        break;

                    publishProgress(block);
                    blocks.add(block);
                }

            }
            catch (IOException e) {
                Log.e(TAG, "Connection error: " + e.toString());
                exception = e;
                return null;
            }
            catch (XmlPullParserException e) {
                Log.e(TAG, "XML error: " + e.toString());
                exception = e;
                return null;
            }
            finally {
                try { if (stream != null) stream.close(); }
                catch (IOException e) {
                    Log.e(TAG, "IOException when closing stream: " + e);
                }
            }

            return blocks;
        }

        @Override
        protected void onProgressUpdate(IodineEighthBlock... entries) {
            mAdapter.add(entries[0]);
            // Log.i(TAG, "Adding entry: " + entries[0]);
        }

        @Override
        protected void onPostExecute(List<IodineEighthBlock> entries) {
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
