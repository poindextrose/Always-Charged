package com.dexnamic.alwayscharged;

import java.lang.reflect.Method;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;

public class AlertReceiver extends BroadcastReceiver {

	private PowerManager mPowerManager;
	static public PowerManager.WakeLock mWakeLock;

	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			SharedPreferences settings = context.getSharedPreferences(
					MainActivity.PREFS_NAME, 0);
			boolean alarmEnabled = settings.getBoolean(
					MainActivity.PREF_ENABLE, false);
			if (alarmEnabled) {
				int hourOfDay = settings.getInt(MainActivity.PREF_HOUR, 22);
				int minute = settings.getInt(MainActivity.PREF_MINUTE, 0);
				AlarmScheduler.setDailyAlarm(context, hourOfDay, minute);
			}
			return;
		}

		mPowerManager = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		/* cycles endlessly right now
		 * if(screenOn()) {  // only works if API 7 or higher
			AlarmScheduler.snoozeAlarm(context, 10);
			return;
		}*/
		PowerManager.WakeLock mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK
				// | PowerManager.ON_AFTER_RELEASE
						| PowerManager.ACQUIRE_CAUSES_WAKEUP, "My Tag");
		mWakeLock.acquire();

		// if (intent.hasCategory("alarm") && screenOff())
		// if (intent.hasCategory("alarm"))
		doAlarm(context);

	}

	public void doAlarm(Context context) {
		Intent intent = new Intent(context, AlertActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		// intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_FROM_BACKGROUND);
		context.startActivity(intent);
	}

	private boolean screenOn() {
		try { // reflection to get PowerManager.isScreenOn() method if available
			Method m = PowerManager.class.getMethod("isScreenOn",
					(Class[]) null);
			boolean on = (Boolean) m.invoke(mPowerManager,
					(Object[]) null);
			return on;
		} catch (Exception e) {
			// hack: read logs to determine screen state
			return false;
		}
	}

}
