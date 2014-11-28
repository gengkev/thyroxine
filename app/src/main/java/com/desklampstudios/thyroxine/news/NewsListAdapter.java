package com.desklampstudios.thyroxine.news;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

class NewsListAdapter extends RecyclerView.Adapter<NewsListAdapter.ViewHolder> {
    private static final String TAG = NewsListAdapter.class.getSimpleName();
    private static DateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM");

    private List<IodineNewsEntry> mDataset;
    private EntryClickListener mListener;

    public NewsListAdapter(List<IodineNewsEntry> dataset, EntryClickListener listener) {
        mDataset = dataset;
        mListener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.news_list_textview, parent, false);

        // set the view's size, margins, paddings and layout parameters

        final ViewHolder vh = new ViewHolder(v);
        vh.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = vh.getPosition();
                IodineNewsEntry entry = mDataset.get(pos);
                mListener.onItemClick(entry);
            }
        });
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        IodineNewsEntry entry = mDataset.get(position);

        holder.mTitleView.setText(entry.title);
        holder.mPublishedView.setText(DATE_FORMAT.format(entry.published));
        holder.mContentView.setText(entry.contentSnippet);
    }

    public void add(IodineNewsEntry entry) {
        add(mDataset.size(), entry);
    }

    public void add(int pos, IodineNewsEntry entry) {
        mDataset.add(pos, entry);
        notifyItemInserted(pos);
    }

    public void clear() {
        int size = mDataset.size();
        for (int i = size - 1; i >= 0; i--) {
            mDataset.remove(i);
        }
        notifyItemRangeRemoved(0, size);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    // Provide a reference to the views for each data item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View mView;
        public TextView mTitleView;
        public TextView mPublishedView;
        public TextView mContentView;

        public ViewHolder(View v) {
            super(v);
            mView = v;
            mTitleView = (TextView) v.findViewById(R.id.iodine_news_feed_title);
            mPublishedView = (TextView) v.findViewById(R.id.iodine_news_feed_published);
            mContentView = (TextView) v.findViewById(R.id.iodine_news_feed_content);
        }
    }

    public static interface EntryClickListener {
        public void onItemClick(IodineNewsEntry entry);
    }
}
