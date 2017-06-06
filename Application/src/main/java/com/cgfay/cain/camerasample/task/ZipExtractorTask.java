package com.cgfay.cain.camerasample.task;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

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

// 压缩包操作任务
public class ZipExtractorTask extends AsyncTask<Void, Integer, Long> {

    private static final String TAG = "ZipExtractorTask";
    private final File mInput;
    private final File mOutput;
    private final ProgressDialog mDialog;
    private int mProgress = 0;
    private final Context mContext;
    private volatile boolean mReplaceAll;
    private volatile boolean mReplace = false;

    private ExtractorCallback mCallback;

    public ZipExtractorTask(Context context, String in, String out) {
        this(context, in, out, false);
    }

    public ZipExtractorTask(Context context, String in, String out, boolean replaceAll) {
        mInput = new File(in);
        mOutput = new File(out);
        mContext = context;
        if (context != null) {
            mDialog = new ProgressDialog(context);
        } else {
            mDialog = null;
        }
        mReplaceAll = replaceAll;
    }

    @Override
    protected Long doInBackground(Void... params) {
        return unzip();
    }

    @Override
    protected void onPostExecute(Long aLong) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        if (isCancelled()) {
            return;
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mDialog != null) {
            mDialog.setTitle("Extracting");
            mDialog.setMessage(mInput.getName());
            mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancel(true);
                }
            });
            mDialog.show();
        }
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mDialog != null) {
            return;
        }

        if (values.length > 1) {
            int max = values[1];
            mDialog.setMax(max);
        } else {
            mDialog.setProgress(values[0].intValue());
        }
    }

    // 解压
    private long unzip() {
        long extractedSize = 0L;
        Enumeration<ZipEntry> entries;
        ZipFile zip = null;
        try {
            zip = new ZipFile(mInput);
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
                    Log.e(TAG, "make = " + destination.getParentFile().getAbsolutePath());
                    destination.getParentFile().mkdirs();
                }

                // 判断是否需要替换已经存在的文件，根据回调
                if (destination.exists() && mContext != null && !mReplaceAll) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mReplace = true;
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mReplace = false;
                        }
                    });
                    builder.setNeutralButton("全部替换", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mReplaceAll = true;
                        }
                    });
                    builder.create().show();
                }

                // 如果不替换，则追加一个时间后缀
                boolean result = true;
                if (!mReplace && !mReplaceAll) {
                    String fileName = entry.getName() + SystemClock.currentThreadTimeMillis();
                    result = destination.renameTo(new File(fileName));
                }
                // 在不替换的情况下重命名不成功，则丢弃该文件，否则写入文件
                // 在替换的情况下，直接写入文件
                if (!result) {
                    continue;
                }
                
                // 写入文件
                ProgressReportingOutputStream outputStream =
                        new ProgressReportingOutputStream(destination);
                extractedSize += copy(zip.getInputStream(entry), outputStream);
                outputStream.close();
            }
        } catch (ZipException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                zip.close();
            } catch (IOException e) {
                e.printStackTrace();
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
        // 解压过程
        void onExtracting(int progress);
        // 解压失败
        void onFailed();
        // 解压成功
        void onComplete();
        // 取消解压
        void onCancel();
    }
}
