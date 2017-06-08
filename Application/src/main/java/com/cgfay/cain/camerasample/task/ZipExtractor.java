package com.cgfay.cain.camerasample.task;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * zip解压器
 */
public class ZipExtractor extends AsyncTask<Void, Integer, Long> {

    private static final String TAG = "ZipExtractor";
    private final File mInput;
    private final File mOutput;
    private int mProgress = 0;  // 解压每个文件进度
    private int mMax = 0;       // 解压每个文件的大小
    private final Context mContext;
    private volatile boolean mReplaceAll;
    private volatile boolean mReplace = false;

    private ExtractorCallback mCallback;

    public ZipExtractor(Context context, String in, String out) {
        this(context, in, out, false);
    }

    public ZipExtractor(Context context, String in, String out, boolean replaceAll) {
        mInput = new File(in);
        mOutput = new File(out);
        mContext = context;
        mReplaceAll = replaceAll;
    }

    @Override
    protected Long doInBackground(Void... params) {
        return unzip(mInput);
    }

    @Override
    protected void onPostExecute(Long aLong) {
        if (isCancelled()) {
            // 取消解压回调
            if (mCallback != null) {
                mCallback.onCancel();
            }
            return;
        }
        // 解压完成回调
        if (mCallback != null) {
            mCallback.onComplete();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // 开始解压回调
        if (mCallback != null) {
            mCallback.onStartExtracting();
        }
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values.length > 1) {
            int max = values[1];
            mMax = max;
        }
        // 解压过程回调
        else if (mCallback != null) {
            mCallback.onExtracting(mProgress, mMax);
        }
    }

    // 解压某个文件
    private long unzip(File inputFile) {
        long extractedSize = 0L;
        Enumeration<ZipEntry> entries;
        ZipFile zip = null;
        try {
            zip = new ZipFile(inputFile);
            long unCompressedSize = getOriginalSize(zip);
            publishProgress(0, (int)unCompressedSize);

            entries = (Enumeration<ZipEntry>) zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                File destination = new File(mOutput, entry.getName());
                // 如果父目录不存在，则需要创建一个
                if (!destination.getParentFile().exists()) {
                    destination.getParentFile().mkdirs();
                }

                // 写入文件
                ProgressReportingOutputStream outputStream =
                        new ProgressReportingOutputStream(destination);
                extractedSize += copy(zip.getInputStream(entry), outputStream);
                outputStream.close();
            }
        } catch (ZipException e) {
            e.printStackTrace();
            if (mCallback != null) {
                mCallback.onFailed();
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (mCallback != null) {
                mCallback.onFailed();
            }
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return extractedSize;
    }

    // 获取ZIP包的原始大小
    private long getOriginalSize(ZipFile file) {
        Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) file.entries();
        long originalSize = 0;
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getSize() >= 0) {
                originalSize += entry.getSize();
            }
        }
        return originalSize;
    }

    // 复制
    private int copy(InputStream input, OutputStream output) {
        byte[] buffer = new byte[1024 * 8];
        BufferedInputStream inputStream = new BufferedInputStream(input, 1024 * 8);
        BufferedOutputStream outputStream = new BufferedOutputStream(output, 1024 * 8);
        int count = 0, n = 0;
        try {
            while((n = inputStream.read(buffer, 0, 1024 * 8)) != -1) {
                outputStream.write(buffer, 0, n);
                count += n;
            }
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream.close();
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

    // 添加解压回调
    public void addExtractorCallback(ExtractorCallback callback) {
        mCallback = callback;
    }

    public interface ExtractorCallback {
        // 准备解压
        void onStartExtracting();
        // 解压过程
        void onExtracting(int progress, int max);
        // 解压失败
        void onFailed();
        // 解压成功
        void onComplete();
        // 取消解压
        void onCancel();
    }
}
