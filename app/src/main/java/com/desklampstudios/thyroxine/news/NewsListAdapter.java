package com.desklampstudios.thyroxine.news;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.ViewHolderClickListener;

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
        return new ViewHolder(v, new ViewHolderClickListener() {
            @Override
            public void onClick(View v, int pos) {
                IodineNewsEntry entry = mDataset.get(pos);
                mListener.onItemClick(entry);
            }
        });
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        IodineNewsEntry entry = mDataset.get(position);

        holder.titleView.setText(entry.title);
        holder.publishedView.setText(DATE_FORMAT.format(entry.published));
        holder.contentView.setText(entry.contentSnippet);
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
        for (int i = size-1; i >= 0; i--) {
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
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public View view;
        public TextView titleView;
        public TextView publishedView;
        public TextView contentView;
        public ViewHolderClickListener mListener;

        public ViewHolder(View v, ViewHolderClickListener listener) {
            super(v);
            view = v;
            titleView = (TextView) v.findViewById(R.id.iodine_news_feed_title);
            publishedView = (TextView) v.findViewById(R.id.iodine_news_feed_published);
            contentView = (TextView) v.findViewById(R.id.iodine_news_feed_content);

            mListener = listener;
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mListener.onClick(v, getPosition());
        }
    }

    public static interface EntryClickListener {
        public void onItemClick(IodineNewsEntry entry);
    }
}
