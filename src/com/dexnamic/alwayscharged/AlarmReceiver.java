package com.dexnamic.alwayscharged;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

	/**
	 * Intent action passed to {@link AlarmActivity} during ACTION_DISCONNECTED to signal that
	 * activity should check battery level and snooze alarm if power snooze enabled
	 */
	public final static String ACTION_DISCONNECTED = "action_disconnected";

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d("dexnamic", "action = " + intent.getAction());

		// Get the app's shared preferences
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		boolean alarmEnabled = settings.getBoolean(MainActivity.KEY_ALARM_ENABLED, false);
		int hourOfDay = settings.getInt(MainActivity.KEY_HOUR, 22);
		int minute = settings.getInt(MainActivity.KEY_MINUTE, 0);
		String action = intent.getAction();
		if (action != null) {
			if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
				AlarmScheduler.setDailyAlarm(context, hourOfDay, minute);
				return;
			} else if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_ALARM);
				AlarmScheduler.setDailyAlarm(context, hourOfDay, minute);
				return;
			} else if (action.equals(AlarmScheduler.TYPE_NOTIFY)) {
				AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE);
				AlarmScheduler.disablePowerSnooze(context);
				return;
			} else if (action.equals(AlarmScheduler.TYPE_SNOOZE)) {
				startAlarmService(context, AlarmScheduler.TYPE_SNOOZE);
			} else if (action.equals(AlarmScheduler.TYPE_ALARM)) {
				resetRepeatCount(context);
				startAlarmService(context, AlarmScheduler.TYPE_ALARM);
			}
			try { // Two fields below require API 5
				if (action.equals((String) Intent.class.getField("ACTION_POWER_CONNECTED")
						.get(null))) {
					AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_ALARM);
					AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE);
					return;
				}
				if (action.equals((String) Intent.class.getField("ACTION_POWER_DISCONNECTED").get(
						null))) {
					AlarmScheduler.setDailyAlarm(context, hourOfDay, minute);
					// if in power_snooze mode, doAlarm, but have activity only alarm if not fully charged
					if (alarmEnabled && AlarmScheduler.isPowerSnooze(context))
						startAlarmService(context, ACTION_DISCONNECTED);
					return;
				}
			} catch (Exception e) {
			}
		}

	}

	public void startAlarmService(Context context, String action) {
		Intent intent = new Intent(context, AlarmService.class);
		intent.setAction(action);
		context.startService(intent);
	}

	private void resetRepeatCount(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(MainActivity.KEY_REPEAT_COUNT, MainActivity.TIMES_TO_REPEAT);
		editor.commit();
	}
}
