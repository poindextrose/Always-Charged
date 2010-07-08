package com.dexnamic.alwayscharged;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class AlarmScheduler {

	public static final int SNOOZE_TIME_MIN = 10;

	public static final String TYPE_ALARM = "alarm";
	public static final String TYPE_SNOOZE = "snooze";
	public static final String TYPE_NOTIFY = "notify";

	/**
	 * SharedPreference key to indicate if alarm should snooze and
	 * alarm if device unplugged before it is fully charged
	 */
	public final static String KEY_POWER_SNOOZE = "key_power_snooze";
	
	public static final int NOTIFY_SNOOZE = 1;

	public static void setDailyAlarm(Context context, int hourOfDay, int minute) {

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		boolean alarmEnabled = settings.getBoolean(MainActivity.KEY_ALARM_ENABLED, false);
		if(!alarmEnabled) return;

		Calendar calNow = Calendar.getInstance();
		Calendar calAlarm = Calendar.getInstance();
		calAlarm.set(Calendar.HOUR_OF_DAY, hourOfDay);
		calAlarm.set(Calendar.MINUTE, minute);
		calAlarm.set(Calendar.SECOND, 0);
		boolean addDay = false;
		if (calAlarm.compareTo(calNow) <= 0) // if alarm is now or earlier
			addDay = true;
		long time_ms = calAlarm.getTimeInMillis();
		final long interval_ms = 24 * 60 * 60 * 1000; // 24 hours in
		// milliseconds
		if (addDay)
			time_ms += interval_ms;

		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = getPendingIntentUpdateCurrent(context, TYPE_ALARM);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time_ms,
				interval_ms, pi);

		// Toast.makeText(context, "daily alarm " + hourOfDay + ":" + minute,
		// Toast.LENGTH_SHORT).show();
	}

	public static void cancelAlarm(Context context, String category) {
		PendingIntent pi = getPendingIntentUpdateCurrent(context, category);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pi);

		cancelNotification(context);
	}

	public static void cancelNotification(Context context) {
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(AlarmScheduler.NOTIFY_SNOOZE);		
	}

	public static void snoozeAlarm(Context context) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		String strMinutes = settings.getString(MainActivity.KEY_SNOOZE, "10");
		int minutes = Integer.parseInt(strMinutes);
		
		enablePowerSnooze(context);

		final long time_ms = System.currentTimeMillis() + minutes * 60 * 1000;
		PendingIntent pi = getPendingIntentUpdateCurrent(context, TYPE_SNOOZE);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, time_ms, pi);

		try {
			NotificationManager nm = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);

			String text = context.getString(R.string.notify_text);
			// Set the icon, scrolling text and timestamp
			Notification notification = new Notification(R.drawable.clock,
					text, System.currentTimeMillis());
			// The PendingIntent to launch our activity if the user selects this
			// notification
			PendingIntent npi = getPendingIntentUpdateCurrent(context,
					TYPE_NOTIFY);
			// Set the info for the views that show in the notification panel.
			notification.setLatestEventInfo(context, context.getString(R.string.app_name), text,
					npi);
			// set notification to appear in "Ongoing" category
			notification.flags = notification.flags
					| Notification.FLAG_AUTO_CANCEL
					| Notification.FLAG_NO_CLEAR;

			nm.notify(NOTIFY_SNOOZE, notification);
		} catch (Exception e) {
			Log.e("dexnamic", e.getMessage());
		}
	}

	private static PendingIntent getPendingIntentUpdateCurrent(Context context,
			String action) {
		Intent intent = new Intent(context, AlertReceiver.class);
		intent.setAction(action);

		return PendingIntent.getBroadcast(context, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

	}
	
	public static void enablePowerSnooze(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(KEY_POWER_SNOOZE, true);
		editor.commit();
	}

	public static void disablePowerSnooze(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(KEY_POWER_SNOOZE, false);
		editor.commit();		
	}
	
	public static boolean isPowerSnooze(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		return settings.getBoolean(KEY_POWER_SNOOZE, false);
	}
}
