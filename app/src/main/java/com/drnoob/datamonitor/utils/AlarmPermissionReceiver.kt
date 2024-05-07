package com.drnoob.datamonitor.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.drnoob.datamonitor.Common
import com.drnoob.datamonitor.Widget.DataUsageWidget
import com.drnoob.datamonitor.core.Values
import com.drnoob.datamonitor.core.Values.ALARM_PERMISSION_NOTIFICATION_ID
import com.drnoob.datamonitor.utils.AlarmManagerExt.Companion.setExactAndAllowWhileIdleCompat

class AlarmPermissionReceiver: BroadcastReceiver() {
    companion object {
        private val TAG = this::class.simpleName
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: permission SCHEDULE_EXACT_ALARM granted.")
        context?.let {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(ALARM_PERMISSION_NOTIFICATION_ID)

            restartServices(context)
        }
    }

    private fun restartServices(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        // Notification
        if (!preferences.getBoolean("combine_notifications", false) &&
            preferences.getBoolean("setup_notification", false)) {
            val notificationIntent = Intent(context, NotificationService::class.java)
            context.stopService(notificationIntent)
            context.startService(Intent(context, NotificationService::class.java))
        }

        // Data usage
        val dataUsageMonitorIntent = Intent(context, DataUsageMonitor::class.java)
        if (preferences.getBoolean("data_usage_alert", false)) {
            context.stopService(dataUsageMonitorIntent)
            context.startService(dataUsageMonitorIntent)
        }

        // Data plan refresh alarm
        if (preferences.getString(Values.DATA_RESET, "null") == Values.DATA_RESET_CUSTOM) {
            Common.cancelDataPlanNotification(context)
            if (preferences.getBoolean("auto_update_data_plan", false)) {
                Common.setRefreshAlarm(context)
            }
            else {
                Common.setDataPlanNotification(context)
            }
        }

        // Daily quota alert
        if (preferences.getBoolean("daily_quota_alert", false)) {
            val intent = Intent(context, DailyQuotaAlertReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 1000, intent, PendingIntent.FLAG_IMMUTABLE)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.setExactAndAllowWhileIdleCompat(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis(),
                    pendingIntent)) {
                Common.postAlarmPermissionDeniedNotification(context)
            }
        }

        // Widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, DataUsageWidget::class.java))
        val intent = Intent(context, DataUsageWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }
}