package com.desklampstudios.thyroxine.news;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

class NewsContract {
    // Warning: for now, also declared in strings.xml
    public static final String CONTENT_AUTHORITY = "com.desklampstudios.thyroxine.news";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_NEWSENTRIES = "news";

    public static class NewsEntries implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_NEWSENTRIES).build();

        public static final String CONTENT_TYPE_NEWSENTRIES = ContentResolver.CURSOR_DIR_BASE_TYPE + "/newsEntries";
        public static final String CONTENT_ITEM_TYPE_NEWSENTRIES = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/newsEntry";

        static final String KEY_TITLE = "title";
        static final String KEY_DATE = "date";
        static final String KEY_LINK = "link";
        static final String KEY_CONTENT = "content";
        static final String KEY_SNIPPET = "content_snippet";

        @NonNull
        public static Uri buildEntryUri(String link) {
            return CONTENT_URI.buildUpon()
                    .appendPath(link)
                    .build();
        }
        @NonNull
        public static String getLink(@NonNull Uri uri) {
            return uri.getPathSegments().get(1);
        }

        // converting to and from not-really-pojos yooooo
        @NonNull
        static NewsEntry fromContentValues(@NonNull ContentValues values) {
            return new NewsEntry.Builder()
                    .link(values.getAsString(NewsEntries.KEY_LINK))
                    .title(values.getAsString(NewsEntries.KEY_TITLE))
                    .published(values.getAsLong(NewsEntries.KEY_DATE))
                    .contentRaw(values.getAsString(NewsEntries.KEY_CONTENT))
                    .contentSnippet(values.getAsString(NewsEntries.KEY_SNIPPET))
                    .build();
        }
        @NonNull
        static ContentValues toContentValues(@NonNull NewsEntry entry) {
            ContentValues values = new ContentValues();
            values.put(NewsEntries.KEY_TITLE, entry.title);
            values.put(NewsEntries.KEY_DATE, entry.published);
            values.put(NewsEntries.KEY_LINK, entry.link);
            values.put(NewsEntries.KEY_CONTENT, entry.contentRaw);
            values.put(NewsEntries.KEY_SNIPPET, entry.contentSnippet);
            return values;
        }
    }
}
