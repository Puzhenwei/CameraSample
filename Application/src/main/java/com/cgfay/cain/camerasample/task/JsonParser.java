package com.cgfay.cain.camerasample.task;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;

/**
 * Json解析器
 */

public class JsonParser extends AsyncTask<Void, Integer, Long> {

    private static final String TAG = "JsonParser";

    private File mFile;
    private Context mContext;
    private int mProgress = 0;
    private int mMax = 0;
    private JsonParserCallback mCallback;

    public JsonParser(Context context, String jsonFileName) {
        mContext = context;
        mFile = new File(jsonFileName);
    }

    @Override
    protected void onPreExecute() {
        // 开始解析回调
        if (mCallback != null) {
            mCallback.onStart();
        }
    }

    @Override
    protected Long doInBackground(Void... params) {
        return parsing();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values.length > 1) {
            mMax = values[1];
        } else if (mCallback != null) {
            mCallback.onParsing(values[0], mMax);
        }
    }

    @Override
    protected void onPostExecute(Long aLong) {
        if (isCancelled()) {
            // 取消解析回调
            if (mCallback != null) {
                mCallback.onCancel();
            }
            return;
        }

        // 解析完成回调
        if (mCallback != null) {
            mCallback.onComplete();
        }
    }

    // 解析Json
    private Long parsing() {


        return Long.valueOf(0);
    }

    // 添加下载器回调
    public void adJsonParserCallback(JsonParserCallback callback) {
        mCallback = callback;
    }

    public interface JsonParserCallback {
        // 失败原因
        enum FailType{ Exist, InComplete, IOError }
        // 准备解析
        void onStart();
        // 解析过程
        void onParsing(int progress, int max);
        // 解析失败
        void onFailed(FailType type);
        // 解析成功
        void onComplete();
        // 取消解析
        void onCancel();
    }
}
