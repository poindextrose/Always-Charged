package com.dexnamic.alwayscharged;

import java.lang.reflect.Method;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.TelephonyManager;

public class AlarmService extends Service {

	public static PowerManager.WakeLock mWakeLock;

	private PowerManager mPowerManager;

	@Override
	public void onCreate() {
		super.onCreate();

		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		AlarmScheduler.enablePowerSnooze(this);

		IntentFilter intentBatteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent intentBattery = registerReceiver(null, intentBatteryChanged);

		int batteryPlugged = intentBattery.getIntExtra("plugged", 0);
		if (batteryPlugged > 0) { // skip alarm since device plugged in
			stopSelf();
			AlarmScheduler.releasePartialWakeLock();
			return;
		}

		// if screen is on, snooze alarm
		// if phone call is progress, snooze alarm
		if (screenOn() || telephoneInUse(this))
		{
			AlarmScheduler.snoozeAlarm(this);
			stopSelf();
			AlarmScheduler.releasePartialWakeLock();
			return;
		}
		
		PowerManager.WakeLock newWakeLock = mPowerManager.newWakeLock(
//				PowerManager.FULL_WAKE_LOCK
				PowerManager.SCREEN_DIM_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP, this
				.getClass().getName());
		newWakeLock.setReferenceCounted(false);
		newWakeLock.acquire(AlarmActivity.ALARM_TIMEOUT_MS);
		AlarmScheduler.releasePartialWakeLock(); // remove original wake lock
		mWakeLock = newWakeLock;  // save new wake lock

		AlarmScheduler.cancelNotification(this);

		Intent intentActivity = new Intent(this, AlarmActivity.class);
		intentActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intentActivity);
				
		stopSelf();
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

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
