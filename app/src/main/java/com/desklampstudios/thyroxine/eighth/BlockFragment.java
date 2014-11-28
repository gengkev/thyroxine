package com.desklampstudios.thyroxine.eighth;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import java.util.BitSet;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BlockFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class BlockFragment extends Fragment implements BlockListAdapter.ActvClickListener {
    private static final String TAG = BlockFragment.class.getSimpleName();
    public static final String ARG_BID = "com.desklampstudios.thyroxine.BID";

    private int bid;

    private GetBlockTask mGetBlockTask;
    private RecyclerView mRecyclerView;
    private BlockListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment BlockFragment.
     */
    public static BlockFragment newInstance(int param1) {
        BlockFragment fragment = new BlockFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_BID, param1);
        fragment.setArguments(args);
        return fragment;
    }
    public BlockFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bid = getArguments().getInt(ARG_BID);
        }

        mAdapter = new BlockListAdapter(new ArrayList<IodineEighthActv>(), this);
        mAdapter.add(new IodineEighthActv(-1, "<Activity name>", "<Activity description>", new BitSet()));

        // load blocks
        getBlock();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_block, container, false);

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
    public void onActvClick(IodineEighthActv actv) {
        Toast.makeText(getActivity(), "Activity: " + actv, Toast.LENGTH_LONG).show();
    }

    // Starts RetrieveBlocksTask
    public void getBlock() {
        if (mGetBlockTask != null) {
            return;
        }

        Toast.makeText(getActivity(), "Loading...", Toast.LENGTH_SHORT).show();

        // Reset adapter
        mAdapter.clear();

        // Load stuff async
        mGetBlockTask = new GetBlockTask();
        mGetBlockTask.execute();
    }

    private void displayBlock(IodineEighthBlock block) {
        Log.d(TAG, "block: " + block);
    }

    private class GetBlockTask extends AsyncTask<Void, IodineEighthActv, List<IodineEighthActv>> {
        private Exception exception = null;

        @Override
        protected List<IodineEighthActv> doInBackground(Void... params) {
            InputStream stream = null;
            IodineEighthParser parser;
            List<IodineEighthActv> eighthActivities = new ArrayList<IodineEighthActv>();

            try {
                stream = IodineApiHelper.getBlock(bid);

                parser = new IodineEighthParser();
                IodineEighthBlock block = parser.beginGetBlock(stream);
                displayBlock(block);

                IodineEighthActv actv;
                while (!isCancelled()) {
                    actv = parser.nextActivity();

                    if (actv == null)
                        break;

                    publishProgress(actv);
                    eighthActivities.add(actv);
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

            return eighthActivities;
        }

        @Override
        protected void onProgressUpdate(IodineEighthActv... entries) {
            mAdapter.add(entries[0]);
            // Log.i(TAG, "Adding entry: " + entries[0]);
        }

        @Override
        protected void onPostExecute(List<IodineEighthActv> entries) {
            mGetBlockTask = null;
            if (exception != null) {
                Log.e(TAG, "GetBlockTask error: " + exception);

                if (getActivity() != null) {
                    Toast.makeText(getActivity(),
                            "GetBlockTask error: " + exception,
                            Toast.LENGTH_LONG).show();
                }
                return;
            }

            Log.i(TAG, "Got activities (" + entries.size() + " activities)");

            if (getActivity() != null) {
                Toast.makeText(getActivity(),
                        "Got activities (" + entries.size() + " activities)",
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            mGetBlockTask = null;
        }
    }

}
