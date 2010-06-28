package com.dexnamic.alwayscharged;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class AlarmScheduler {

	public static final String TYPE_ALARM = "alarm";
	public static final String TYPE_SNOOZE = "snooze";

	public static void setDailyAlarm(Context context, int hourOfDay,
			int minute) {

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
		if(addDay) time_ms += interval_ms;

		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = getPendingIntent(context, TYPE_ALARM);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time_ms,
				interval_ms, pi);
	}

	public static void cancelAlarm(Context context, String category) {

		PendingIntent pi = getPendingIntent(context, category);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pi);
	}

	public static void snoozeAlarm(Context context,
			int minutes) {
		final long time_ms = System.currentTimeMillis() + minutes*60*1000;
		PendingIntent pi = getPendingIntent(context, TYPE_SNOOZE);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, time_ms, pi);
	}

	private static PendingIntent getPendingIntent(Context context,
			String category) {
		Intent intent = new Intent(context, AlertReceiver.class);
		intent.addCategory(category);

		return PendingIntent.getBroadcast(context, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

	}
}
