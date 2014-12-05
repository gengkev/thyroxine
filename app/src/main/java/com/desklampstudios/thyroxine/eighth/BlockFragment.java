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
import java.text.DateFormat;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BlockFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BlockFragment extends Fragment implements BlockAdapter.ActvClickListener {
    private static final String TAG = BlockFragment.class.getSimpleName();
    public static final String ARG_BID = "com.desklampstudios.thyroxine.BID";

    private int bid;

    private GetBlockTask mGetBlockTask;
    private RecyclerView mRecyclerView;
    private BlockAdapter mAdapter;

    private RecyclerView mSelectedActvRecyclerView;
    private BlockAdapter mSelectedActvAdapter;

    public BlockFragment() {
    }

    public static BlockFragment newInstance(int bid) {
        BlockFragment fragment = new BlockFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_BID, bid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bid = getArguments().getInt(ARG_BID);
        }

        mAdapter = new BlockAdapter( new ArrayList<EighthActvInstance>(), this );
        mAdapter.add( new EighthActvInstance(
                new EighthActv(-1, "<Actv name>", "<Actv desc>", 0),
                "<Actv comment>", 0));


        mSelectedActvAdapter = new BlockAdapter( new ArrayList<EighthActvInstance>(), this );
        mAdapter.add( new EighthActvInstance(
                new EighthActv(-1, "<Actv name>", "<Actv desc>", 0),
                "<Actv comment>", 0));

        // load blocks
        getBlock();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_eighth_block, container, false);

        // recyclerview!
        mRecyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true); // changes in content don't change layout size

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);

        // item decorations??
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(
                getActivity(), DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);

        // selected activity recycler view
        mSelectedActvRecyclerView = (RecyclerView) view.findViewById(R.id.selected_actv_recycler_view);
        mSelectedActvRecyclerView.setAdapter(mSelectedActvAdapter);
        mSelectedActvRecyclerView.setHasFixedSize(false);
        mSelectedActvRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    // Called when an item in the adapter is clicked
    @Override
    public void onActvClick(EighthActvInstance actv) {
        Toast.makeText(getActivity(), "Activity: " + actv, Toast.LENGTH_LONG).show();
    }

    // Starts RetrieveBlocksTask
    private void getBlock() {
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

    private void displayBlock(EighthBlock block) {
        Log.d(TAG, "block: " + block);

        mSelectedActvAdapter.clear();
        mSelectedActvAdapter.add(block.selectedActv);


        DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM); // default locale OK
        getActivity().setTitle(DATE_FORMAT.format(block.date) + " Block " + block.type);
    }

    private class GetBlockTask extends AsyncTask<Void, EighthActvInstance, EighthBlock> {
        private Exception exception = null;

        @Override
        protected EighthBlock doInBackground(Void... params) {
            InputStream stream = null;
            IodineEighthParser parser;
            EighthBlock block;

            try {
                stream = IodineApiHelper.getBlock(bid);

                parser = new IodineEighthParser();
                block = parser.beginGetBlock(stream);

                EighthActvInstance actv;
                while (!isCancelled()) {
                    actv = parser.nextActivity();

                    if (actv == null)
                        break;

                    publishProgress(actv);
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

            return block;
        }

        @Override
        protected void onProgressUpdate(EighthActvInstance... entries) {
            mAdapter.add(entries[0]);
            // Log.i(TAG, "Adding entry: " + entries[0]);
        }

        @Override
        protected void onPostExecute(EighthBlock block) {
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

            displayBlock(block);
            /*
            Log.i(TAG, "Got activities (" + entries.size() + " activities)");

            if (getActivity() != null) {
                Toast.makeText(getActivity(),
                        "Got activities (" + entries.size() + " activities)",
                        Toast.LENGTH_SHORT).show();
            }
            */
        }

        @Override
        protected void onCancelled() {
            mGetBlockTask = null;
        }
    }

}
