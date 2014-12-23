package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import java.util.Arrays;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ScheduleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ScheduleFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ScheduleFragment.class.getSimpleName();
    private static final int BLOCKS_LOADER = 0;

    private static final String[] BLOCKS_LOADER_PROJECTION = new String[] {
            EighthContract.Blocks._ID,
            EighthContract.Blocks.KEY_BLOCK_ID,
            EighthContract.Blocks.KEY_TYPE,
            EighthContract.Blocks.KEY_DATE,
            EighthContract.Actvs.KEY_NAME,
            EighthContract.Actvs.KEY_FLAGS,
            EighthContract.Actvs.KEY_ACTV_ID,
            EighthContract.ActvInstances.KEY_FLAGS,
            EighthContract.ActvInstances.KEY_MEMBER_COUNT,
            EighthContract.ActvInstances.KEY_CAPACITY
    };

    private FetchScheduleTask mFetchScheduleTask;

    private BlocksListAdapter mAdapter;

    public ScheduleFragment() {
    }

    @NonNull
    public static ScheduleFragment newInstance() {
        ScheduleFragment fragment = new ScheduleFragment();
        fragment.setArguments(Bundle.EMPTY);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create list adapter
        mAdapter = new BlocksListAdapter(getActivity(), null, 0);

        // load blocks
        retrieveBlocks();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_eighth_schedule, container, false);

        ListView listView = (ListView) view.findViewById(R.id.blocks_list);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                Cursor cursor = mAdapter.getCursor();

                if (cursor != null && cursor.moveToPosition(pos)) {
                    int blockId = cursor.getInt(cursor.getColumnIndex(
                            EighthContract.Blocks.KEY_BLOCK_ID));
                    onBlockClick(blockId);
                }
            }
        });

        return view;
    }

    // Called when an item in the adapter is clicked
    private void onBlockClick(int blockId) {
        //Toast.makeText(getActivity(), "Block: " + block, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(getActivity(), BlockActivity.class);
        intent.putExtra(BlockFragment.ARG_BLOCK_ID, blockId);
        startActivity(intent);
    }

    // Starts FetchScheduleTask
    private void retrieveBlocks() {
        if (mFetchScheduleTask != null) {
            return;
        }

        Toast.makeText(getActivity(), "Loading...", Toast.LENGTH_SHORT).show();

        // Check login state
        final AccountManager am = AccountManager.get(getActivity());
        Account[] accounts = am.getAccountsByType(IodineAuthenticator.ACCOUNT_TYPE);

        if (accounts.length == 0) { // not logged in
            Log.e(TAG, "No accounts found (not logged in)");
            Toast.makeText(getActivity(), "Not logged in", Toast.LENGTH_SHORT).show();
            am.addAccount(IodineAuthenticator.ACCOUNT_TYPE,
                    IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN,
                    null, null, getActivity(), new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> future) {
                            retrieveBlocks();
                        }
                    }, null);
            return;
        }
        else if (accounts.length > 1) {
            Log.e(TAG, "More than one account: " + Arrays.toString(accounts));
        }

        // Load stuff async
        mFetchScheduleTask = new FetchScheduleTask(getActivity());
        mFetchScheduleTask.execute(accounts[0]);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // start loader
        getLoaderManager().initLoader(BLOCKS_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case BLOCKS_LOADER: {
                return new CursorLoader(
                        getActivity(),
                        EighthContract.Blocks.CONTENT_URI,
                        BLOCKS_LOADER_PROJECTION, // columns
                        null, // selection
                        null, // selectionArgs
                        EighthContract.Blocks.DEFAULT_SORT // orderBy
                );
            }
            default: {
                return null;
            }
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
