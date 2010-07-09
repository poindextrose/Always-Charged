package com.dexnamic.alwayscharged;

import java.lang.reflect.Method;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AlarmService extends Service {

	public static PowerManager.WakeLock mWakeLock;

	private PowerManager mPowerManager;
	private SensorManager mSensorManager;

	@Override
	public void onCreate() {
		super.onCreate();

		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

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
			AlarmScheduler.releaseWakeLock();
			return;
		}

		// if screen is on, snooze alarm
		// if phone call is progress, snooze alarm
		if (screenOn() || telephoneInUse(this)) {
			doSnooze();
			return;
		}

		checkForMotion();
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

	private void checkForMotion() {
		mFirstAccelerationValues = null;
		Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (mSensorManager.registerListener(mSensorEventListener, accelerometer,
				SensorManager.SENSOR_DELAY_NORMAL)) {

			Message msgTimeOut = Message.obtain(mHandler, MSG_TIMEOUT);
			mHandler.sendMessageDelayed(msgTimeOut, TIMEOUT_MS);
		} else { // sensor was not enabled
			doAlarm();
			return;
		}
	}

	private final long TIMEOUT_MS = 5000; // 5 seconds in milliseconds

	private static final int MSG_TIMEOUT = 0;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIMEOUT:
				doAlarm();
				break;
			}
		}
	};

	private float[] mFirstAccelerationValues;

	private int mSensorCount; // for debugging

//	private int mSensorResolution;

	private SensorEventListener mSensorEventListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			mSensorCount++;
			float[] firstValues = mFirstAccelerationValues;
			if (firstValues == null) {
				mFirstAccelerationValues = new float[event.values.length];
				for (int i = 0; i < event.values.length; i++)
					mFirstAccelerationValues[i] = event.values[i];
			} else {
				for (int i = 0; i < event.values.length; i++) {
					float change = Math.abs(firstValues[i] - event.values[i]);
					if (change > 1) // movement more than 1 m/s/s
					{
						Log.i("dexnamic", "motion detected...snoozing alarm...");
						doSnooze();
						return;
					}
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};

	private void doAlarm() {

		Log.v("dexnamic", "mSensorCount = " + mSensorCount);

		mHandler.removeMessages(MSG_TIMEOUT);
		cancelSensorReadings();

		PowerManager.WakeLock newWakeLock = mPowerManager.newWakeLock(
//				PowerManager.FULL_WAKE_LOCK
				PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, this
						.getClass().getName());
		newWakeLock.setReferenceCounted(false);
		newWakeLock.acquire(AlarmActivity.ALARM_TIMEOUT_MS);
		AlarmScheduler.releaseWakeLock(); // remove original wake lock
		mWakeLock = newWakeLock; // save new wake lock

		AlarmScheduler.cancelNotification(this);

		Intent intentActivity = new Intent(this, AlarmActivity.class);
		intentActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intentActivity);

		stopSelf();
	}

	private void doSnooze() {
		mHandler.removeMessages(MSG_TIMEOUT);
		cancelSensorReadings();

		AlarmScheduler.snoozeAlarm(this);
		stopSelf();
		AlarmScheduler.releaseWakeLock();
	}

	private void cancelSensorReadings() {
		if (mSensorManager != null && mSensorEventListener != null)
			mSensorManager.unregisterListener(mSensorEventListener);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		cancelSensorReadings();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
