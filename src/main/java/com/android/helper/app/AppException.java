package com.android.helper.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ParseException;
import android.text.TextUtils;
import android.util.Log;

import com.android.helper.utils.LogUtil;
import com.google.gson.JsonParseException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import retrofit2.HttpException;

/**
 * 崩溃异常的收集
 */
public class AppException extends Exception implements UncaughtExceptionHandler {

    @SuppressLint("StaticFieldLeak")
    private static AppException INSTANCE;
    private final UncaughtExceptionHandler mDefaultHandler;
    private final Context mContext;
    private final HashMap<String, String> mMapParameter = new LinkedHashMap<>();

    private AppException(Context context) {
        this.mContext = context;
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static AppException getAppExceptionHandler(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AppException(context);
        }
        return INSTANCE;
    }

    @Override
    public void uncaughtException(@NotNull Thread thread, @NotNull Throwable ex) {
        if (mDefaultHandler != null && !handleException(thread, ex)) {
            // 如果没有处理就交给系统处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            // Sleep一会后结束程序
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e("AppException", "Error : ", e);
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    public boolean saveErrorLog(Throwable ex) {
        boolean isSave = false;
        FileWriter fw = null;
        PrintWriter pw = null;
        try {
            // 获取路径
            String path = getErrorLogPath();
            LogUtil.e("app --- error --path: " + path);
            if (TextUtils.isEmpty(path)) {
                return false;
            }

            fw = new FileWriter(path, false);
            pw = new PrintWriter(fw);

            pw.println("--------------------" + (DateFormat.getDateTimeInstance()
                    .format(new Date())) + "---------------------");

            Map<String, String> parameter = getParameter();
            for (Entry<String, String> entry : parameter.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                pw.println(key + " : " + value);
            }

            pw.println("Exception: " + ex.getMessage() + "\n");
            ex.printStackTrace(pw);
            pw.close();
            fw.close();
            isSave = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("AppException", "App崩溃信息异常，请检查是否给予了应用读写权限！ --->error:" + e.getMessage());
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ignored) {
                }
            }
        }
        return isSave;
    }

    /**
     * 自定义异常处理:收集错误信息&发送错误报告
     *
     * @return true:处理了该异常信息;否则返回false
     */
    private boolean handleException(Thread thread, Throwable ex) {
        if (thread == null || ex == null || mContext == null) {
            return false;
        } else {
            try {
                boolean saveErrorLog = saveErrorLog(ex);
                LogUtil.e("日志是否写成功：" + saveErrorLog);
                return saveErrorLog;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    /**
     * 获取App安装包信息
     */
    private PackageInfo getPackageInfo() {
        PackageInfo info = null;
        try {
            String packageName = mContext.getPackageName();
            LogUtil.e("----P: packageName: " + packageName);
            info = mContext.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace(System.err);
        }
        if (info == null) info = new PackageInfo();
        return info;
    }

    /**
     * 处理错误的异常信息
     */
    public static String exception(Throwable e) {
        String msg = "";
        if (e != null) {
            String message = e.getMessage();
            if (e instanceof HttpException) {
                msg = "Http异常：" + message;
            } else if (e instanceof JsonParseException || e instanceof JSONException || e instanceof ParseException) {
                msg = "数据解析异常：" + message;
            } else if (e instanceof ConnectException) {
                msg = "服务链接异常：" + message;
            } else if (e instanceof javax.net.ssl.SSLHandshakeException) {
                msg = "SSL异常：" + message;
            } else if (e instanceof SocketTimeoutException) {
                msg = "读取超时：" + message;
            } else {
                msg = "未知异常：" + message;
            }
        }
        return msg;
    }

    /**
     * @return 返回一个应用目录下的File文件的路径，这里不适用sd卡的路径，在高版本手机上，很多权限被拒绝，这里尽量避免类似的情况
     */
    public String getErrorLogPath() {
        if (mContext != null) {
            File filesDir = mContext.getFilesDir();
            if (filesDir != null) {
                String path = filesDir.getPath();
                if (!TextUtils.isEmpty(path)) {
                    File file = new File(path + File.separator, "error.txt");
                    // 文件存在
                    if (file.exists()) {
                        return file.getPath();
                    } else {
                        // 文件不存在
                        try {
                            boolean newFile = file.createNewFile();
                            if (newFile) {
                                return file.getPath();
                            } else {
                                return "";
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            return "";
                        }
                    }
                }
            }
        }
        return "";
    }


    public Map<String, String> getParameter() {
        PackageInfo packageInfo = getPackageInfo();
        mMapParameter.put("versionName", packageInfo.versionName);
        mMapParameter.put("versionCode", packageInfo.versionCode + "");
        mMapParameter.put("AndroidVersion", android.os.Build.VERSION.RELEASE);
        mMapParameter.put("AndroidModel", android.os.Build.MODEL);
        return mMapParameter;
    }

}
