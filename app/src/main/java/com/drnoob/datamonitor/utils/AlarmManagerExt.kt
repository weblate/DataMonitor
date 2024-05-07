package com.drnoob.datamonitor.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log

/**
 * Extension functions for the AlarmManager class.
 */
 class AlarmManagerExt {
    companion object {
        private val TAG = this::class.simpleName

        /**
         * Checks for the state of SCHEDULE_EXACT_ALARM permission before invoking AlarmManager.setExactAndAllowWhileIdle
         * with the specified params.
         *
         * @param type type of alarm.
         * @param triggerAtMillis time in milliseconds that the alarm should go
         *        off, using the appropriate clock (depending on the alarm type).
         * @param operation the PendingIntent action to perform when the alarm goes off.
         *
         * @return true if the operation is successful, false otherwise.
         *
         * @see AlarmManager.setExactAndAllowWhileIdle
         */
        fun AlarmManager.setExactAndAllowWhileIdleCompat(type: Int, triggerAtMillis: Long,
                                                         operation: PendingIntent): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (canScheduleExactAlarms()) {
                    setExactAndAllowWhileIdle(type, triggerAtMillis, operation)
                    true
                }
                else {
                    Log.d(TAG, "permission SCHEDULE_EXACT_ALARM denied during setExactAndAllowWhileIdleCompat().")
                    false
                }
            }
            else {
                setExactAndAllowWhileIdle(type, triggerAtMillis, operation)
                true
            }
        }

        /**
         * Checks for the state of SCHEDULE_EXACT_ALARM permission before invoking AlarmManager.setExact
         * with the specified params.
         *
         * @param type type of alarm.
         * @param triggerAtMillis time in milliseconds that the alarm should go
         *        off, using the appropriate clock (depending on the alarm type).
         * @param operation the PendingIntent action to perform when the alarm goes off.
         *
         * @return true if the operation is successful, false otherwise.
         *
         * @see AlarmManager.setExact
         */
        fun AlarmManager.setExactCompat(type: Int, triggerAtMillis: Long,
                                        operation: PendingIntent): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (canScheduleExactAlarms()) {
                    setExact(type, triggerAtMillis, operation)
                    true
                }
                else {
                    Log.d(TAG, "permission SCHEDULE_EXACT_ALARM denied during setExactCompat().")
                    false
                }
            }
            else {
                setExact(type, triggerAtMillis, operation)
                true
            }
        }
    }
}