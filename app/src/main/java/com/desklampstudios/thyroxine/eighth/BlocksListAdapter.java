package com.desklampstudios.thyroxine.eighth;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;

import java.util.ArrayList;

class BlocksListAdapter extends CursorAdapter {
    private static final String TAG = BlocksListAdapter.class.getSimpleName();

    private Context mContext;

    public BlocksListAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        mContext = context;
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
        final Resources resources = mContext.getResources();
        final ViewHolder holder = (ViewHolder) view.getTag();

        ContentValues values = Utils.cursorRowToContentValues(cursor);
        // Log.d(TAG, "Values: " + values);

        EighthBlock block = EighthContract.Blocks.fromContentValues(values);

        int actvId = -1;
        try {
            actvId = values.getAsInteger(EighthContract.Schedule.KEY_ACTV_ID);
        } catch (NullPointerException e) { // it'll just be -1
            Log.w(TAG, "NullPointerException getting activity ID: " + e);
        }

        long actvFlags = 0;
        long actvInstanceFlags = 0;
        try {
            actvFlags = values.getAsLong(EighthContract.Actvs.KEY_FLAGS);
            actvInstanceFlags = values.getAsLong(EighthContract.ActvInstances.KEY_FLAGS);
        } catch (NullPointerException e) { // they'll just be 0
            Log.w(TAG, "NullPointerException getting actvFlags or actvInstanceFlags: " + e);
        }

        //String blockStr = mContext.getResources().getString(R.string.block_title, block.type);
        holder.mBlockView.setText(block.type);

        CharSequence name = values.getAsString(EighthContract.Actvs.KEY_NAME);
        if (actvId == 999) {
            name = Html.fromHtml(resources.getString(R.string.actv_not_selected));
        } else if (name == null) {
            name = resources.getString(R.string.actv_id_placeholder, actvId);
        }
        holder.mActivityNameView.setText(name);

        String roomsStr = values.getAsString(EighthContract.ActvInstances.KEY_ROOMS_STR);
        if (roomsStr == null) {
            roomsStr = mContext.getString(R.string.error);
        } else if (roomsStr.isEmpty()) {
            roomsStr = mContext.getString(R.string.actvInstance_rooms_placeholder);
        }
        holder.mActivityRoomsView.setText(roomsStr);

        long allFlags = actvFlags | actvInstanceFlags;

        // Display statuses
        String statusText = getBlockStatuses(resources, allFlags, block.locked);
        holder.mStatusView.setText(Html.fromHtml(statusText));
    }

    public String getBlockStatuses(final Resources resources, long flags, boolean locked) {
        ArrayList<String> statuses = new ArrayList<>();

        // locked
        if (locked) {
            statuses.add(resources.getString(R.string.block_status_locked));
        }
        // restricted
        if ((flags & EighthActv.FLAG_RESTRICTED) != 0) {
            int textColor = resources.getColor(R.color.actv_textColor_restricted);
            statuses.add(resources.getString(R.string.actv_status_restricted,
                    Utils.colorToHtmlHex(textColor)));
        }
        // sticky
        if ((flags & EighthActv.FLAG_STICKY) != 0) {
            int textColor = resources.getColor(R.color.actv_textColor_sticky);
            statuses.add(resources.getString(R.string.actv_status_sticky,
                    Utils.colorToHtmlHex(textColor)));
        }
        // cancelled
        if ((flags & EighthActvInstance.FLAG_CANCELLED) != 0) {
            int textColor = resources.getColor(R.color.actvInstance_textColor_cancelled);
            statuses.clear(); // clear other statuses
            statuses.add(resources.getString(R.string.actvInstance_status_cancelled,
                    Utils.colorToHtmlHex(textColor)));
        }

        return Utils.join(statuses, ", ");
    }

    // Provide a reference to the views for each data item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        @NonNull public final View mView;
        @NonNull public final TextView mBlockView;
        @NonNull public final TextView mActivityNameView;
        @NonNull public final TextView mStatusView;
        @NonNull public final TextView mActivityRoomsView;

        public ViewHolder(@NonNull View v) {
            super(v);
            mView = v;
            mBlockView = (TextView) v.findViewById(R.id.eighth_block);
            mActivityNameView = (TextView) v.findViewById(R.id.eighth_activity_name);
            mStatusView = (TextView) v.findViewById(R.id.eighth_activity_status);
            mActivityRoomsView = (TextView) v.findViewById(R.id.eighth_activity_rooms);
        }
    }
}
