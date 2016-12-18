package com.desklampstudios.thyroxine;

import android.os.AsyncTask;

public abstract class BetterAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    private ResultListener<Result> mResultListener;
    private ProgressListener<Progress> mProgressListener = null;
    private Exception mException = null;

    protected BetterAsyncTask(ResultListener<Result> listener) {
        setResultListener(listener);
    }

    public void setResultListener(ResultListener<Result> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Result listener cannot be null");
        }
        mResultListener = listener;
    }

    public void setProgressListener(ProgressListener<Progress> listener) {
        mProgressListener = listener;
    }

    protected void setException(Exception e) {
        mException = e;
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        super.onProgressUpdate(values);
        if (mProgressListener != null) {
            mProgressListener.onProgress(values);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        if (mException != null) {
            mResultListener.onError(mException);
        } else {
            mResultListener.onSuccess(result);
        }
    }

    public interface ResultListener<Result> {
        void onSuccess(Result result);
        void onError(Exception e);
    }

    public interface ProgressListener<Progress> {
        void onProgress(Progress... progress);
    }
}
