package com.dexnamic.alwayscharged;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;

public class PowerSnoozeService extends Service {

	public static PowerManager.WakeLock mWakeLock;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

//		String action = intent.getAction();
		Bundle extras = intent.getExtras();
//		if (extras == null) {
//			Log.e("", "extras == null 938290493");
//		}
		Alarm alarm = (Alarm) extras.getSerializable("alarm");

		IntentFilter intentBatteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent intentBattery = registerReceiver(null, intentBatteryChanged);

		int batteryScale = intentBattery.getIntExtra("scale", 0);
		if (batteryScale != 0) {  // can't divide by zero
			int batteryLevel = intentBattery.getIntExtra("level", batteryScale); // default full
			float batteryPercent = (float) batteryLevel / (float) batteryScale;
			if (batteryPercent >= 0.80) {
				Scheduler.disablePowerSnooze(this);
			} else {
				Scheduler.resetRepeatCount(this, null);
				Scheduler.snoozeAlarm(this, 0, R.string.notify_power, alarm);
			}
		}

		stopSelf();
		Scheduler.releaseWakeLock();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
