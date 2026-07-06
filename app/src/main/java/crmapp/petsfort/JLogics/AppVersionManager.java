package crmapp.petsfort.JLogics;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

public class AppVersionManager {
    private static final String PREF_NAME = "app_prefs";
    private static final String ALERT_FREEZE_SUFFIX = "_alertFreeze";
    private static final String RELOGIN_EXEMPT_MIN_VERSION = "1.20";

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

    public static boolean shouldForceRelogin(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currentVersion = getAppVersion(context);

        if (sharedPref.contains(currentVersion)) {
            return false;
        }

        String lastLoggedInVersion = getLatestLoggedInVersion(sharedPref);
        if (lastLoggedInVersion == null) {
            return true;
        }

        if (compareVersions(lastLoggedInVersion, RELOGIN_EXEMPT_MIN_VERSION) >= 0) {
            setLoginToCurrentVersion(context);
            return false;
        }

        return true;
    }

    private static String getLatestLoggedInVersion(SharedPreferences sharedPref) {
        String latestVersion = null;

        for (String key : sharedPref.getAll().keySet()) {
            if (key.endsWith(ALERT_FREEZE_SUFFIX)) {
                continue;
            }

            if (latestVersion == null || compareVersions(key, latestVersion) > 0) {
                latestVersion = key;
            }
        }

        return latestVersion;
    }

    private static int compareVersions(String leftVersion, String rightVersion) {
        String[] leftParts = leftVersion.split("\\.");
        String[] rightParts = rightVersion.split("\\.");
        int maxLength = Math.max(leftParts.length, rightParts.length);

        for (int i = 0; i < maxLength; i++) {
            int leftPart = i < leftParts.length ? parseVersionPart(leftParts[i]) : 0;
            int rightPart = i < rightParts.length ? parseVersionPart(rightParts[i]) : 0;

            if (leftPart != rightPart) {
                return leftPart - rightPart;
            }
        }

        return 0;
    }

    private static int parseVersionPart(String versionPart) {
        try {
            return Integer.parseInt(versionPart);
        } catch (NumberFormatException ignored) {
            return 0;
        }
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
