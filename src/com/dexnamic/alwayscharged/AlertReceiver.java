package com.dexnamic.alwayscharged;

import java.lang.reflect.Method;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AlertReceiver extends BroadcastReceiver {

	private PowerManager mPowerManager;

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d("dexnamic", "action = " + intent.getAction());

		// Get the app's shared preferences
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean alarmEnabled = settings.getBoolean(
				MainActivity.KEY_ALARM_ENABLED, false);
		int hourOfDay = settings.getInt(MainActivity.KEY_HOUR, 22);
		int minute = settings.getInt(MainActivity.KEY_MINUTE, 0);
		String action = intent.getAction();
		if (action != null) {
			if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
				if (alarmEnabled) {
					AlarmScheduler.setDailyAlarm(context, hourOfDay, minute);
				}
				return;
			} else if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_ALARM);
				AlarmScheduler.setDailyAlarm(context, hourOfDay, minute);
				return;
			} else if (action.equals(AlarmScheduler.TYPE_NOTIFY)) {
				AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE);
				return;
			} else if (action.equals(AlarmScheduler.TYPE_SNOOZE)) {
				doAlarm(context, action);
			} else if (action.equals(AlarmScheduler.TYPE_ALARM)) {
				AlarmScheduler.resetRepeatCount(context);
				doAlarm(context, action);
			}
			try {
				if (action.equals((String) Intent.class.getField(
						"ACTION_POWER_CONNECTED").get(null))) {
					AlarmScheduler.cancelAlarm(context,
							AlarmScheduler.TYPE_ALARM);
					AlarmScheduler.cancelAlarm(context,
							AlarmScheduler.TYPE_SNOOZE);
					return;
				}
				if (alarmEnabled
						&& action.equals((String) Intent.class.getField(
								"ACTION_POWER_DISCONNECTED").get(null))) {
					AlarmScheduler.setDailyAlarm(context, hourOfDay, minute);
					return;
				}
			} catch (Exception e) {
			}
		}

	}

	public void doAlarm(Context context, String action) {
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		int callState = tm.getCallState();
		if (screenOn() && callState != TelephonyManager.CALL_STATE_OFFHOOK
				&& callState != TelephonyManager.CALL_STATE_RINGING) {
			AlarmScheduler.snoozeAlarm(context);
			return;
		}

		mPowerManager = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK
				// PowerManager.SCREEN_DIM_WAKE_LOCK
				// | PowerManager.ACQUIRE_CAUSES_WAKEUP
				, "My Tag");
		// PowerManager.WakeLock mWakeLock = mPowerManager.newWakeLock(
		// PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		final long time_ms = AlertActivity.ALARM_TIMEOUT_MS + 10 * 1000; // add
																			// 10
																			// seconds
		mWakeLock.acquire(time_ms);

		AlarmScheduler.cancelNotification(context);

		Intent intent = new Intent(context, AlertActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_FROM_BACKGROUND);
		intent.setAction(action);
		context.startActivity(intent);
		// context.startService(new Intent(context, AlertService.class));
	}

	private boolean screenOn() {
		try { // reflection to get PowerManager.isScreenOn() method if available
			Method m = PowerManager.class.getMethod("isScreenOn",
					(Class[]) null);
			boolean on = (Boolean) m.invoke(mPowerManager, (Object[]) null);
			return on;
		} catch (Exception e) {
			// hack: read logs to determine screen state
			return false;
		}
	}
}
