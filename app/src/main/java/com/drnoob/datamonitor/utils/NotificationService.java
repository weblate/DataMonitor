/*
 * Copyright (C) 2021 Dr.NooB
 *
 * This file is a part of Data Monitor <https://github.com/itsdrnoob/DataMonitor>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.drnoob.datamonitor.utils;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.ui.activities.MainActivity;

import java.text.ParseException;

import static com.drnoob.datamonitor.core.Values.DATA_USAGE_NOTIFICATION_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_NOTIFICATION_ID;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_NOTIFICATION_NOTIFICATION_GROUP;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_MOBILE_DATA;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_REFRESH_INTERVAL;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_WIFI;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;

public class NotificationService extends Service {

    private static final String TAG = NotificationService.class.getSimpleName();
    private AlarmManager mAlarmManager;
    private Intent mUpdaterIntent;
    private PendingIntent mUpdaterPendingIntent;
    NotificationUpdater updater = new NotificationUpdater();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mUpdaterIntent = new Intent(this, NotificationUpdater.class);
        mUpdaterPendingIntent = PendingIntent.getBroadcast(this, 0, mUpdaterIntent,
                PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                DATA_USAGE_NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_mobile_data);
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_LOW);
        builder.setContentTitle(getString(R.string.title_data_usage_notification, getString(R.string.body_data_usage_notification_loading)));
        builder.setContentText(getString(R.string.body_data_usage_notification_loading));
        builder.setShowWhen(false);
        builder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setGroup(DATA_USAGE_NOTIFICATION_NOTIFICATION_GROUP);

        startForeground(DATA_USAGE_NOTIFICATION_ID, builder.build());
        startUpdater(getApplicationContext());

        mAlarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis(), mUpdaterPendingIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopUpdater(getApplicationContext());
    }

    private void startUpdater(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.setPriority(100);
        context.registerReceiver(updater, intentFilter);
        Log.d(TAG, "startUpdater: started");
    }

    public void stopUpdater(Context context) {
        try {
            context.unregisterReceiver(updater);
            Log.d(TAG, "stopUpdater: stopped");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class NotificationUpdater extends BroadcastReceiver {
        private static final String TAG = NotificationUpdater.class.getSimpleName();
        private String mobileDataUsage, wifiDataUsage,  totalDataUsage;
        private Long[] mobile, wifi;

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: NotificationUpdater");

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            StatusBarNotification[] notification = manager.getActiveNotifications();

            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setup_notification", true)) {

                try {
                    mobile = getDeviceMobileDataUsage(context, SESSION_TODAY, 1);
                    String[] mobileData = formatData(mobile[0], mobile[1]);

                    wifi = getDeviceWifiDataUsage(context, SESSION_TODAY);
                    String[] wifiData = formatData(wifi[0], wifi[1]);

                    long totalSent = mobile[0] + wifi[0];
                    long totalReceived = mobile[1] + wifi[1];

                    String[] total = formatData(totalSent, totalReceived);
                    totalDataUsage = context.getResources().getString(R.string.title_data_usage_notification, total[2]);
                    mobileDataUsage = context.getResources().getString(R.string.notification_mobile_data_usage,
                            mobileData[2]);
                    wifiDataUsage = context.getResources().getString(R.string.notification_wifi_data_usage,
                            wifiData[2]);
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "onReceive: notification update");
                StringBuilder totalDataUsageText = new StringBuilder();
                totalDataUsageText.append(mobileDataUsage + "\n")
                        .append(wifiDataUsage + "\n");

                Boolean showMobileData = PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(NOTIFICATION_MOBILE_DATA, true);
                Boolean showWifi = PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(NOTIFICATION_WIFI, true);


                Intent activityIntent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                        DATA_USAGE_NOTIFICATION_CHANNEL_ID);
                builder.setSmallIcon(R.drawable.ic_mobile_data);
                builder.setOngoing(true);
                builder.setPriority(NotificationCompat.PRIORITY_LOW);
//                builder.setContentTitle(context.getString(R.string.title_data_usage_notification));
                builder.setContentTitle(totalDataUsage);
                if (showMobileData && showWifi) {
                    builder.setStyle(new NotificationCompat.InboxStyle()
                            .addLine(mobileDataUsage)
                            .addLine(wifiDataUsage));
                }
                if (!showMobileData && showWifi) {
                    builder.setContentText(wifiDataUsage);
                }
                if (showMobileData && !showWifi) {
                    builder.setContentText(mobileDataUsage);
                }
                builder.setContentIntent(pendingIntent);
                builder.setAutoCancel(false);
                builder.setShowWhen(false);
                builder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
                builder.setGroup(DATA_USAGE_NOTIFICATION_NOTIFICATION_GROUP);
                NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
                managerCompat.notify(DATA_USAGE_NOTIFICATION_ID, builder.build());

                setRepeating(context);
            } else {
                Log.d(TAG, "onReceive: Aborted");
                abortBroadcast();
            }
        }

        private static void setRepeating(Context context) {
            Intent i = new Intent(context, NotificationUpdater.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            int elapsedTime = PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt(NOTIFICATION_REFRESH_INTERVAL, 60000);
            alarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis() + elapsedTime, pi);
        }
    }

    public static class NotificationRemover extends BroadcastReceiver {
        private static final String TAG = com.drnoob.datamonitor.utils.NotificationService.class.getSimpleName();
        private static final int DATA_USAGE_NOTIFICATION_ID = 0x45;

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: remove ");

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(DATA_USAGE_NOTIFICATION_ID);
        }
    }
}
