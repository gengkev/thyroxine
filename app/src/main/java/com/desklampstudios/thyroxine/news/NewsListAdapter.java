package com.desklampstudios.thyroxine.news;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

class NewsListAdapter extends CursorAdapter {
    private static final String TAG = NewsListAdapter.class.getSimpleName();
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM");

    public NewsListAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.news_list_textview, parent, false);

        // yay ViewHolders!
        final ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ContentValues values = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, values);
        ViewHolder holder = (ViewHolder) view.getTag();

        holder.mTitleView.setText(
                values.getAsString(NewsDbHelper.KEY_NEWS_TITLE));
        holder.mPublishedView.setText(DATE_FORMAT.format(
                values.getAsLong(NewsDbHelper.KEY_NEWS_DATE)));
        holder.mSnippetView.setText(
                values.getAsString(NewsDbHelper.KEY_NEWS_SNIPPET));
    }

    // Provide a reference to the views for each data item
    // There is a 0.000% need to continue extending RecyclerView.ViewHolder but why not.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTitleView;
        public final TextView mPublishedView;
        public final TextView mSnippetView;

        public ViewHolder(View v) {
            super(v);
            mView = v;
            mTitleView = (TextView) v.findViewById(R.id.news_entry_title);
            mPublishedView = (TextView) v.findViewById(R.id.news_entry_published);
            mSnippetView = (TextView) v.findViewById(R.id.news_entry_snippet);
        }
    }
}
