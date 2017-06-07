package com.cgfay.cain.camerasample.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static com.cgfay.cain.camerasample.task.Downloader.DownloaderCallback.FailType.Exist;
import static com.cgfay.cain.camerasample.task.Downloader.DownloaderCallback.FailType.IOError;
import static com.cgfay.cain.camerasample.task.Downloader.DownloaderCallback.FailType.InComplete;

/**
 * 下载器
 */
public class Downloader extends AsyncTask<Void, Integer, Long> {

    private static final String TAG = "Downloader";

    private URL mUrl;
    private File mFile;
    private int mProgress = 0;
    private int mMax = 0;
    private ProgressReportingOutputStream mOutputStream;
    private Context mContext;
    private DownloaderCallback mCallback;

    /**
     * @param context 上下文
     * @param url   下载地址
     * @param outPath   存放路径
     */
    public Downloader(Context context, String url, String outPath) {
        mContext = context;
        try {
            mUrl = new URL(url);
            String fileName = new File(mUrl.getFile()).getName();
            mFile = new File(outPath, fileName);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPreExecute() {
        if (mCallback != null) {
            mCallback.onStart();
        }
    }

    @Override
    protected Long doInBackground(Void... params) {
        return download();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values.length > 1) {
            mMax = values[1];
        } else if (mCallback != null) {
            mCallback.onDownloading(mProgress, mMax);
        }
    }

    @Override
    protected void onPostExecute(Long aLong) {
        if (isCancelled()) {
            if (mCallback != null) {
                mCallback.onCancel();
            }
            return;
        }
        if (mCallback != null) {
            mCallback.onComplete();
        }
    }

    // 下载
    private long download() {
        URLConnection connection = null;
        int bytesCopied = 0;

        try {
            connection = mUrl.openConnection();
            int length = connection.getContentLength();
            if (mFile.exists() && length == mFile.length()) {
                Log.d(TAG, "file " + mFile.getName() + " already exits!");
                // 失败原因回调：文件已存在
                if (mCallback != null) {
                    mCallback.onFailed(Exist);
                }
                return -1;
            }
            mOutputStream = new ProgressReportingOutputStream(mFile);
            publishProgress(0, length);
            bytesCopied = copy(connection.getInputStream(), mOutputStream);
            if (bytesCopied != length && length != -1) {
                Log.e(TAG, "Download incomplete bytesCopied = "
                        + bytesCopied + ", length = " + length);
                // 失败原因回调：文件不完整
                if (mCallback != null) {
                    mCallback.onFailed(InComplete);
                }
                return -1;
            }
            mOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            // 失败原因回调：IO错误
            if (mCallback != null) {
                mCallback.onFailed(IOError);
            }
        }
        return bytesCopied;
    }

    private int copy(InputStream input, OutputStream output) {
        byte[] buffer = new byte[1024 * 8];
        BufferedInputStream in = new BufferedInputStream(input, 1024 * 8);
        BufferedOutputStream out = new BufferedOutputStream(output, 1024 *8);
        int count = 0, n = 0;
        try {
            while ((n = in.read(buffer, 0, 1024 * 8)) != -1) {
                out.write(buffer, 0, n);
                count += n;
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    private final class ProgressReportingOutputStream extends FileOutputStream {

        public ProgressReportingOutputStream(File name) throws FileNotFoundException {
            super(name);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            mProgress += len;
            publishProgress(mProgress);
        }
    }


    // 添加下载器回调
    public void addDownloaderCallback(DownloaderCallback callback) {
        mCallback = callback;
    }

    public interface DownloaderCallback {
        // 失败原因
        enum FailType{ Exist, InComplete, IOError }
        // 准备下载
        void onStart();
        // 下载过程
        void onDownloading(int progress, int max);
        // 下载失败
        void onFailed(FailType type);
        // 下载成功
        void onComplete();
        // 取消下载
        void onCancel();
    }
}
