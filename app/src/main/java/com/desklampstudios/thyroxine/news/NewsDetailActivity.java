package com.desklampstudios.thyroxine.news;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.desklampstudios.thyroxine.R;

import java.text.DateFormat;
import java.util.Date;


public class NewsDetailActivity extends ActionBarActivity {
    private static final String TAG = NewsDetailActivity.class.getSimpleName();
    private static final DateFormat DATE_FORMAT =
            DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT); // default locale OK

    private NewsEntry mNewsEntry;
    private ShareActionProvider mShareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_detail);

        // use Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // enable Up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // no title
        setTitle("");

        TextView title = (TextView) findViewById(R.id.news_entry_title);
        TextView published = (TextView) findViewById(R.id.news_entry_published);
        //TextView content = (TextView) findViewById(R.id.news_entry_content);
        WebView webView = (WebView) findViewById(R.id.news_entry_webview);

        Intent intent = getIntent();
        long id = intent.getLongExtra(NewsFragment.EXTRA_NEWS_ID, -1);

        // load from db
        Cursor cursor = getContentResolver().query(NewsProvider.CONTENT_URI_NEWS,
                null, // projection
                NewsDbHelper.KEY_NEWS_ID + " = ?", // selection
                new String[]{ String.valueOf(id) }, // selectionArgs
                null // sortOrder
        );
        if (cursor == null || !cursor.moveToNext()) {
            Toast.makeText(this, "Error loading from database", Toast.LENGTH_LONG).show();
            return;
        }
        mNewsEntry = NewsDbHelper.cursorRowToNewsEntry(cursor);

        Log.d(TAG, "Entry: " + mNewsEntry);

        title.setText(mNewsEntry.title);
        published.setText(DATE_FORMAT.format(new Date(mNewsEntry.published)));
        //content.setText(Html.fromHtml(mNewsEntry.contentRaw));
        webView.loadData(mNewsEntry.contentRaw, "text/html;charset=utf-8", null);
        webView.setBackgroundColor(getResources().getColor(R.color.background));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.entry_detail, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        setShareIntent(createShareIntent());

        return true;
    }

    private Intent createShareIntent() {
        if (mNewsEntry == null)
            return null;

        String shareSubject = getResources().getString(
                R.string.news_entry_share_subject, mNewsEntry.title);
        String shareMessage = getResources().getString(
                R.string.news_entry_share_body, mNewsEntry.title, mNewsEntry.link);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);

        return shareIntent;
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.news_entry_browser:
                // open in browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mNewsEntry.link));
                startActivity(browserIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
