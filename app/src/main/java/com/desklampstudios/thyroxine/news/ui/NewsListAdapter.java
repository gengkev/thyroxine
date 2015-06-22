package com.desklampstudios.thyroxine.news.ui;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.news.provider.NewsContract;

class NewsListAdapter extends CursorAdapter {
    private static final String TAG = NewsListAdapter.class.getSimpleName();

    private final Context mContext;

    public NewsListAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        mContext = context;
    }

    @Override
    public View newView(@NonNull Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.news_list_textview, parent, false);

        // yay ViewHolders!
        final ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);

        return view;
    }

    @Override
    public void bindView(@NonNull View view, Context context, Cursor cursor) {
        ContentValues values = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, values);
        ViewHolder holder = (ViewHolder) view.getTag();

        holder.mTitleView.setText(
                values.getAsString(NewsContract.NewsEntries.KEY_TITLE));
        holder.mPublishedView.setText(Utils.DateFormats.MED_DAYMONTH.format(mContext,
                values.getAsLong(NewsContract.NewsEntries.KEY_PUBLISHED)));
        holder.mSnippetView.setText(
                values.getAsString(NewsContract.NewsEntries.KEY_CONTENT_SNIPPET));
    }

    // Provide a reference to the views for each data item
    // There is a 0.000% need to continue extending RecyclerView.ViewHolder but why not.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        @NonNull public final View mView;
        @NonNull public final TextView mTitleView;
        @NonNull public final TextView mPublishedView;
        @NonNull public final TextView mSnippetView;

        public ViewHolder(@NonNull View v) {
            super(v);
            mView = v;
            mTitleView = (TextView) v.findViewById(R.id.news_entry_title);
            mPublishedView = (TextView) v.findViewById(R.id.news_entry_published);
            mSnippetView = (TextView) v.findViewById(R.id.news_entry_snippet);
        }
    }
}
