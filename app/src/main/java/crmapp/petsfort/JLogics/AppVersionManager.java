package crmapp.petsfort.JLogics;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

public class AppVersionManager {
    private static final String PREF_NAME = "app_prefs";

    public static boolean isLoggedIn(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return (sharedPref.getString(getAppVersion(context), null) != null);
    }

    public static void setLoginToCurrentVersion(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getAppVersion(context), "temp");
        editor.apply();
    }



    public static boolean getAlertFreezeEnableToCurrentVersion(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return (sharedPref.getString(getAppVersion(context).concat("_alertFreeze"), null) != null);
    }

    public static void setAlertFreezeEnableToCurrentVersion(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getAppVersion(context).concat("_alertFreeze"), "temp");
        editor.apply();
    }


    // Helper: Get current app version
    public static String getAppVersion(Context context) {
        String fallbackVersion = "0.0.0"; // Default fallback
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(0));
            } else {
                packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            }

            if (packageInfo.versionName != null) {
                return packageInfo.versionName;
            }
        } catch (Throwable ignored) {
            // Catch absolutely everything (even weird OEM bugs)
        }
        return fallbackVersion;
    }

}
