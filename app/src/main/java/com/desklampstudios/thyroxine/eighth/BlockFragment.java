package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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

import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BlockFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BlockFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
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
    @Nullable private SignupActvTask mSignupActvTask;

    private ActvsListAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

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
        mAdapter = new ActvsListAdapter(getActivity());
        mAdapter.setOnItemClickListener(new ActvsListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int pos) {
                Pair<EighthActv, EighthActvInstance> pair = mAdapter.get(pos);
                onActvClick(pair);
            }
            @Override
            public boolean onItemLongClick(View view, int pos) {
                Pair<EighthActv, EighthActvInstance> pair = mAdapter.get(pos);
                changeSelectedActv(pair);
                return true;
            }
        });
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

        // check if user is logged in
        if (checkLoginState()) {
            // start block loader
            getLoaderManager().initLoader(BLOCK_LOADER, null, this);

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

    // Called when an item in the adapter is clicked
    public void onActvClick(@NonNull Pair<EighthActv, EighthActvInstance> pair) {
        String text = getString(R.string.actv_toast_text,
                pair.first.name,
                pair.second.roomsStr,
                pair.second.comment,
                pair.first.description,
                pair.second.memberCount,
                pair.second.capacity
        );
        Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
    }

    private boolean checkLoginState() {
        Account account = IodineAuthenticator.getIodineAccount(getActivity());
        if (account == null) { // not logged in
            Toast.makeText(getActivity(), R.string.error_not_logged_in, Toast.LENGTH_SHORT).show();
            IodineAuthenticator.attemptAddAccount(getActivity());
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
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        // indicate syncing
        mSwipeRefreshLayout.setRefreshing(true);

        // Load stuff using AsyncTask
        Account account = IodineAuthenticator.getIodineAccount(getActivity());
        mFetchBlockTask = new FetchBlockTask(getActivity(), account, new FetchBlockTask.ActvsResultListener() {
            @Override
            public void onActvsResult(ArrayList<Pair<EighthActv, EighthActvInstance>> pairList) {
                mFetchBlockTask = null;
                mSwipeRefreshLayout.setRefreshing(false); // syncing done

                mAdapter.clear();
                mAdapter.addAll(pairList);
            }

            @Override
            public void onError(Exception exception) {
                mFetchBlockTask = null;
                mSwipeRefreshLayout.setRefreshing(false); // syncing done
                handleAsyncTaskError(exception);
            }
        });
        mFetchBlockTask.execute(blockId);
    }

    private void changeSelectedActv(@NonNull final Pair<EighthActv, EighthActvInstance> pair) {
        if (mSignupActvTask != null) {
            String message = getActivity().getString(R.string.signup_changing_wait);
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            return;
        }

        String message = getActivity().getString(R.string.signup_changing, pair.first.name);
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

        Account account = IodineAuthenticator.getIodineAccount(getActivity());
        mSignupActvTask = new SignupActvTask(getActivity(), account, new SignupActvTask.SignupResultListener() {
            @Override
            public void onSignupResult(int result) {
                mSignupActvTask = null;
                if (result == 0) { // success
                    String message = getActivity().getString(R.string.signup_success, pair.first.name);
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                    updateDatabase(blockId, pair.first, pair.second);
                } else { // error
                    String message = getActivity().getString(R.string.signup_failure, pair.first.name);
                    String errors = getSignupErrorString(result);
                    Toast.makeText(getActivity(), message + errors, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception exception) {
                mSignupActvTask = null;
                handleAsyncTaskError(exception);
            }
        });
        mSignupActvTask.execute(blockId, pair.first.actvId);
    }

    private void handleAsyncTaskError(Exception exception) {
        if (exception instanceof IodineAuthException.NotLoggedInException) {
            Toast.makeText(getActivity(), R.string.attempt_login_try_again, Toast.LENGTH_LONG).show();
        } else {
            String message = getString(
                    R.string.unexpected_error, String.valueOf(exception));
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private String getSignupErrorString(int result) {
        final String[] arr = getResources().getStringArray(R.array.eighth_signup_error);

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if ((result & (1 << i)) != 0) {
                out.append("\n").append(arr[i]);
            }
        }
        return out.toString();
    }

    // TODO: make less hacky
    private void updateDatabase(int blockId, @NonNull EighthActv actv, @NonNull EighthActvInstance actvInstance) {
        // push to db, or something
        final ContentResolver resolver = getActivity().getContentResolver();

        // Update schedule
        ContentValues scheduleValues = new ContentValues();
        scheduleValues.put(EighthContract.Schedule.KEY_BLOCK_ID, blockId);
        scheduleValues.put(EighthContract.Schedule.KEY_ACTV_ID, actv.actvId);
        Uri scheduleUri = resolver.insert(EighthContract.Schedule.CONTENT_URI, scheduleValues);
        Log.d(TAG, "updated schedule: inserted with uri " + scheduleUri);

        // Update EighthActv
        ContentValues actvValues = EighthContract.Actvs.toContentValues(actv);
        Uri actvUri = resolver.insert(EighthContract.Actvs.CONTENT_URI, actvValues);
        Log.d(TAG, "updated actv: inserted with uri " + actvUri);

        // Update EighthActvInstance
        ContentValues actvInstanceValues = EighthContract.ActvInstances.toContentValues(actvInstance);
        Uri actvInstanceUri = resolver.insert(EighthContract.ActvInstances.CONTENT_URI, actvInstanceValues);
        Log.d(TAG, "updated actvInstance: inserted with uri " + actvInstanceUri);

        // notify changes
        resolver.notifyChange(EighthContract.Blocks.CONTENT_URI, null, false);
    }

    private void displayBlock(@NonNull EighthBlock block) {
        String dateStr = Utils.DateFormats.MED_DAYMONTH.formatBasicDate(getActivity(), block.date);
        String weekday = Utils.DateFormats.WEEKDAY.formatBasicDate(getActivity(), block.date);
        String displayStr = getResources().getString(R.string.block_title_date,
                weekday, block.type, dateStr);

        if (getActivity() != null) {
            getActivity().setTitle(displayStr);
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

            int curActvId = cursor.getInt(
                    cursor.getColumnIndex(EighthContract.Schedule.KEY_ACTV_ID));
            mAdapter.setSelectedActvId(curActvId);

            Log.d(TAG, "Got block: " + block + ", curActvId: " + curActvId);
        } else {
            Log.e(TAG, "Cursor error");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }
}
