package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;
import com.desklampstudios.thyroxine.util.DividerItemDecoration;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BlockFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BlockFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,ActvsListAdapter.ActvClickListener {
    private static final String TAG = BlockFragment.class.getSimpleName();
    private static final int BLOCK_LOADER = 1;
    public static final String ARG_BLOCK_ID = "com.desklampstudios.thyroxine.eighth.BLOCK_ID";

    private static final String[] BLOCK_LOADER_PROJECTION = new String[]{
            EighthContract.Blocks.KEY_BLOCK_ID,
            EighthContract.Blocks.KEY_DATE,
            EighthContract.Blocks.KEY_TYPE,
            EighthContract.Blocks.KEY_LOCKED,
            EighthContract.Schedule.KEY_ACTV_ID
    };

    private int blockId;

    @Nullable private CursorLoader blockLoader;
    @Nullable private FetchBlockTask mFetchBlockTask;

    private RecyclerView mRecyclerView;
    private ActvsListAdapter mAdapter;

    public BlockFragment() {
    }

    @NonNull
    public static BlockFragment newInstance(int bid) {
        BlockFragment fragment = new BlockFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_BLOCK_ID, bid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            blockId = getArguments().getInt(ARG_BLOCK_ID);
        }

        // create list adapter
        mAdapter = new ActvsListAdapter(this);
        mAdapter.add(new Pair<>(
                new EighthActv(999, "Test activity", "Test description", 0),
                new EighthActvInstance(999, 1337, "Test comment", 0, "All the rooms", 0, 0)
        ));

        // load blocks
        getBlock();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_eighth_block, container, false);

        // recyclerview!
        mRecyclerView = (RecyclerView) view.findViewById(R.id.actvs_list);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true); // changes in content don't change layout size

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);

        // item decorations??
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(
                getActivity(), DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);

        return view;
    }

    // Called when an item in the adapter is clicked
    @Override
    public void onActvClick(EighthActvInstance actv) {
        Toast.makeText(getActivity(), "Activity: " + actv, Toast.LENGTH_LONG).show();
    }

    // Starts FetchBlockTask
    private void getBlock() {
        if (mFetchBlockTask != null) {
            return;
        }

        Toast.makeText(getActivity(), "Loading...", Toast.LENGTH_SHORT).show();

        // Check login state
        Account account = IodineAuthenticator.getIodineAccount(getActivity());

        if (account == null) { // not logged in... ???
            Log.e(TAG, "No accounts found (not logged in) in BlockFragment???");
            Toast.makeText(getActivity(), "Not logged in", Toast.LENGTH_SHORT).show();
            // TODO: maybe implement login screen here
            return;
        }

        // Load stuff async
        mFetchBlockTask = new FetchBlockTask(getActivity(), account, new FetchBlockTask.ActvsResultListener() {
            @Override
            public void onActvsResult(ArrayList<Pair<EighthActv, EighthActvInstance>> pairList) {
                mAdapter.clear();
                mAdapter.addAll(pairList);
            }
        });
        mFetchBlockTask.execute(blockId);
    }

    private void displayBlock(@NonNull EighthBlock block) {
        Log.d(TAG, "block: " + block);

        String dateStr = Utils.formatBasicDate(block.date, Utils.DISPLAY_DATE_FORMAT_MEDIUM);
        if (getActivity() != null) {
            getActivity().setTitle(dateStr + " Block " + block.type);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // start loaders
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(BLOCK_LOADER, null, this);
    }

    @Nullable
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case BLOCK_LOADER: {
                blockLoader = new CursorLoader(
                        getActivity(),
                        EighthContract.Blocks.buildBlockUri(blockId),
                        BLOCK_LOADER_PROJECTION, // columns
                        null, // selection
                        null, // selectionArgs
                        null
                );
                return blockLoader;
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, @Nullable Cursor cursor) {
        if (cursorLoader == blockLoader) {
            if (cursor != null && cursor.moveToFirst()) {
                ContentValues blockValues = Utils.cursorRowToContentValues(cursor);
                EighthBlock block = EighthContract.Blocks.fromContentValues(blockValues);
                displayBlock(block);

                // TODO: do stuff with current actvId
                int curActvId = cursor.getInt(cursor.getColumnIndex(EighthContract.Schedule.KEY_ACTV_ID));

                Log.d(TAG, "Got block: " + block + ", curActvId: " + curActvId);
            } else {
                Log.e(TAG, "Cursor error");
            }
        }
        else {
            Log.e(TAG, "onLoadFinished for unknown CursorLoader: " + cursorLoader);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // ???
    }
}
