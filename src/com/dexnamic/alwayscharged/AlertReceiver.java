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

	/**
	 * Intent action passed to {@link AlertActivity} during ACTION_DISCONNECTED to signal that
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
				startAlertActivity(context, action, false);
			} else if (action.equals(AlarmScheduler.TYPE_ALARM)) {
				resetRepeatCount(context);
				startAlertActivity(context, action, false);
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
						startAlertActivity(context, ACTION_DISCONNECTED, true);
					return;
				}
			} catch (Exception e) {
			}
		}

	}

	public void startAlertActivity(Context context, String action, boolean forceStart) {
		if (!forceStart) {
			if (screenOn() || deviceMoving() || telephoneInUse(context)) {
				AlarmScheduler.snoozeAlarm(context);
				return;
			}
		}

		AlarmScheduler.enablePowerSnooze(context);

		mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
		final long time_ms = AlertActivity.ALARM_TIMEOUT_MS + 10 * 1000; // add 10 seconds
		mWakeLock.acquire(time_ms);

		AlarmScheduler.cancelNotification(context);

		Intent intent = new Intent(context, AlertActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND);
		intent.setAction(action);
		context.startActivity(intent);
		// context.startService(new Intent(context, AlertService.class));
	}

	private boolean deviceMoving() {
		return false;
	}

	private boolean telephoneInUse(Context context) {
		try {
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			int callState = tm.getCallState();
			if (callState == TelephonyManager.CALL_STATE_OFFHOOK
					|| callState == TelephonyManager.CALL_STATE_RINGING)
				return true;
		} catch (Exception e) {
		}
		return false;
	}

	private boolean screenOn() {
		try { // reflection to get PowerManager.isScreenOn() method if available
			Method m = PowerManager.class.getMethod("isScreenOn", (Class[]) null);
			boolean on = (Boolean) m.invoke(mPowerManager, (Object[]) null);
			return on;
		} catch (Exception e) {
			// hack: read logs to determine screen state
			return false;
		}
	}

	private void resetRepeatCount(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(MainActivity.KEY_REPEAT_COUNT, MainActivity.TIMES_TO_REPEAT);
		editor.commit();
	}
}
