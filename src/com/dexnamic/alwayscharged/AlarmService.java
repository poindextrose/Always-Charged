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

	private PowerManager mPowerManager;
	
	@Override
	public void onCreate() {
		super.onCreate();

		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// if user just disconnected phone, then check battery level
		String action = intent.getAction();
		IntentFilter intentBatteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent intentBattery = registerReceiver(null, intentBatteryChanged);
		if (action != null && action.equals(AlarmReceiver.ACTION_DISCONNECTED)) {
			int batteryScale = intentBattery.getIntExtra("scale", 0);
			if (batteryScale == 0) {
				stopSelf();
				return;
			}
			int batteryLevel = intentBattery.getIntExtra("level", batteryScale); // default full
			float batteryPercent = (float) batteryLevel / (float) batteryScale;
			if (batteryPercent >= 0.80) {
				AlarmScheduler.disablePowerSnooze(this);
				stopSelf();
				return;
			} else {
				AlarmScheduler.snoozeAlarm(this);
				stopSelf();
				return;
			}
		}
		
		AlarmScheduler.enablePowerSnooze(this);
		
		int batteryPlugged = intentBattery.getIntExtra("plugged", 0);
		if (batteryPlugged > 0) { // skip alarm since device plugged in
			stopSelf();
			return;
		}
//		
//		// if screen is on, snooze alarm
//		// if phone call is progress, snooze alarm
		if(screenOn() || telephoneInUse(this)) {
			AlarmScheduler.snoozeAlarm(this);
			stopSelf();
			return;
		}
		
		PowerManager.WakeLock mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
		final long time_ms = AlarmActivity.ALARM_TIMEOUT_MS + 10 * 1000; // add 10 seconds
		mWakeLock.acquire(time_ms);

		AlarmScheduler.cancelNotification(this);

		Intent intentActivity = new Intent(this, AlarmActivity.class);
		intentActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND);
		startActivity(intentActivity);		
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
