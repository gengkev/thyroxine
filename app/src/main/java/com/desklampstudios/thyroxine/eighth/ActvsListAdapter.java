package com.desklampstudios.thyroxine.eighth;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class ActvsListAdapter extends RecyclerView.Adapter<ActvsListAdapter.ViewHolder> {
    private static final String TAG = ActvsListAdapter.class.getSimpleName();

    private final List<Pair<EighthActv, EighthActvInstance>> mDataset;
    private final ActvClickListener mListener;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ActvsListAdapter(ActvClickListener listener) {
        this.mDataset = new ArrayList<>();
        this.mListener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.actv_list_textview, parent, false);

        // set the view's size, margins, paddings and layout parameters

        final ViewHolder vh = new ViewHolder(v);
        vh.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = vh.getPosition();
                Pair<EighthActv, EighthActvInstance> pair = mDataset.get(pos);
                mListener.onActvClick(pair.second);
            }
        });
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Pair<EighthActv, EighthActvInstance> pair = mDataset.get(position);
        EighthActv actv = pair.first;
        EighthActvInstance actvInstance = pair.second;

        holder.mNameView.setText(actv.name);
        holder.mRoomView.setText(actvInstance.roomsStr);

        String description = String.format("%s %s",
                actvInstance.comment, actv.description)
                .trim();
        holder.mDescriptionView.setText(description);

        ArrayList<String> statuses = new ArrayList<>();
        int color = (position % 2 == 0) ? 0x7FF8F8F8 : 0x7FFAFAFA;

        // restricted
        if ((actv.flags & EighthActv.FLAG_RESTRICTED) != 0) {
            statuses.add("(R)");
            color = (position % 2 == 0) ? 0x7FFFCCAA : 0x7FFDC5A0;
        }
        // sticky
        if ((actv.flags & EighthActv.FLAG_STICKY) != 0) {
            statuses.add("(S)");
        }
        // capacity full
        if (actvInstance.memberCount >= actvInstance.capacity) {
            statuses.add("<font color=\"#0000FF\">FULL</font>");
        }
        // cancelled
        if ((actvInstance.flags & EighthActvInstance.FLAG_CANCELLED) != 0) {
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

    public void add(Pair<EighthActv, EighthActvInstance> pair) {
        add(mDataset.size(), pair);
    }

    public void add(int pos, Pair<EighthActv, EighthActvInstance> pair) {
        mDataset.add(pos, pair);
        notifyItemInserted(pos);
    }

    public void addAll(Collection<Pair<EighthActv, EighthActvInstance>> pairList) {
        int size = mDataset.size();
        mDataset.addAll(pairList);
        notifyItemRangeInserted(size, size + pairList.size());
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
        @NonNull public final View mView;
        @NonNull public final TextView mNameView;
        @NonNull public final TextView mRoomView;
        @NonNull public final TextView mDescriptionView;
        @NonNull public final TextView mStatusView;

        public ViewHolder(@NonNull View v) {
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
