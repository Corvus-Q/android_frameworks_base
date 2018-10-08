/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.du;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import android.os.AsyncTask;
import android.os.SystemProperties;
import android.os.UserHandle;

import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.R;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

public class ActionUtils {

    public static final String INTENT_SCREENSHOT = "action_take_screenshot";
    public static final String INTENT_REGION_SCREENSHOT = "action_take_region_screenshot";
    public static final String INTENT_LONG_SCREENSHOT = "action_take_long_screenshot";

    private static IStatusBarService mStatusBarService = null;

    private static IStatusBarService getStatusBarService() {
        synchronized (ActionUtils.class) {
            if (mStatusBarService == null) {
                mStatusBarService = IStatusBarService.Stub.asInterface(
                        ServiceManager.getService("statusbar"));
            }
            return mStatusBarService;
        }
    }

    // Check to see if device is WiFi only
    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }

    // Check if device is connected to Wi-Fi
    public static boolean isWiFiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi.isConnected();
    }

    // Check to see if device is connected to the internet
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return wifi.isConnected() || mobile.isConnected();
    }

    // Screen off
    public static void switchScreenOff(Context ctx) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm!= null && pm.isScreenOn()) {
            pm.goToSleep(SystemClock.uptimeMillis());
        }
    }

    // Screen on
    public static void switchScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        pm.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:CAMERA_GESTURE_PREVENT_LOCK");
    }

    // Volume panel
    public static void toggleVolumePanel(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
    }

    // Toggle camera
    public static void toggleCameraFlash() {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.toggleCameraFlash();
            } catch (RemoteException e) {
                // do nothing.
            }
        }
    }

    // Clear notifications
    public static void clearAllNotifications() {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.onClearAllNotifications(ActivityManager.getCurrentUser());
            } catch (RemoteException e) {
                // do nothing.
            }
        }
    }

    // Partial screenshot
    public static void setPartialScreenshot(boolean active) {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.setPartialScreenshot(active);
            } catch (RemoteException e) {}
        }
    }

    // Full screenshots
    public static void takeScreenshot(int type) {
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            switch (type) {
            case 0:
                // Do Nothing
                break;
            case 1:
                wm.sendCustomAction(new Intent(INTENT_SCREENSHOT));
                break;
            case 2:
                wm.sendCustomAction(new Intent(INTENT_REGION_SCREENSHOT));
                break;
            case 3:
                wm.sendCustomAction(new Intent(INTENT_LONG_SCREENSHOT));
                break;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Toggle notifications panel
    public static void toggleNotifications() {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.togglePanel();
            } catch (RemoteException e) {
                // do nothing.
            }
        }
    }

    // Toggle qs panel
    public static void toggleQsPanel() {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.toggleSettingsPanel();
            } catch (RemoteException e) {
                // do nothing.
            }
        }
    }

    // Cycle ringer modes
    public static void toggleRingerModes (Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Vibrator mVibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                if (mVibrator.hasVibrator()) {
                    am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                }
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;
        }
    }

    // Launch camera
    public static void launchCamera(Context context) {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    // Launch voice search
    public static void launchVoiceSearch(Context context) {
        Intent intent = new Intent(Intent.ACTION_SEARCH_LONG_PRESS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void sendSystemKeyToStatusBar(int keyCode) {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.handleSystemKey(keyCode);
            } catch (RemoteException e) {
                // do nothing.
            }
        }
    }

    public static void killForegroundApp() {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.killForegroundApp();
            } catch (RemoteException e) {
                // do nothing.
            }
        }
    }

    public static void restartSystemUi(Context context) {
        new RestartSystemUiTask(context).execute();
    }

    public static void showSystemUiRestartDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.systemui_restart_title)
                .setMessage(R.string.systemui_restart_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        restartSystemUi(context);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static class RestartSystemUiTask extends AsyncTask<Void, Void, Void> {
        private Context mContext;


        public RestartSystemUiTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                ActivityManager am =
                        (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                IActivityManager ams = ActivityManager.getService();
                for (ActivityManager.RunningAppProcessInfo app: am.getRunningAppProcesses()) {
                    if ("com.android.systemui".equals(app.processName)) {
                        ams.killApplicationProcess(app.processName, app.uid);
                        break;
                    }
                }
                //Class ActivityManagerNative = Class.forName("android.app.ActivityManagerNative");
                //Method getDefault = ActivityManagerNative.getDeclaredMethod("getDefault", null);
                //Object amn = getDefault.invoke(null, null);
                //Method killApplicationProcess = amn.getClass().getDeclaredMethod("killApplicationProcess", String.class, int.class);
                //mContext.stopService(new Intent().setComponent(new ComponentName("com.android.systemui", "com.android.systemui.SystemUIService")));
                //am.killBackgroundProcesses("com.android.systemui");
                //for (ActivityManager.RunningAppProcessInfo app : am.getRunningAppProcesses()) {
                //    if ("com.android.systemui".equals(app.processName)) {
                //        killApplicationProcess.invoke(amn, app.processName, app.uid);
                //        break;
                //    }
                //}
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
