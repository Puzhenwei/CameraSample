package com.cgfay.cain.camerasample.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;


public class PermissionUtils {
    private PermissionUtils() {}

    public static void askPermission(Activity context,
                                     String[] permissions,
                                     int req,
                                     Runnable runnable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = ActivityCompat.checkSelfPermission(context, permissions[0]);
            if (result == PackageManager.PERMISSION_GRANTED) {
                runnable.run();
            } else {
                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE}, req);
            }
        } else {
            runnable.run();
        }
    }

    public static void  onRequestPermissionsResult(boolean isReq,
                                                   int[] grantedResults,
                                                   Runnable okRun,
                                                   Runnable deniRun) {
        if (isReq) {
            if (grantedResults.length > 0
                    && grantedResults[0] == PackageManager.PERMISSION_GRANTED) {
                okRun.run();
            } else {
                deniRun.run();
            }
        }
    }
}
