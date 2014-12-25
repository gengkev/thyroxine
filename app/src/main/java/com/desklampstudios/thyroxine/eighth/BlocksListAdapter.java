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

        ContentValues values = Utils.cursorRowToContentValues(cursor);
        ViewHolder holder = (ViewHolder) view.getTag();

        Log.d(TAG, "Values: " + values);

        EighthBlock block = EighthContract.Blocks.fromContentValues(values);

        String dateStr = Utils.formatBasicDate(block.date, Utils.DateFormats.FULL_DATE.get());
        String blockStr = mContext.getResources().getString(R.string.block_title, block.type);
        String nameStr = values.getAsString(EighthContract.Actvs.KEY_NAME);

        holder.mDateView.setText(dateStr);
        holder.mBlockView.setText(blockStr);

        int actvId;
        try {
            actvId = values.getAsInteger(EighthContract.Schedule.KEY_ACTV_ID);
        } catch (NullPointerException e) {
            Log.w(TAG, "NullPointerException getting activity ID: " + e);
            actvId = -1;
        }

        if (actvId == 999) {
            holder.mActivityNameView.setText(Html.fromHtml(
                    resources.getString(R.string.actv_not_selected)));
        }
        else if (nameStr != null) {
            holder.mActivityNameView.setText(nameStr);
        }
        else {
            holder.mActivityNameView.setText(
                    resources.getString(R.string.actv_id_placeholder, actvId));
        }

        // Display flags
        ArrayList<String> statuses = new ArrayList<>();

        long actvFlags = 0;
        long actvInstanceFlags = 0;
        try {
            actvFlags = values.getAsLong(EighthContract.Actvs.KEY_FLAGS);
            actvInstanceFlags = values.getAsLong(EighthContract.ActvInstances.KEY_FLAGS);
        } catch (NullPointerException e) {
            // this is okay; they'll just be 0
            Log.w(TAG, "NullPointerException getting actvFlags or actvInstanceFlags: " + e);
        }

        // locked
        if (block.locked) {
            statuses.add(resources.getString(R.string.block_status_locked));
        }
        // cancelled
        if ((actvInstanceFlags & EighthActvInstance.FLAG_CANCELLED) != 0) {
            int textColor = resources.getColor(R.color.actvInstance_textColor_cancelled);
            statuses.add(resources.getString(R.string.actvInstance_status_cancelled,
                    Utils.colorToHtmlHex(textColor)));
        }
        // restricted
        if ((actvFlags & EighthActv.FLAG_RESTRICTED) != 0) {
            int textColor = resources.getColor(R.color.actv_textColor_restricted);
            statuses.add(resources.getString(R.string.actv_status_restricted,
                    Utils.colorToHtmlHex(textColor)));
        }
        // sticky
        if ((actvFlags & EighthActv.FLAG_STICKY) != 0) {
            int textColor = resources.getColor(R.color.actv_textColor_sticky);
            statuses.add(resources.getString(R.string.actv_status_sticky,
                    Utils.colorToHtmlHex(textColor)));
        }

        // display statuses
        if (statuses.size() > 0) {
            holder.mStatusView.setPaddingRelative(0, 0, 8, 0);

            String statusText = "(" + Utils.join(statuses, ", ") + ")";
            holder.mStatusView.setText(Html.fromHtml(statusText));
        } else {
            holder.mStatusView.setPaddingRelative(0, 0, 0, 0);
            holder.mStatusView.setText("");
        }
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
