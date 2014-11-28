package com.desklampstudios.thyroxine.eighth;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class EighthListAdapter extends RecyclerView.Adapter<EighthListAdapter.ViewHolder> {
    private static final String TAG = EighthListAdapter.class.getSimpleName();
    private static DateFormat DATE_FORMAT =
            DateFormat.getDateInstance(DateFormat.FULL); // default locale OK

    private List<IodineEighthBlock> mDataset;
    private BlockClickListener mListener;

    public EighthListAdapter(List<IodineEighthBlock> dataset, BlockClickListener listener) {
        this.mDataset = dataset;
        this.mListener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.eighth_list_textview, parent, false);

        // set the view's size, margins, paddings and layout parameters

        final ViewHolder vh = new ViewHolder(v);
        vh.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = vh.getPosition();
                IodineEighthBlock block = mDataset.get(pos);
                mListener.onBlockClick(block);
            }
        });
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        IodineEighthBlock block = mDataset.get(position);
        IodineEighthActv actv = block.currentActv;

        holder.mDateView.setText(DATE_FORMAT.format(new Date(block.date)));
        holder.mBlockView.setText("Block " + block.type);
        holder.mActivityNameView.setText(block.currentActv.name);

        ArrayList<String> statuses = new ArrayList<String>();
        if (actv.getFlag(IodineEighthActv.ActivityFlag.CANCELLED)) {
            // cancelled
            statuses.add("CANCELLED");
        }
        if (actv.getFlag(IodineEighthActv.ActivityFlag.ROOMCHANGED)) {
            // room changed
            statuses.add("ROOM CHANGED");
        }
        if (actv.aid == IodineEighthActv.NOT_SELECTED_AID) {
            // not selected (mainly for testing)
            statuses.add("NOT SELECTED");
        }

        if (statuses.size() > 0) {
            holder.mStatusView.setPaddingRelative(0, 0, 8, 0);
            holder.mStatusView.setText(statuses.toString().replaceAll("[\\[\\]]", ""));
        } else {
            holder.mStatusView.setPaddingRelative(0, 0, 0, 0);
            holder.mStatusView.setText("");
        }
    }

    public void add(IodineEighthBlock block) {
        add(mDataset.size(), block);
    }

    public void add(int pos, IodineEighthBlock block) {
        mDataset.add(pos, block);
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
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View mView;
        public TextView mDateView;
        public TextView mBlockView;
        public TextView mActivityNameView;
        public TextView mStatusView;

        public ViewHolder(View v) {
            super(v);
            mView = v;
            mDateView = (TextView) v.findViewById(R.id.eighth_date);
            mBlockView = (TextView) v.findViewById(R.id.eighth_block);
            mActivityNameView = (TextView) v.findViewById(R.id.eighth_activity_name);
            mStatusView = (TextView) v.findViewById(R.id.eighth_activity_status);
        }
    }

    public static interface BlockClickListener {
        public void onBlockClick(IodineEighthBlock block);
    }
}
