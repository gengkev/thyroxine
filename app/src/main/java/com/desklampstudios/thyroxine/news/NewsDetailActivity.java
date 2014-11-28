package com.desklampstudios.thyroxine.news;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;

import java.text.DateFormat;
import java.util.Date;


public class NewsDetailActivity extends ActionBarActivity {
    private static final String TAG = NewsDetailActivity.class.getSimpleName();
    private static final DateFormat DATE_FORMAT =
            DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT); // default locale OK

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_detail);

        // use Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // enable Up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView title = (TextView) findViewById(R.id.iodine_news_feed_title);
        TextView published = (TextView) findViewById(R.id.iodine_news_feed_published);
        TextView content = (TextView) findViewById(R.id.iodine_news_feed_content);

        Intent intent = getIntent();
        IodineNewsEntry entry = (IodineNewsEntry) intent.getSerializableExtra(NewsFragment.EXTRA_ENTRY);

        Log.d(TAG, "Entry: " + entry);

        setTitle(entry.title);
        title.setText(entry.title);
        published.setText(DATE_FORMAT.format(new Date(entry.published)));
        content.setText(entry.contentParsed);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.entry_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
