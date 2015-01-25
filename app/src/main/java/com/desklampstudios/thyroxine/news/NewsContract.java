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
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_NEWSENTRIES).build();

        public static final String CONTENT_TYPE_NEWSENTRIES =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/newsEntries";
        public static final String CONTENT_ITEM_TYPE_NEWSENTRIES =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/newsEntry";

        static final String KEY_TITLE = "news_title";
        static final String KEY_PUBLISHED = "news_published";
        static final String KEY_NEWS_ID = "news_id";
        static final String KEY_CONTENT = "news_content";
        static final String KEY_CONTENT_SNIPPET = "news_content_snippet";
        static final String KEY_LIKED = "news_liked";
        static final String KEY_NUM_LIKES = "news_num_likes";

        /** Default "ORDER BY" clause */
        static final String DEFAULT_SORT = KEY_PUBLISHED + " DESC";

        @NonNull
        public static Uri buildEntryUri(int newsId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(newsId))
                    .build();
        }
        public static int getNewsId(@NonNull Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(1));
        }

        // converting to and from not-really-pojos yooooo
        @NonNull
        static NewsEntry fromContentValues(@NonNull ContentValues values) {
            return new NewsEntry.Builder()
                    .newsId(values.getAsInteger(KEY_NEWS_ID))
                    .title(values.getAsString(KEY_TITLE))
                    .published(values.getAsLong(KEY_PUBLISHED))
                    .content(values.getAsString(KEY_CONTENT))
                    .contentSnippet(values.getAsString(KEY_CONTENT_SNIPPET))
                    .liked(values.getAsBoolean(KEY_LIKED))
                    .numLikes(values.getAsInteger(KEY_NUM_LIKES))
                    .build();
        }
        @NonNull
        static ContentValues toContentValues(@NonNull NewsEntry entry) {
            ContentValues values = new ContentValues();
            values.put(KEY_TITLE, entry.title);
            values.put(KEY_PUBLISHED, entry.published);
            values.put(KEY_NEWS_ID, entry.newsId);
            values.put(KEY_CONTENT, entry.content);
            values.put(KEY_CONTENT_SNIPPET, entry.contentSnippet);
            values.put(KEY_LIKED, entry.liked);
            values.put(KEY_NUM_LIKES, entry.numLikes);
            return values;
        }
    }
}
