package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.desklampstudios.thyroxine.DividerItemDecoration;
import com.desklampstudios.thyroxine.IodineApiHelper;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ScheduleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ScheduleFragment extends Fragment implements ScheduleListAdapter.BlockClickListener {
    private static final String TAG = ScheduleFragment.class.getSimpleName();

    private RetrieveBlocksTask mRetrieveBlocksTask;
    private RecyclerView mRecyclerView;
    private ScheduleListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private Account mAccount;

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

        mAdapter = new ScheduleListAdapter(this);
        mAdapter.add(new Pair<>(new EighthBlock(-1, "2014-12-21", "A", false), 999));

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
        intent.putExtra(BlockFragment.ARG_BID, block.blockId);
        startActivity(intent);
    }

    // Starts RetrieveBlocksTask
    private void retrieveBlocks() {
        if (mRetrieveBlocksTask != null) {
            return;
        }

        Toast.makeText(getActivity(), "Loading...", Toast.LENGTH_SHORT).show();

        // Reset adapter
        mAdapter.clear();

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
        else {
            mAccount = accounts[0];
        }

        // Load stuff async
        mRetrieveBlocksTask = new RetrieveBlocksTask();
        mRetrieveBlocksTask.execute();
    }

    private class RetrieveBlocksTask extends AsyncTask<Void, Object, Void> {
        private Exception exception = null;

        @Override
        protected Void doInBackground(Void... params) {
            final AccountManager am = AccountManager.get(getActivity());
            AccountManagerFuture<Bundle> future = am.getAuthToken(mAccount,
                    IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN, Bundle.EMPTY, getActivity(), null, null);

            String authToken;
            try {
                Bundle bundle = future.getResult();
                authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                Log.v(TAG, "Got bundle: " + bundle);
            } catch (IOException e) {
                Log.e(TAG, "Connection error: "+ e.toString());
                exception = e;
                return null;
            } catch (OperationCanceledException | AuthenticatorException e) {
                Log.e(TAG, "Authentication error: " + e.toString());
                exception = e;
                return null;
            }

            InputStream stream = null;
            IodineEighthParser parser = null;

            try {
                stream = IodineApiHelper.getBlockList(authToken);

                parser = new IodineEighthParser(getActivity());
                parser.beginListBlocks(stream);

                Pair<EighthBlock, Integer> pair;
                while (!isCancelled()) {
                    pair = parser.nextBlock();

                    if (pair == null)
                        break;

                    publishProgress(pair.first, pair.second);
                }

            } catch (IOException e) {
                Log.e(TAG, "Connection error: " + e.toString());
                exception = e;
                return null;
            } catch (XmlPullParserException e) {
                Log.e(TAG, "XML error: " + e.toString());
                exception = e;
                return null;
            } catch (IodineAuthException e) {
                Log.e(TAG, "Iodine auth error", e);
                exception = e;
                return null;
            } finally {
                if (parser != null)
                    parser.stopParse();
                try {
                    if (stream != null) stream.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException when closing stream: " + e);
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Object... args) {
            // TODO: this is incredibly stupid, remove it
            mAdapter.add(new Pair<>((EighthBlock)args[0], (Integer) args[1]));
            // Log.i(TAG, "Adding entry: " + entries[0]);
        }

        @Override
        protected void onPostExecute(Void _) {
            mRetrieveBlocksTask = null;
            if (exception != null) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(),
                            "RetrieveBlocksTask error: " + exception,
                            Toast.LENGTH_LONG).show();
                }
                return;
            }

            Log.i(TAG, "Got blocks");

            if (getActivity() != null) {
                Toast.makeText(getActivity(),
                        "Got blocks",
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            mRetrieveBlocksTask = null;
        }
    }
}
