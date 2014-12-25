package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;
import com.desklampstudios.thyroxine.util.DividerItemDecoration;

import java.util.ArrayList;

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

    @Nullable private FetchBlockTask mFetchBlockTask;

    private ActvsListAdapter mAdapter;
    private SwipeRefreshLayout mSwipeLayout;

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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_eighth_block, container, false);

        // recyclerview!
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.actvs_list);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setHasFixedSize(true); // changes in content don't change layout size

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // item decorations??
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(
                getActivity(), DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.primary);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                retrieveBlock();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // check if user is logged in
        if (checkLoginState()) {
            // start block loader
            getLoaderManager().initLoader(BLOCK_LOADER, null, this);

            // load actvs from server
            // http://stackoverflow.com/a/26910973/689161
            mSwipeLayout.post(new Runnable() {
                @Override
                public void run() {
                    retrieveBlock();
                }
            });
        }
    }

    // Called when an item in the adapter is clicked
    @Override
    public void onActvClick(EighthActvInstance actv) {
        Toast.makeText(getActivity(), "Activity: " + actv, Toast.LENGTH_LONG).show();
    }

    private boolean checkLoginState() {
        Account account = IodineAuthenticator.getIodineAccount(getActivity());
        if (account == null) { // not logged in
            Toast.makeText(getActivity(), "Not logged in", Toast.LENGTH_SHORT).show();
            IodineAuthenticator.addAccount(getActivity());
            return false;
        }
        return true;
    }

    // Starts FetchBlockTask
    private void retrieveBlock() {
        if (mFetchBlockTask != null) {
            Log.w(TAG, "retrieveBlock cannot refresh while mFetchBlockTask is non-null");
            return;
        }

        // make sure user is logged in
        if (!checkLoginState()) {
            mSwipeLayout.setRefreshing(false);
            return;
        }

        // indicate syncing
        mSwipeLayout.setRefreshing(true);

        // Load stuff using AsyncTask
        Account account = IodineAuthenticator.getIodineAccount(getActivity());
        mFetchBlockTask = new FetchBlockTask(getActivity(), account, new FetchBlockTask.ActvsResultListener() {
            @Override
            public void onActvsResult(ArrayList<Pair<EighthActv, EighthActvInstance>> pairList) {
                Log.d(TAG, "syncing done");

                mFetchBlockTask = null;
                mSwipeLayout.setRefreshing(false); // syncing done

                mAdapter.clear();
                mAdapter.addAll(pairList);
            }
        });
        mFetchBlockTask.execute(blockId);
    }

    private void displayBlock(@NonNull EighthBlock block) {
        String dateStr = Utils.formatBasicDate(block.date, Utils.DISPLAY_DATE_FORMAT_MEDIUM);
        if (getActivity() != null) {
            getActivity().setTitle(dateStr + " Block " + block.type);
        }
    }

    @Nullable
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case BLOCK_LOADER: {
                return new CursorLoader(
                        getActivity(),
                        EighthContract.Blocks.buildBlockUri(blockId),
                        BLOCK_LOADER_PROJECTION, // columns
                        null, // selection
                        null, // selectionArgs
                        null
                );
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, @Nullable Cursor cursor) {
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

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // ???
    }
}
