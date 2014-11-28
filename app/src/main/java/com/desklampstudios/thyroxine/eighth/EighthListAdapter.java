package com.desklampstudios.thyroxine.eighth;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.ViewHolderClickListener;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        return new ViewHolder(v, new ViewHolderClickListener() {
            @Override
            public void onClick(View v, int pos) {
                IodineEighthBlock block = mDataset.get(pos);
                mListener.onBlockClick(block);
            }
        });
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        IodineEighthBlock block = mDataset.get(position);
        IodineEighthActv actv = block.currentActv;

        holder.dateView.setText(DATE_FORMAT.format(new Date(block.date)));
        holder.blockView.setText("Block " + block.type);
        holder.activityNameView.setText(block.currentActv.name);

        if (actv.getFlag(IodineEighthActv.ActivityFlag.CANCELLED)) {
            // cancelled
            holder.mStatusView.setVisibility(View.VISIBLE);
            holder.mStatusView.setText("CANCELLED");
        }
        else if (actv.getFlag(IodineEighthActv.ActivityFlag.ROOMCHANGED)) {
            // room changed
            holder.mStatusView.setVisibility(View.VISIBLE);
            holder.mStatusView.setText("ROOM CHANGED");
        }
        else if (actv.getFlag(IodineEighthActv.ActivityFlag.RESTRICTED)) {
            // restricted
            holder.mStatusView.setText("RESTRICTED");
        }
        else if (actv.aid == IodineEighthActv.NOT_SELECTED_AID) {
            // not selected
            holder.mStatusView.setVisibility(View.VISIBLE);
            holder.mStatusView.setText("NOT SELECTED");
        }
        else {
            holder.mStatusView.setVisibility(View.INVISIBLE);
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
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public View view;
        public TextView dateView;
        public TextView blockView;
        public TextView activityNameView;
        public TextView mStatusView;
        public ViewHolderClickListener mListener;

        public ViewHolder(View v, ViewHolderClickListener listener) {
            super(v);
            view = v;
            dateView = (TextView) v.findViewById(R.id.iodine_eighth_date);
            blockView = (TextView) v.findViewById(R.id.iodine_eighth_block);
            activityNameView = (TextView) v.findViewById(R.id.iodine_eighth_activity_name);
            mStatusView = (TextView) v.findViewById(R.id.iodine_eighth_activity_status);

            mListener = listener;
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mListener.onClick(v, getPosition());
        }
    }

    public static interface BlockClickListener {
        public void onBlockClick(IodineEighthBlock block);
    }
}
