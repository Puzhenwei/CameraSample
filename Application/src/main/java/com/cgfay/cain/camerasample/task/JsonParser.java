package com.cgfay.cain.camerasample.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.cgfay.cain.camerasample.util.FileUtils;
import com.google.gson.Gson;

import java.io.File;

/**
 * Json解析器
 */

public class JsonParser extends AsyncTask<Void, Integer, Long> {

    private static final String TAG = "JsonParser";

    private File mFile;
    private JsonParserCallback mCallback;
    private Class mClassType;
    private Object mObject;

    /**
     * JsonParser 构造器
     * @param jsonFileName  json文件名
     * @param classType     用于存放json解析结果的类类型
     */
    public JsonParser(String jsonFileName, Class classType) {
        mFile = new File(jsonFileName);
        mClassType = classType;
    }

    @Override
    protected Long doInBackground(Void... params) {
        return parsing();
    }

    @Override
    protected void onPostExecute(Long aLong) {
        if (isCancelled()) {
            return;
        }

        // 解析完成回调
        if (mCallback != null) {
            mCallback.onComplete(mObject);
        }
    }

    // 解析Json
    private Long parsing() {
        String strJson = FileUtils.readTextFromFile(mFile);
        Gson json = new Gson();
        mObject = json.fromJson(strJson, mClassType);
        return 0L;
    }

    // 添加下载器回调
    public void addJsonParserCallback(JsonParserCallback callback) {
        mCallback = callback;
    }

    public interface JsonParserCallback {
        // 解析成功
        void onComplete(Object object);
    }
}
