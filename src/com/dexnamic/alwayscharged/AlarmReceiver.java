package com.dexnamic.alwayscharged;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

	/**
	 * Intent action passed to {@link AlarmActivity} during ACTION_DISCONNECTED to signal that
	 * activity should check battery level and snooze alarm if power snooze enabled
	 */
	public final static String ACTION_DISCONNECTED = "action_disconnected";

	private PowerManager mPowerManager;

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d("dexnamic", "action = " + intent.getAction());

		mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

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
				startAlarmService(context, action);
			} else if (action.equals(AlarmScheduler.TYPE_ALARM)) {
				AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE);
				resetRepeatCount(context);
				startAlarmService(context, action);
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
					if (alarmEnabled && AlarmScheduler.isPowerSnoozeSet(context))
						startPowerSnoozeService(context);
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

		AlarmScheduler.setPartialWakeLock(mPowerManager);
	}

	public void startPowerSnoozeService(Context context) {
		Intent intent = new Intent(context, PowerSnoozeService.class);
		context.startService(intent);

		AlarmScheduler.setPartialWakeLock(mPowerManager);
	}

	private void resetRepeatCount(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(MainActivity.KEY_REPEAT_COUNT, 0);
		editor.commit();
	}
}
