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
import android.widget.Toast;

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
	static int setDailyAlarm(Context context, int repeats, int hourOfDay, int minute, int id) {
		Log.i("AlarmScheduler", "setDailyAlarm(" + repeats + ", " + hourOfDay + ", " + minute
				+ ", " + id + ")");

		// AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		// boolean alarmEnabled =
		// settings.getBoolean(MainActivity.KEY_ALARM_ENABLED, false);
		// if (!alarmEnabled)
		// return 0;

		// TODO: clean this up
		int snoozeTime = Integer.parseInt(settings.getString(
				MainActivity.KEY_SNOOZE_TIME_MIN,
				settings.getString(MainActivity.KEY_SNOOZE_TIME_MIN,
						context.getString(R.string.default_snooze_time))));

		Calendar now = Calendar.getInstance();
		Calendar nearestAlarmCalendar = null;

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		int week_ms = 7 * 24 * 60 * 60 * 1000;

		if (repeats > 0) {
			for (Integer day = 0; day < 7; day++) { // 0 = monday, 1 = tuesday
				if ((repeats & (1 << day)) > 0) {
					Calendar alarmCalendar = Calendar.getInstance();
					alarmCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
					alarmCalendar.set(Calendar.MINUTE, minute);
					// real alarm needs lead time to measurement movement
					alarmCalendar.add(Calendar.MINUTE, -snoozeTime);
					alarmCalendar.set(Calendar.SECOND, 0);
					// Calendar: 1 = Sunday, 2 = Monday, ...
					alarmCalendar.set(Calendar.DAY_OF_WEEK, ((day + 1) % 7)+1);
					while (alarmCalendar.before(now))
						alarmCalendar.add(Calendar.WEEK_OF_YEAR, 1);
					if (nearestAlarmCalendar == null || alarmCalendar.before(nearestAlarmCalendar))
						nearestAlarmCalendar = (Calendar) alarmCalendar.clone();
					PendingIntent pendingIntent = getPendingIntentUpdateCurrent(context, "alarm",
							id, day);
					alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
							alarmCalendar.getTimeInMillis(), week_ms, pendingIntent);
					Log.i("", "setRepeating " + alarmCalendar.toString());
				}
			}
		} else {
			Calendar alarmCalendar = Calendar.getInstance();
			alarmCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
			alarmCalendar.set(Calendar.MINUTE, minute);
			// real alarm needs lead time to measurement movement
			alarmCalendar.add(Calendar.MINUTE, -snoozeTime);
			alarmCalendar.set(Calendar.SECOND, 0);
			while (alarmCalendar.before(now))
				alarmCalendar.add(Calendar.DAY_OF_WEEK, 1);
			PendingIntent pendingIntent = getPendingIntentUpdateCurrent(context, "alarm", id, -1);
			alarmManager.set(AlarmManager.RTC_WAKEUP, alarmCalendar.getTimeInMillis(),
					pendingIntent);
			nearestAlarmCalendar = (Calendar) alarmCalendar.clone();
		}

		int minutesUntilNextAlarm = 1 + (int) ((nearestAlarmCalendar.getTimeInMillis() - now
				.getTimeInMillis()) / 1000 / 60);
		Toast.makeText(context, "Minutes = " + minutesUntilNextAlarm, Toast.LENGTH_LONG).show();

		return minutesUntilNextAlarm;
	}

	static void cancelAlarm(Context context, String action, int id, int day) {
		PendingIntent pi = getPendingIntentUpdateCurrent(context, action, id, day);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pi);

		cancelNotification(context);
	}

	// static void cancelAlarm(Context context, Intent intent) {
	// PendingIntent pi = getPendingIntentUpdateCurrent(context, intent);
	// AlarmManager alarmManager = (AlarmManager)
	// context.getSystemService(Context.ALARM_SERVICE);
	// alarmManager.cancel(pi);
	//
	// cancelNotification(context);
	// }

	static void cancelNotification(Context context) {
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(AlarmScheduler.NOTIFY_SNOOZE);
	}

	static void snoozeAlarm(Context context, int snoozeTime_min, int reason_resource_id, int id,
			int day) {

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String strMinutes = settings.getString(MainActivity.KEY_SNOOZE_TIME_MIN,
				context.getString(R.string.default_snooze_time));
		int minutes = Integer.parseInt(strMinutes);
		if (snoozeTime_min > 0)
			minutes = snoozeTime_min;

		final long time_ms = System.currentTimeMillis() + minutes * 60 * 1000;
		PendingIntent pi = getPendingIntentUpdateCurrent(context, TYPE_SNOOZE, id, day);
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
				PendingIntent npi = getPendingIntentUpdateCurrent(context, TYPE_NOTIFY, id, day);
				// Set the info for the views that show in the notification
				// panel.
				notification.setLatestEventInfo(context, context.getString(R.string.app_name),
						text, npi);
				// set notification to appear in "Ongoing" category
				notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL
						| Notification.FLAG_NO_CLEAR;

				nm.notify(NOTIFY_SNOOZE, notification);
			} catch (Exception e) {
			}
		}
	}

	static PendingIntent getPendingIntentUpdateCurrent(Context context, String action, int id,
			int day) {
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(action);
		intent.putExtra("id", id);
		intent.putExtra("day", day);
		return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

	}

	// static PendingIntent getPendingIntentUpdateCurrent(Context context,
	// Intent intent) {
	// return PendingIntent.getBroadcast(context, 0, intent,
	// PendingIntent.FLAG_UPDATE_CURRENT);
	//
	// }

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

	public static void cancelAlarm(Context context, Alarm alarm) {
		for (Integer day = -1; day < 7; day++) {
			cancelAlarm(context, "alarm", alarm.getID(), -1);
		}
	}

	// static void disableAllAlarms(Context context) {
	// cancelAlarm(context, AlarmScheduler.TYPE_ALARM);
	// cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE);
	// disablePowerSnooze(context);
	// }
}
