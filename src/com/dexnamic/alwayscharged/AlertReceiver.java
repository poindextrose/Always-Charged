package com.dexnamic.alwayscharged;

import java.lang.reflect.Method;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;

public class AlertReceiver extends BroadcastReceiver {

	private PowerManager mPowerManager;
	static public PowerManager.WakeLock mWakeLock;
	
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d("dexnamic", "action = " + intent.getAction());

		SharedPreferences settings = context.getSharedPreferences(
				MainActivity.PREFS_NAME, 0);
		boolean alarmEnabled = settings.getBoolean(MainActivity.PREF_ENABLE,
				false);
		String action = intent.getAction();
		if (action != null) {
			if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
				if (alarmEnabled) {
					int hourOfDay = settings.getInt(MainActivity.PREF_HOUR, 22);
					int minute = settings.getInt(MainActivity.PREF_MINUTE, 0);
					AlarmScheduler.setDailyAlarm(context, hourOfDay, minute);
				}
				return;
			} else if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				/********************
				 * adjust time zone
				 */
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
					int hourOfDay = settings.getInt(MainActivity.PREF_HOUR, 22);
					int minute = settings.getInt(MainActivity.PREF_MINUTE, 0);
					AlarmScheduler.setDailyAlarm(context, hourOfDay, minute);
					return;
				}
			} catch (Exception e) {
			}
		}

		mPowerManager = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);

		if (screenOn()) {
			AlarmScheduler.snoozeAlarm(context, AlarmScheduler.SNOOZE_TIME_MIN); // snooze alarm for 10 minutes if screen on
			return;
		}

		PowerManager.WakeLock mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK
						| PowerManager.ACQUIRE_CAUSES_WAKEUP, "My Tag");
		mWakeLock.acquire();

		doAlarm(context);

	}

	public void doAlarm(Context context) {
		Intent intent = new Intent(context, AlertActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_FROM_BACKGROUND);
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
