package com.dexnamic.alwayscharged;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class PowerSnoozeService extends Service {

	public static PowerManager.WakeLock mWakeLock;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		String action = intent.getAction();
		Bundle extras = intent.getExtras();
		if (extras == null) {
			Log.e("", "extras == null 938290493");
		}
		int id = extras.getInt("id");
		int day = extras.getInt("day");

		IntentFilter intentBatteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent intentBattery = registerReceiver(null, intentBatteryChanged);

		int batteryScale = intentBattery.getIntExtra("scale", 0);
		if (batteryScale != 0) {  // can't divide by zero
			int batteryLevel = intentBattery.getIntExtra("level", batteryScale); // default full
			float batteryPercent = (float) batteryLevel / (float) batteryScale;
			if (batteryPercent >= 0.80) {
				AlarmScheduler.disablePowerSnooze(this);
			} else {
				AlarmScheduler.resetRepeatCount(this, null);
				AlarmScheduler.snoozeAlarm(this, 0, R.string.notify_power, id, day);
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
