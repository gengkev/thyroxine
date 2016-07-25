package com.desklampstudios.thyroxine.eighth.ui;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.eighth.model.EighthActv;
import com.desklampstudios.thyroxine.eighth.model.EighthActvInstance;
import com.desklampstudios.thyroxine.eighth.model.EighthBlockAndActv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

class ActvsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ActvsListAdapter.class.getSimpleName();
    private static final int TYPE_ITEM = 1;

    @NonNull private final List<EighthBlockAndActv> mDataset;
    @NonNull private final Context mContext;
    @Nullable private OnItemClickListener mListener = null;

    private int mSelectedActvId = -1;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ActvsListAdapter(@NonNull Context context) {
        this.mDataset = new ArrayList<>();
        this.mContext = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    public void setSelectedActvId(int actvId) {
        this.mSelectedActvId = actvId;
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.actv_list_textview, parent, false);
            return new ViewHolder(v);
        }
        else {
            Log.wtf(TAG, "Invalid view type");
            throw new IllegalArgumentException();
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int actualPosition) {
        final int viewType = getItemViewType(actualPosition);

        if (viewType == TYPE_ITEM) {
            final ViewHolder itemHolder = (ViewHolder) holder;

            EighthBlockAndActv pair = getItem(actualPosition);
            EighthActv actv = pair.actv;
            EighthActvInstance actvInstance = pair.actvInstance;

            itemHolder.mNameView.setText(formatName(actv));
            itemHolder.mDescriptionView.setText(formatDescription(actv, actvInstance));
            itemHolder.mGroupView.setText(formatGroup(actv, actualPosition));

            long allFlags = actv.flags | actvInstance.flags;
            boolean full = actvInstance.isFull();

            // display statuses
            String statusText = getActvStatuses(allFlags, full);
            itemHolder.mStatusView.setText(Html.fromHtml(statusText));

            // set background color
            int color = getActvColor(actualPosition, allFlags);
            itemHolder.mView.findViewById(R.id.eighth_activity_content).setBackgroundColor(color);

            // set event listeners
            itemHolder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) mListener.onItemClick(view, itemHolder.getAdapterPosition());
                }
            });
            itemHolder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return mListener != null && mListener.onItemLongClick(view, itemHolder.getAdapterPosition());
                }
            });
        }
        else {
            Log.wtf(TAG, "Invalid view type");
        }
    }

    private int getActvColor(int position, long flags) {
        int color = 0;
/*
        // restricted
        if ((flags & EighthActv.FLAG_RESTRICTED) != 0) {
            color = resources.getColor((position % 2 == 0) ?
                    R.color.actv_background_restricted_1 :
                    R.color.actv_background_restricted_2);
        }
*/
        // cancelled
        if ((flags & EighthActvInstance.FLAG_CANCELLED) != 0) {
            color = ContextCompat.getColor(mContext, (position % 2 == 0) ?
                    R.color.actvInstance_background_cancelled_1 :
                    R.color.actvInstance_background_cancelled_2);
        }
        return color;
    }

    @NonNull
    private String getActvStatuses(long flags, boolean full) {
        ArrayList<String> statuses = new ArrayList<>();
        final String fontColor = mContext.getString(R.string.font_color);

        // sticky (always? implies restricted)
        if ((flags & EighthActv.FLAG_STICKY) != 0) {
            int textColor = ContextCompat.getColor(mContext, R.color.actv_textColor_sticky);
            statuses.add(String.format(fontColor,
                    mContext.getString(R.string.actv_status_sticky),
                    Utils.colorToHtmlHex(textColor)));
        }
        // restricted
        else if ((flags & EighthActv.FLAG_RESTRICTED) != 0) {
            int textColor = ContextCompat.getColor(mContext, R.color.actv_textColor_restricted);
            statuses.add(String.format(fontColor,
                    mContext.getString(R.string.actv_status_restricted),
                    Utils.colorToHtmlHex(textColor)));
        }

        // full
        if (full) {
            int textColor = ContextCompat.getColor(mContext, R.color.actvInstance_textColor_full);
            statuses.add(String.format(fontColor,
                    mContext.getString(R.string.actvInstance_status_full),
                    Utils.colorToHtmlHex(textColor)));
        }

        // cancelled
        if ((flags & EighthActvInstance.FLAG_CANCELLED) != 0) {
            int textColor = ContextCompat.getColor(mContext, R.color.actvInstance_textColor_cancelled);
            statuses.clear(); // clear other statuses
            statuses.add(mContext.getString(R.string.font_color,
                    mContext.getString(R.string.actvInstance_status_cancelled),
                    Utils.colorToHtmlHex(textColor)));
        }

        return Utils.join(statuses, ", ");
    }

    private String formatName(EighthActv actv) {
        String name = actv.name;
        if (actv.actvId == mSelectedActvId) {
            name = "* " + name;
        }
        return name;
    }
    private String formatDescription(EighthActv actv, EighthActvInstance actvInstance) {
        if (!actvInstance.comment.isEmpty()) {
            String out = String.format("(%s) %s", actvInstance.comment, actv.description);
            return out.trim();
        }
        else if (!actv.description.isEmpty()) {
            return actv.description.trim();
        }
        else {
            return mContext.getString(R.string.actv_description_placeholder);
        }
    }
    private String formatGroup(EighthActv actv, int position) {
        String group = getGroup(actv);
        String prevGroup = "";
        if (position > 0) {
            EighthBlockAndActv prevPair = getItem(position - 1);
            prevGroup = getGroup(prevPair.actv);
        }

        if (!group.equals(prevGroup)) {
            return group;
        } else {
            return "";
        }
    }
    private static String getGroup(EighthActv actv) {
        char letter = actv.name.toUpperCase(Locale.ENGLISH).charAt(0);

        if ((actv.flags & EighthActv.FLAG_SPECIAL) != 0) {
            return "~";
        } else if (Character.isLetter(letter)) {
            return String.valueOf(letter);
        } else if (Character.isDigit(letter)) {
            return "#";
        } else {
            return "?";
        }
    }

    public void addItem(@NonNull EighthBlockAndActv pair) {
        addItem(mDataset.size(), pair);
    }

    public void addItem(int pos, @NonNull EighthBlockAndActv pair) {
        mDataset.add(pos, pair);
        notifyItemInserted(pos);
    }

    public EighthBlockAndActv getItem(int pos) {
        return mDataset.get(pos);
    }

    public void replaceAllItems(@NonNull Collection<EighthBlockAndActv> pairList) {
        int oldSize = mDataset.size();
        mDataset.clear();
        mDataset.addAll(pairList);

        notifyItemRangeRemoved(0, oldSize);
        notifyItemRangeInserted(0, pairList.size());
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
    @Override
    public int getItemViewType(int position) {
        return TYPE_ITEM;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        @NonNull public final View mView;
        @NonNull public final TextView mNameView;
        @NonNull public final TextView mDescriptionView;
        @NonNull public final TextView mStatusView;
        @NonNull public final TextView mGroupView;
        @NonNull public final View mContentView;

        public ViewHolder(@NonNull View v) {
            super(v);
            mView = v;
            mNameView = (TextView) v.findViewById(R.id.iodine_eighth_activity_name);
            mDescriptionView = (TextView) v.findViewById(R.id.iodine_eighth_activity_description);
            mStatusView = (TextView) v.findViewById(R.id.iodine_eighth_activity_status);
            mGroupView = (TextView) v.findViewById(R.id.eighth_activity_group);
            mContentView = v.findViewById(R.id.eighth_activity_content);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int pos);
        boolean onItemLongClick(View view, int pos);
    }
}
