package com.desklampstudios.thyroxine.eighth;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;

class BlocksListAdapter extends CursorAdapter {
    private static final String TAG = BlocksListAdapter.class.getSimpleName();

    public BlocksListAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.block_list_textview, parent, false);

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

        //Log.d(TAG, "Values: " + values);

        String dateStr = Utils.formatBasicDate(
                values.getAsString(EighthContract.Blocks.KEY_DATE),
                Utils.DISPLAY_DATE_FORMAT);
        holder.mDateView.setText(dateStr);

        holder.mBlockView.setText("Block " +
                values.getAsString(EighthContract.Blocks.KEY_TYPE));
        holder.mActivityNameView.setText(
                values.getAsInteger(EighthContract.ActvInstances.KEY_ACTV_ID) + " " +
                values.getAsString(EighthContract.Actvs.KEY_NAME));

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

    // Provide a reference to the views for each data item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        @NonNull public final View mView;
        @NonNull public final TextView mDateView;
        @NonNull public final TextView mBlockView;
        @NonNull public final TextView mActivityNameView;
        @NonNull public final TextView mStatusView;

        public ViewHolder(@NonNull View v) {
            super(v);
            mView = v;
            mDateView = (TextView) v.findViewById(R.id.eighth_date);
            mBlockView = (TextView) v.findViewById(R.id.eighth_block);
            mActivityNameView = (TextView) v.findViewById(R.id.eighth_activity_name);
            mStatusView = (TextView) v.findViewById(R.id.eighth_activity_status);
        }
    }
}
