package com.cgfay.cain.camerasample.task;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Administrator on 2017/6/6.
 */

public class JsonAnalysizingTask extends AsyncTask<Void, Integer, Long> {

    private static final String TAG = "JsonAnalysizingTask";

    private File mFile;
    private Context mContext;
    private int mProgress = 0;
    private int mMax = 0;

    public JsonAnalysizingTask(Context context, String jsonFileName) {
        mContext = context;
        mFile = new File(jsonFileName);
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected Long doInBackground(Void... params) {
        return analysizing();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values.length > 1) {
            int length = values[1];
            if (length == -1) {
                mMax = 0;
            } else {
                mMax = length;
            }
        } else {
            mProgress = values[0].intValue();
        }
    }

    @Override
    protected void onPostExecute(Long aLong) {
        if (isCancelled()) {
            return;
        }

    }

    // 解析Json
    private Long analysizing() {

        return Long.valueOf(0);
    }

    private int mValues = 0;
    private final class ProgressReportingOutputStream extends FileOutputStream {

        public ProgressReportingOutputStream(File file) throws FileNotFoundException {
            super(file);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            mValues += len;
            publishProgress(mValues);
        }
    }
}
