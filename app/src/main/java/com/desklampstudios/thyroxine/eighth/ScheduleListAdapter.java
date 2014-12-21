package com.desklampstudios.thyroxine.eighth;

import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;

import java.util.ArrayList;
import java.util.List;

class ScheduleListAdapter extends RecyclerView.Adapter<ScheduleListAdapter.ViewHolder> {
    private static final String TAG = ScheduleListAdapter.class.getSimpleName();

    private final List<Pair<EighthBlock, Integer>> mDataset;
    private final BlockClickListener mListener;

    public ScheduleListAdapter(BlockClickListener listener) {
        this.mDataset = new ArrayList<>();
        this.mListener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.block_list_textview, parent, false);

        // set the view's size, margins, paddings and layout parameters

        final ViewHolder vh = new ViewHolder(v);
        vh.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = vh.getPosition();
                Pair<EighthBlock, Integer> pair = mDataset.get(pos);
                mListener.onBlockClick(pair.first);
            }
        });
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Pair<EighthBlock, Integer> pair = mDataset.get(position);
        EighthBlock block = pair.first;
        //EighthActvInstance actvInstance = block.selectedActv;

        String dateStr = Utils.formatBasicDate(block.date, Utils.DISPLAY_DATE_FORMAT);
        holder.mDateView.setText(dateStr);
        holder.mBlockView.setText("Block " + block.type);
        //holder.mActivityNameView.setText(actvInstance.actv.name);
        holder.mActivityNameView.setText("<activity name>");

        /*
        ArrayList<String> statuses = new ArrayList<>();
        // cancelled
        if ((actvInstance.getFlags() & EighthActvInstance.FLAG_CANCELLED) != 0) {
            statuses.add("CANCELLED");
        }
        // not selected
        if (actvInstance.actv.aid == EighthActv.NOT_SELECTED_AID) {
            statuses.add("NOT SELECTED");
        }

        // display statuses
        if (statuses.size() > 0) {
            holder.mStatusView.setPaddingRelative(0, 0, 8, 0);
            holder.mStatusView.setText(statuses.toString().replaceAll("[\\[\\]]", ""));
        } else {
            holder.mStatusView.setPaddingRelative(0, 0, 0, 0);
            holder.mStatusView.setText("");
        }
        */
    }

    public void add(Pair<EighthBlock, Integer> pair) {
        add(mDataset.size(), pair);
    }

    public void add(int pos, Pair<EighthBlock, Integer> pair) {
        mDataset.add(pos, pair);
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
        public final View mView;
        public final TextView mDateView;
        public final TextView mBlockView;
        public final TextView mActivityNameView;
        public final TextView mStatusView;

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
        public void onBlockClick(EighthBlock block);
    }
}
