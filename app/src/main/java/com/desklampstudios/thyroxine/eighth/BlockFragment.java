package com.desklampstudios.thyroxine.eighth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.sync.IodineAuthenticator;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BlockFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BlockFragment extends Fragment implements BlockListAdapter.ActvClickListener {
    private static final String TAG = BlockFragment.class.getSimpleName();
    public static final String ARG_BID = "com.desklampstudios.thyroxine.BID";

    private int bid;

    private GetBlockTask mGetBlockTask;
    private RecyclerView mRecyclerView;
    private BlockListAdapter mAdapter;

    private Account mAccount = null;

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

        mAdapter = new BlockListAdapter(this);
        mAdapter.add(new Pair<>(
                new EighthActv(999, "<Actv name>", "<Actv description>", 0),
                new EighthActvInstance(999, -1, "<Actv comment>", 0)));

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

        // Check login state
        final AccountManager am = AccountManager.get(getActivity());
        Account[] accounts = am.getAccountsByType(IodineAuthenticator.ACCOUNT_TYPE);

        if (accounts.length == 0) { // not logged in
            Log.e(TAG, "No accounts found (not logged in)");
            Toast.makeText(getActivity(), "Not logged in", Toast.LENGTH_SHORT).show();
            am.addAccount(IodineAuthenticator.ACCOUNT_TYPE,
                    IodineAuthenticator.IODINE_COOKIE_AUTH_TOKEN,
                    null, null, getActivity(), null, null);
            return;
        }
        else {
            mAccount = accounts[0];
        }

        // Load stuff async
        mGetBlockTask = new GetBlockTask();
        mGetBlockTask.execute();
    }

    private void displayBlock(EighthBlock block) {
        Log.d(TAG, "block: " + block);

        String dateStr = Utils.formatBasicDate(block.date, Utils.DISPLAY_DATE_FORMAT_MEDIUM);
        getActivity().setTitle(dateStr + " Block " + block.type);
    }

    private class GetBlockTask extends AsyncTask<Void, Object, EighthBlock> {
        private Exception exception = null;

        @Override
        protected EighthBlock doInBackground(Void... params) {
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
            Pair<EighthBlock, Integer> blockPair;

            try {
                stream = IodineApiHelper.getBlock(bid, authToken);

                parser = new IodineEighthParser(getActivity());
                blockPair = parser.beginGetBlock(stream);

                Pair<EighthActv, EighthActvInstance> pair;
                while (!isCancelled()) {
                    pair = parser.nextActivity();

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
                    if (stream != null)
                        stream.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException when closing stream: " + e);
                }
            }

            return blockPair.first;
        }

        @Override
        protected void onProgressUpdate(Object... args) {
            mAdapter.add(new Pair<>((EighthActv)args[0], (EighthActvInstance)args[1]));
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
