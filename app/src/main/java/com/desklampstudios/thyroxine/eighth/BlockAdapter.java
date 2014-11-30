package com.desklampstudios.thyroxine.eighth;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;

import java.util.ArrayList;
import java.util.List;

public class BlockAdapter extends RecyclerView.Adapter<BlockAdapter.ViewHolder> {
    private List<EighthActvInstance> mDataset;
    private ActvClickListener mListener;

    // Provide a suitable constructor (depends on the kind of dataset)
    public BlockAdapter(List<EighthActvInstance> dataset, ActvClickListener listener) {
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
                EighthActvInstance actv = mDataset.get(pos);
                mListener.onActvClick(actv);
            }
        });
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        EighthActvInstance actvInstance = mDataset.get(position);

        holder.mNameView.setText(actvInstance.actv.name);
        holder.mRoomView.setText(actvInstance.roomsStr);
        holder.mDescriptionView.setText((
                actvInstance.comment + " " +
                actvInstance.actv.description).trim());

        ArrayList<String> statuses = new ArrayList<String>();
        int color = (position % 2 == 0) ? 0x7FF8F8F8 : 0x7FFAFAFA;

        // restricted
        if ((actvInstance.getFlags() & EighthActv.FLAG_RESTRICTED) != 0) {
            statuses.add("(R)");
            color = (position % 2 == 0) ? 0x7FFFCCAA : 0x7FFDC5A0;
        }
        // sticky
        if ((actvInstance.getFlags() & EighthActv.FLAG_STICKY) != 0) {
            statuses.add("(S)");
        }
        // capacity full
        if (actvInstance.memberCount != null && actvInstance.capacity != null &&
                actvInstance.memberCount >= actvInstance.capacity) {
            statuses.add("<font color=\"#0000FF\">FULL</font>");
        }
        // cancelled
        if ((actvInstance.getFlags() & EighthActvInstance.FLAG_CANCELLED) != 0) {
            statuses.add("<font color=\"#FF0000\">CANCELLED</font>");
            color = (position % 2 == 0) ? 0x7FCF0000 : 0x7FCA0000;
        }

        // display statuses
        if (statuses.size() > 0) {
            String statusText = statuses.toString().replaceAll("[\\[\\]]", "");
            holder.mStatusView.setText(Html.fromHtml(statusText));
        } else {
            holder.mStatusView.setText("");
        }

        // set background color
        holder.mView.setBackgroundColor(color);
    }

    public void add(EighthActvInstance actv) {
        add(mDataset.size(), actv);
    }

    public void add(int pos, EighthActvInstance actv) {
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
        public void onActvClick(EighthActvInstance actv);
    }
}
