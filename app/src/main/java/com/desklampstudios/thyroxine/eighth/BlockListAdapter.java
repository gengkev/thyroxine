package com.desklampstudios.thyroxine.eighth;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;

class BlockListAdapter extends CursorAdapter {
    private static final String TAG = BlockListAdapter.class.getSimpleName();

    public BlockListAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.actv_list_textview, parent, false);

        // yay ViewHolders!
        final ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);

        return view;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ContentValues values = Utils.cursorRowToContentValues(cursor);
        ViewHolder holder = (ViewHolder) view.getTag();

        holder.mNameView.setText(
                values.getAsString(EighthContract.Actvs.KEY_NAME));

        holder.mRoomView.setText(
                values.getAsString(EighthContract.ActvInstances.KEY_ROOMS_STR));

        String description = String.format("%s %s",
                values.getAsString(EighthContract.ActvInstances.KEY_COMMENT),
                values.getAsString(EighthContract.Actvs.KEY_DESCRIPTION));
        description = description.trim();
        holder.mDescriptionView.setText(description);

        /*
        ArrayList<String> statuses = new ArrayList<>();
        int color = (position % 2 == 0) ? 0x7FF8F8F8 : 0x7FFAFAFA;

        // restricted
        if ((pair.first.flags & EighthActv.FLAG_RESTRICTED) != 0) {
            statuses.add("(R)");
            color = (position % 2 == 0) ? 0x7FFFCCAA : 0x7FFDC5A0;
        }
        // sticky
        if ((pair.first.flags & EighthActv.FLAG_STICKY) != 0) {
            statuses.add("(S)");
        }
        // capacity full
        if (pair.second.memberCount >= pair.second.capacity) {
            statuses.add("<font color=\"#0000FF\">FULL</font>");
        }
        // cancelled
        if ((pair.second.flags & EighthActvInstance.FLAG_CANCELLED) != 0) {
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
        */
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mNameView;
        public final TextView mRoomView;
        public final TextView mDescriptionView;
        public final TextView mStatusView;

        public ViewHolder(View v) {
            super(v);
            mView = v;
            mNameView = (TextView) v.findViewById(R.id.iodine_eighth_activity_name);
            mRoomView = (TextView) v.findViewById(R.id.iodine_eighth_activity_room);
            mDescriptionView = (TextView) v.findViewById(R.id.iodine_eighth_activity_description);
            mStatusView = (TextView) v.findViewById(R.id.iodine_eighth_activity_status);
        }
    }
}
