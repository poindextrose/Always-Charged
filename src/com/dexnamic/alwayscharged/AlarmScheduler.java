package com.dexnamic.alwayscharged;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class AlarmScheduler {

	static final String TYPE_ALARM = "alarm";
	static final String TYPE_SNOOZE = "snooze";
	static final String TYPE_NOTIFY = "notify";

	/**
	 * SharedPreference key to indicate if alarm should snooze and alarm if
	 * device unplugged before it is fully charged
	 */
	final static String KEY_POWER_SNOOZE = "key_power_snooze";

	static final int NOTIFY_SNOOZE = 1;

	/**
	 * sets a daily, repeating alarm to potentially alter user to not-charging
	 * state
	 * 
	 * @param context
	 *            - Application context
	 * @param hourOfDay
	 *            - hour to set alarm (0-23)
	 * @param minute
	 *            - minute to set alarm (0-59)
	 * @return number of minutes until next alarm
	 */
	static int setDailyAlarm(Context context, int hourOfDay, int minute) {
		Log.i("AlarmScheduler", "setDailyAlarm(" + hourOfDay + ", " + minute + ")");

		AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		boolean alarmEnabled = settings.getBoolean(MainActivity.KEY_ALARM_ENABLED, false);
		if (!alarmEnabled)
			return 0;

		Calendar calNow = Calendar.getInstance();
		Calendar calAlarm = Calendar.getInstance();
		calAlarm.set(Calendar.HOUR_OF_DAY, hourOfDay);
		calAlarm.set(Calendar.MINUTE, minute);
		calAlarm.set(Calendar.SECOND, 0);
		long alarmTime_ms = calAlarm.getTimeInMillis();
		final long day_ms = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
		if (calAlarm.before(calNow)) // if alarm is now or earlier
			alarmTime_ms += day_ms;
		int snoozeTime = Integer.parseInt(settings.getString(
				MainActivity.KEY_SNOOZE_TIME_MIN,
				settings.getString(MainActivity.KEY_SNOOZE_TIME_MIN,
						context.getString(R.string.default_snooze_time))));
		int snoozeTime_ms = 60 * 1000 * snoozeTime;
		long alarmNotifyTime_ms = alarmTime_ms;
		alarmTime_ms -= snoozeTime_ms; // need to measure sensors before actual
										// bedtime

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = getPendingIntentUpdateCurrent(context, TYPE_ALARM);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime_ms, day_ms, pi);

		return 1 + (int) ((alarmNotifyTime_ms - calNow.getTimeInMillis()) / 1000.0 / 60.0);
	}

	static void cancelAlarm(Context context, String category) {
		PendingIntent pi = getPendingIntentUpdateCurrent(context, category);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pi);

		cancelNotification(context);
	}

	static void cancelNotification(Context context) {
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(AlarmScheduler.NOTIFY_SNOOZE);
	}

	static void snoozeAlarm(Context context, int snoozeTime_min, int reason_resource_id) {

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String strMinutes = settings.getString(MainActivity.KEY_SNOOZE_TIME_MIN,
				context.getString(R.string.default_snooze_time));
		int minutes = Integer.parseInt(strMinutes);
		if (snoozeTime_min > 0)
			minutes = snoozeTime_min;

		final long time_ms = System.currentTimeMillis() + minutes * 60 * 1000;
		PendingIntent pi = getPendingIntentUpdateCurrent(context, TYPE_SNOOZE);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, time_ms, pi);

		// no need to notify user before alarm may go off...
		if (reason_resource_id != R.string.notify_init) {
			try {
				NotificationManager nm = (NotificationManager) context
						.getSystemService(Context.NOTIFICATION_SERVICE);

				String text = context.getString(reason_resource_id);
				// Set the icon, scrolling text and timestamp
				Notification notification = new Notification(R.drawable.ic_stat_notify, text,
						System.currentTimeMillis());
				// The PendingIntent to launch our activity if the user selects
				// this
				// notification
				PendingIntent npi = getPendingIntentUpdateCurrent(context, TYPE_NOTIFY);
				// Set the info for the views that show in the notification
				// panel.
				notification.setLatestEventInfo(context, context.getString(R.string.app_name),
						text, npi);
				// set notification to appear in "Ongoing" category
				notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL
						| Notification.FLAG_NO_CLEAR;

				nm.notify(NOTIFY_SNOOZE, notification);
			} catch (Exception e) {
				// Log.e("dexnamic", e.getMessage());
			}
		}
	}

	static PendingIntent getPendingIntentUpdateCurrent(Context context, String action) {
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(action);

		return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

	}

	static void enablePowerSnooze(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(KEY_POWER_SNOOZE, true);
		editor.commit();
	}

	static void disablePowerSnooze(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(KEY_POWER_SNOOZE, false);
		editor.commit();
	}

	static PowerManager.WakeLock mWakeLock;

	static void setPartialWakeLock(PowerManager pm) {
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.dexnamic");
		mWakeLock.setReferenceCounted(false);
		mWakeLock.acquire();
	}

	static void releaseWakeLock() {
		if (mWakeLock != null) {
			try {
				mWakeLock.release();
			} catch (Exception e) {
			}
			mWakeLock = null;
		}
	}

	static void resetRepeatCount(Context context, SharedPreferences settings) {
		if (settings == null)
			settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(MainActivity.KEY_REPEAT_COUNT, 0);
		editor.commit();
	}

	static void disableAllAlarms(Context context) {
		cancelAlarm(context, AlarmScheduler.TYPE_ALARM);
		cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE);
		disablePowerSnooze(context);
	}
}
