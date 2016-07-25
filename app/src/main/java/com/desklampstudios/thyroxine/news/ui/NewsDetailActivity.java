package com.desklampstudios.thyroxine.news.ui;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.news.io.IodineNewsApi;
import com.desklampstudios.thyroxine.news.model.NewsEntry;
import com.desklampstudios.thyroxine.news.provider.NewsContract;

public class NewsDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = NewsDetailActivity.class.getSimpleName();

    private int mNewsId = -1;
    @Nullable private NewsEntry mNewsEntry = null;
    private ShareActionProvider mShareActionProvider;

    private TextView mTitleView;
    private TextView mPublishedView;
    private TextView mLikesView;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        // use Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // enable Up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // no title in action bar
        setTitle("");

        // read intent
        Intent intent = getIntent();
        mNewsId = intent.getIntExtra(NewsFragment.EXTRA_NEWS_ID, -1);

        // reference views
        mTitleView = (TextView) findViewById(R.id.news_entry_title);
        mPublishedView = (TextView) findViewById(R.id.news_entry_published);
        mLikesView = (TextView) findViewById(R.id.news_entry_likes);
        mWebView = (WebView) findViewById(R.id.news_entry_webview);

        mWebView.setBackgroundColor(ContextCompat.getColor(this, R.color.background));

        // initialize loader
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.entry_detail, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        return true;
    }

    @Nullable
    private Intent createShareIntent() {
        if (mNewsEntry == null)
            return null;

        String shareSubject = mNewsEntry.title;
        String shareMessage = mNewsEntry.title + "\n" +
                IodineNewsApi.getNewsShowUrl(mNewsEntry.newsId);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);

        return shareIntent;
    }

    // Call to update the share intent
    private void setShareIntent(@Nullable Intent shareIntent) {
        if (mShareActionProvider != null && shareIntent != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.news_entry_browser: {
                if (mNewsEntry != null) {
                    // open in browser
                    String link = IodineNewsApi.getNewsShowUrl(mNewsEntry.newsId);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(browserIntent);
                    return true;
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        // Workaround so that there aren't two action bars upon text selection
        getSupportActionBar().hide();

        super.onSupportActionModeStarted(mode);
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        // Workaround so that there aren't two action bars upon text selection
        getSupportActionBar().show();

        super.onSupportActionModeFinished(mode);
    }

    @Nullable
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        return new CursorLoader(
                this,
                NewsContract.NewsEntries.buildEntryUri(mNewsId),
                null, // projection
                null, // selection
                null, // selectionArgs
                null // orderBy
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, @Nullable Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            ContentValues values = Utils.cursorRowToContentValues(cursor);
            mNewsEntry = NewsContract.NewsEntries.fromContentValues(values);

            Log.d(TAG, "Entry: " + mNewsEntry);

            String published = Utils.DateFormats.FULL_DATETIME.format(this, mNewsEntry.published);
            String liked = getResources().getQuantityString(R.plurals.news_entry_liked,
                    mNewsEntry.numLikes, mNewsEntry.numLikes);

            // Update WebView
            mWebView.loadData(mNewsEntry.content, "text/html;charset=utf-8", null);

            // Update UI fields
            mTitleView.setText(mNewsEntry.title);
            mPublishedView.setText(published);
            mLikesView.setText(liked);

            // create share intent
            setShareIntent(createShareIntent());
        }
        else {
            Log.e(TAG, "Cursor error");
            Toast.makeText(this, R.string.error_database, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }
}
