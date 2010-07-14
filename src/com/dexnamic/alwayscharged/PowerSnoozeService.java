package com.dexnamic.alwayscharged;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;

public class PowerSnoozeService extends Service {

	public static PowerManager.WakeLock mWakeLock;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		IntentFilter intentBatteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent intentBattery = registerReceiver(null, intentBatteryChanged);

		int batteryScale = intentBattery.getIntExtra("scale", 0);
		if (batteryScale != 0) {  // can't divide by zero
			int batteryLevel = intentBattery.getIntExtra("level", batteryScale); // default full
			float batteryPercent = (float) batteryLevel / (float) batteryScale;
			if (batteryPercent >= 0.80) {
				AlarmScheduler.disablePowerSnooze(this);
			} else {
				AlarmScheduler.snoozeAlarm(this, 0);
			}
		}

		stopSelf();
		AlarmScheduler.releaseWakeLock();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
