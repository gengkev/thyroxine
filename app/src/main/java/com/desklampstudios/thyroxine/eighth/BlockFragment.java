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
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BlockFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BlockFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = BlockFragment.class.getSimpleName();
    private static final int ACTVS_LOADER = 0;
    private static final int BLOCK_LOADER = 1;
    public static final String ARG_BLOCK_ID = "com.desklampstudios.thyroxine.eighth.BLOCK_ID";

    private int blockId;

    private CursorLoader actvsLoader;
    private CursorLoader blockLoader;

    private FetchBlockTask mFetchBlockTask;

    private BlockListAdapter mAdapter;
    private ListView mListView;

    public BlockFragment() {
    }

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
        mAdapter = new BlockListAdapter(getActivity(), null, 0);

        // load blocks
        getBlock();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_eighth_block, container, false);

        mListView = (ListView) view.findViewById(R.id.activities_list);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                Cursor cursor = mAdapter.getCursor();

                if (cursor != null && cursor.moveToPosition(pos)) {
                    int actvId = cursor.getInt(cursor.getColumnIndex(
                            EighthContract.Actvs.KEY_ACTV_ID));
                    onActvClick(actvId);
                }
            }
        });

        return view;
    }

    // Called when an item in the adapter is clicked
    private void onActvClick(int actvId) {
        Toast.makeText(getActivity(), "Activity: " + actvId, Toast.LENGTH_LONG).show();
    }

    // Starts FetchBlockTask
    private void getBlock() {
        if (mFetchBlockTask != null) {
            return;
        }

        Toast.makeText(getActivity(), "Loading...", Toast.LENGTH_SHORT).show();

        // Check login state
        final AccountManager am = AccountManager.get(getActivity());
        Account[] accounts = am.getAccountsByType(IodineAuthenticator.ACCOUNT_TYPE);

        if (accounts.length == 0) { // not logged in... wait what?
            Log.e(TAG, "No accounts found (not logged in) in BlockFragment??");
            Toast.makeText(getActivity(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        else if (accounts.length > 1) {
            Log.e(TAG, "More than one account: " + Arrays.toString(accounts));
        }

        // Load stuff async
        mFetchBlockTask = new FetchBlockTask(getActivity(), accounts[0]);
        mFetchBlockTask.execute(blockId);
    }

    private void displayBlock(EighthBlock block) {
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
        loaderManager.initLoader(ACTVS_LOADER, null, this);
        loaderManager.initLoader(BLOCK_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
        case ACTVS_LOADER:
            actvsLoader = new CursorLoader(
                    getActivity(),
                    EighthContract.Blocks.buildBlockUri(blockId)
                            .buildUpon().appendPath("actvInstances").build(),
                    new String[] { // columns
                            EighthContract.ActvInstances._ID,
                            EighthContract.ActvInstances.KEY_ACTV_ID,
                            EighthContract.ActvInstances.KEY_ROOMS_STR,
                            EighthContract.ActvInstances.KEY_COMMENT,
                            EighthContract.ActvInstances.KEY_MEMBER_COUNT,
                            EighthContract.ActvInstances.KEY_CAPACITY,
                            EighthContract.ActvInstances.KEY_FLAGS,
                            EighthContract.Actvs.KEY_NAME,
                            EighthContract.Actvs.KEY_DESCRIPTION,
                            EighthContract.Actvs.KEY_FLAGS
                    },
                    null, // selection
                    null, // selectionArgs
                    EighthContract.Actvs.KEY_NAME + " ASC" // orderBy
            );
            return actvsLoader;
        case BLOCK_LOADER:
            blockLoader = new CursorLoader(
                    getActivity(),
                    EighthContract.Blocks.buildBlockUri(blockId),
                    new String[] { // columns
                            EighthContract.Blocks.KEY_BLOCK_ID,
                            EighthContract.Blocks.KEY_DATE,
                            EighthContract.Blocks.KEY_TYPE,
                            EighthContract.Blocks.KEY_LOCKED,
                            EighthContract.Schedule.KEY_ACTV_ID
                    },
                    null, // selection
                    null, // selectionArgs
                    null
            );
            return blockLoader;
        default:
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursorLoader == actvsLoader) {
            mAdapter.swapCursor(cursor);
        }
        else if (cursorLoader == blockLoader) {
            if (cursor != null && cursor.moveToFirst()) {
                ContentValues blockValues = Utils.cursorRowToContentValues(cursor);
                EighthBlock block = EighthContract.Blocks.contentValuesToEighthBlock(blockValues);
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
        if (cursorLoader == actvsLoader) {
            mAdapter.swapCursor(null);
        }
    }
}
