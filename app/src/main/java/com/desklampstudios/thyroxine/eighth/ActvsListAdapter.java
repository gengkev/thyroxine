package com.desklampstudios.thyroxine.eighth;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class ActvsListAdapter extends RecyclerView.Adapter<ActvsListAdapter.ViewHolder> {
    private static final String TAG = ActvsListAdapter.class.getSimpleName();

    private final List<Pair<EighthActv, EighthActvInstance>> mDataset;
    private final Context mContext;
    private OnItemClickListener mListener;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ActvsListAdapter(Context context) {
        this.mDataset = new ArrayList<>();
        this.mContext = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.actv_list_textview, parent, false);

        // set the view's size, margins, paddings and layout parameters

        final ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final Resources resources = mContext.getResources();

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

        int color = resources.getColor((position % 2 == 0) ?
                R.color.actv_background_default_1 :
                R.color.actv_background_default_2);

        // restricted
        if ((actv.flags & EighthActv.FLAG_RESTRICTED) != 0) {
            int textColor = resources.getColor(R.color.actv_textColor_restricted);
            statuses.add(resources.getString(R.string.actv_status_restricted,
                    Utils.colorToHtmlHex(textColor)));
            color = resources.getColor((position % 2 == 0) ?
                    R.color.actv_background_restricted_1 :
                    R.color.actv_background_restricted_2);
        }
        // sticky
        if ((actv.flags & EighthActv.FLAG_STICKY) != 0) {
            int textColor = resources.getColor(R.color.actv_textColor_sticky);
            statuses.add(resources.getString(R.string.actv_status_sticky,
                    Utils.colorToHtmlHex(textColor)));
        }
        // capacity full
        if (actvInstance.memberCount >= actvInstance.capacity) {
            int textColor = resources.getColor(R.color.actvInstance_textColor_full);
            statuses.add(resources.getString(R.string.actvInstance_status_full,
                    Utils.colorToHtmlHex(textColor)));
        }
        // cancelled
        if ((actvInstance.flags & EighthActvInstance.FLAG_CANCELLED) != 0) {
            int textColor = resources.getColor(R.color.actvInstance_textColor_cancelled);
            statuses.clear(); // clear other statuses
            statuses.add(resources.getString(R.string.actvInstance_status_cancelled,
                    Utils.colorToHtmlHex(textColor)));
            color = resources.getColor((position % 2 == 0) ?
                    R.color.actvInstance_background_cancelled_1 :
                    R.color.actvInstance_background_cancelled_2);
        }


        // display statuses
        if (statuses.size() > 0) {
            String statusText = Utils.join(statuses, ", ");
            holder.mStatusView.setText(Html.fromHtml(statusText));
        } else {
            holder.mStatusView.setText("");
        }

        // set background color
        holder.mView.setBackgroundColor(color);

        // set event listeners
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onItemClick(view, position);
                }
            }
        });
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return mListener != null && mListener.onItemLongClick(view, position);
            }
        });
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

    public Pair<EighthActv, EighthActvInstance> get(int pos) {
        return mDataset.get(pos);
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

    public interface OnItemClickListener {
        public void onItemClick(View view, int pos);
        public boolean onItemLongClick(View view, int pos);
    }
}
