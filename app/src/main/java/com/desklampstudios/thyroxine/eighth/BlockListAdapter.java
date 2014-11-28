package com.desklampstudios.thyroxine.eighth;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;

import java.util.List;

public class BlockListAdapter extends RecyclerView.Adapter<BlockListAdapter.ViewHolder> {
    private List<IodineEighthActv> mDataset;
    private ActvClickListener mListener;

    // Provide a suitable constructor (depends on the kind of dataset)
    public BlockListAdapter(List<IodineEighthActv> dataset, ActvClickListener listener) {
        this.mDataset = dataset;
        this.mListener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_list_textview, parent, false);

        // set the view's size, margins, paddings and layout parameters

        final ViewHolder vh = new ViewHolder(v);
        vh.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = vh.getPosition();
                IodineEighthActv actv = mDataset.get(pos);
                mListener.onActvClick(actv);
            }
        });
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        IodineEighthActv actv = mDataset.get(position);

        holder.mNameView.setText(actv.name);
        holder.mRoomView.setText(actv.roomsStr);
        holder.mDescriptionView.setText(actv.description);

        if (actv.getFlag(IodineEighthActv.ActivityFlag.CANCELLED)) {
            // cancelled
            holder.mStatusView.setText("CANCELLED");
            holder.mStatusView.setVisibility(View.VISIBLE);
        } else if (actv.getFlag(IodineEighthActv.ActivityFlag.ROOMCHANGED)) {
            // room changed
            holder.mStatusView.setText("ROOM CHANGED");
            holder.mStatusView.setVisibility(View.VISIBLE);
        } else if (actv.getFlag(IodineEighthActv.ActivityFlag.RESTRICTED)) {
            // restricted
            holder.mStatusView.setText("(R)");
            holder.mStatusView.setVisibility(View.VISIBLE);
        } else if (actv.aid == IodineEighthActv.NOT_SELECTED_AID) {
            // not selected
            holder.mStatusView.setText("(S)");
            holder.mStatusView.setVisibility(View.VISIBLE);
        } else {
            holder.mStatusView.setVisibility(View.INVISIBLE);
            holder.mStatusView.setText("");
        }

    }

    public void add(IodineEighthActv actv) {
        add(mDataset.size(), actv);
    }

    public void add(int pos, IodineEighthActv actv) {
        mDataset.add(pos, actv);
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
        public TextView mNameView;
        public TextView mRoomView;
        public TextView mDescriptionView;
        public TextView mStatusView;

        public ViewHolder(View v) {
            super(v);
            mView = v;
            mNameView = (TextView) v.findViewById(R.id.iodine_eighth_activity_name);
            mRoomView = (TextView) v.findViewById(R.id.iodine_eighth_activity_room);
            mDescriptionView = (TextView) v.findViewById(R.id.iodine_eighth_activity_description);
            mStatusView = (TextView) v.findViewById(R.id.iodine_eighth_activity_status);
        }
    }

    public static interface ActvClickListener {
        public void onActvClick(IodineEighthActv actv);
    }
}
