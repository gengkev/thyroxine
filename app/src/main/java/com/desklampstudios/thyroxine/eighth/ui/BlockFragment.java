package com.desklampstudios.thyroxine.eighth.ui;

import android.accounts.Account;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.eighth.sync.FetchBlockTask;
import com.desklampstudios.thyroxine.eighth.sync.SignupActvTask;
import com.desklampstudios.thyroxine.eighth.io.EighthSignupException;
import com.desklampstudios.thyroxine.eighth.provider.EighthContract;
import com.desklampstudios.thyroxine.eighth.model.EighthBlock;
import com.desklampstudios.thyroxine.eighth.model.EighthBlockAndActv;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BlockFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BlockFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = BlockFragment.class.getSimpleName();
    public static final String ARG_BLOCK_ID = "com.desklampstudios.thyroxine.eighth.BLOCK_ID";

    private static final int BLOCK_LOADER = 1;
    private static final String[] BLOCK_LOADER_PROJECTION = new String[]{
            EighthContract.Blocks.KEY_BLOCK_ID,
            EighthContract.Blocks.KEY_DATE,
            EighthContract.Blocks.KEY_TYPE,
            EighthContract.Blocks.KEY_LOCKED,
            EighthContract.Schedule.KEY_ACTV_ID
    };

    private int blockId;

    @Nullable private FetchBlockTask mFetchBlockTask;
    @Nullable private SignupActvTask mSignupActvTask;

    private ActvsListAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public BlockFragment() {}

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
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            blockId = getArguments().getInt(ARG_BLOCK_ID);
        }

        // create list adapter
        mAdapter = new ActvsListAdapter(getActivity());
        mAdapter.setOnItemClickListener(new ActvsListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int pos) {
                EighthBlockAndActv pair = mAdapter.getItem(pos);
                onActvClick(pair);
            }
            @Override
            public boolean onItemLongClick(View view, int pos) {
                EighthBlockAndActv pair = mAdapter.getItem(pos);
                changeSelectedActv(pair);
                return true;
            }
        });

        /*
        mAdapter.addItem(new Pair<>(
                new EighthActv.Builder()
                        .actvId(999)
                        .name("Test activity")
                        .description("Test description")
                        .build(),
                new EighthActvInstance.Builder()
                        .actvId(999)
                        .comment("Test comment")
                        .roomsStr("All the rooms")
                        .build()
        ));
        */
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_eighth_block, container, false);

        // RecyclerView!
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.actvs_list);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setHasFixedSize(true); // changes in content don't change layout size

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // dividers between items
        //RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(
        //        getActivity(), DividerItemDecoration.VERTICAL_LIST);
        //recyclerView.addItemDecoration(itemDecoration);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.primary);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
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
        getLoaderManager().initLoader(BLOCK_LOADER, null, this);

        // check if user is logged in
        if (checkLoginState() != null) {
            // load actvs from server
            // http://stackoverflow.com/a/26910973/689161
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    retrieveBlock();
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.eighth_block, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // Called when an item in the adapter is clicked
    private void onActvClick(@NonNull EighthBlockAndActv pair) {
        String text = getString(R.string.actv_toast_text,
                pair.actv.actvId,
                pair.actv.name,
                pair.actv.description,
                pair.actvInstance.comment,
                pair.actvInstance.roomsStr,
                pair.actvInstance.sponsorsStr,
                pair.actvInstance.memberCount,
                pair.actvInstance.capacity
        );
        Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
    }

    private Account checkLoginState() {
        if (getActivity() == null) {
            Log.w(TAG, "getActivity() is null");
            return null;
        }
        Account account = IodineAuthenticator.getIodineAccount(getActivity());
        if (account == null) { // not logged in
            Toast.makeText(getActivity(), R.string.error_not_logged_in, Toast.LENGTH_SHORT).show();
            IodineAuthenticator.attemptAddAccount(getActivity());
        }
        return account;
    }

    // Starts FetchBlockTask
    private void retrieveBlock() {
        if (mFetchBlockTask != null) {
            Log.w(TAG, "retrieveBlock cannot refresh while mFetchBlockTask is non-null");
            return;
        }

        // make sure user is logged in
        Account account = checkLoginState();
        if (account == null) {
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        // indicate syncing
        mSwipeRefreshLayout.setRefreshing(true);

        // Load stuff using AsyncTask
        mFetchBlockTask = new FetchBlockTask(getActivity(), account, new FetchBlockTask.ActvsResultListener() {
            @Override
            public void onActvsResult(List<EighthBlockAndActv> pairList) {
                mFetchBlockTask = null;
                mSwipeRefreshLayout.setRefreshing(false); // syncing done

                mAdapter.replaceAllItems(pairList);
            }

            @Override
            public void onError(Exception exception) {
                mFetchBlockTask = null;
                mSwipeRefreshLayout.setRefreshing(false); // syncing done
                String message = getString(R.string.unexpected_error, String.valueOf(exception));
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            }
        });
        mFetchBlockTask.execute(blockId);
    }

    private void changeSelectedActv(@NonNull final EighthBlockAndActv pair) {
        if (mSignupActvTask != null) {
            String message = getActivity().getString(R.string.signup_changing_wait);
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            return;
        }

        String message = getActivity().getString(R.string.signup_changing, pair.actv.name);
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

        Account account = IodineAuthenticator.getIodineAccount(getActivity());
        mSignupActvTask = new SignupActvTask(getActivity(), account, new SignupActvTask.SignupResultListener() {
            @Override
            public void onSignupResult() {
                mSignupActvTask = null;
                String message = getActivity().getString(R.string.signup_success, pair.actv.name);
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Exception exception) {
                mSignupActvTask = null;
                if (exception instanceof EighthSignupException) {
                    String message = getActivity().getString(R.string.signup_failure, pair.actv.name)
                            + "\n" + exception.getMessage();
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                } else {
                    String message = getString(R.string.unexpected_error, String.valueOf(exception));
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
        mSignupActvTask.execute(blockId, pair.actv, pair.actvInstance);
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
                        null // orderBy
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

            int curActvId = cursor.getInt(
                    cursor.getColumnIndex(EighthContract.Schedule.KEY_ACTV_ID));
            mAdapter.setSelectedActvId(curActvId);

            Log.d(TAG, "Got block: " + block + ", curActvId: " + curActvId);
        }
        else {
            Log.e(TAG, "Cursor error");
            Toast.makeText(getActivity(), R.string.error_database, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.setSelectedActvId(-1);
    }
}
