package com.dexnamic.alwayscharged;

import java.lang.reflect.Method;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AlarmService extends Service implements SensorEventListener {

	private float mMotionToleranceDeg;
	private final long TIMEOUT_MS = 10000; // 10 seconds in milliseconds
	private static final int SENSOR_STABILZE_WAIT_TIME_MS = 5000; // 5 seconds
																	// in
																	// milliseconds

	public static PowerManager.WakeLock mWakeLock;

	private PowerManager mPowerManager;
	private SensorManager mSensorManager;
	private SharedPreferences mSharedPreferences;

	private boolean wasSnooze;

	@Override
	public void onCreate() {
		super.onCreate();

		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		String stringMotionTolerance = mSharedPreferences.getString(
				MainActivity.KEY_MOTION_TOLERANCE, "4");
		mMotionToleranceDeg = Float.parseFloat(stringMotionTolerance);
	}
	
	int mId, mDay;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		Bundle extras = intent.getExtras();
		if (extras == null) {
			Log.e("", "extras == null 938290493");
		}
		mId = extras.getInt("id");
		mDay = extras.getInt("day");

		Log.v(this.getClass().getSimpleName(), "AlarmServer.onStart()");
		AlarmScheduler.enablePowerSnooze(this);

		IntentFilter intentBatteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent intentBattery = registerReceiver(null, intentBatteryChanged);

		int batteryPlugged = intentBattery.getIntExtra("plugged", 0);
		if (batteryPlugged > 0) { // skip alarm since device plugged in
			Log.v(this.getClass().getSimpleName(), "device is plugged. no alarm.");
			stopSelf();
			AlarmScheduler.releaseWakeLock();
			return;
		}

		// if screen is on, snooze alarm
		// if phone call is progress, snooze alarm
		if (screenOn()) {
			Log.v(this.getClass().getSimpleName(), "screen is on.  snoozing...");
			doSnooze(R.string.notify_screen, mId, mDay);
			return;
		}
		if (telephoneInUse()) {
			Log.v(this.getClass().getSimpleName(), "telephone in use.  snoozing...");
			doSnooze(R.string.notify_phone, mId, mDay);
			return;
		}

		String action = intent.getAction();
		if (action != null && action.equals(AlarmScheduler.TYPE_SNOOZE)) {
			Log.v(this.getClass().getSimpleName(), "wasSnooze = true");
			wasSnooze = true;
		} else
			wasSnooze = false;

		if (mMotionToleranceDeg > 0)
			startReadingSensors();
		else {
			Log.v(this.getClass().getSimpleName(), "motion detection disabled.  alarm...");
			if (wasSnooze) {
				doAlarm();
			} else {
				Log.v(this.getClass().getSimpleName(), "motion detection disabled.  snoozing...");
				doSnooze(R.string.notify_motion, mId, mDay);
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		stopReadingSensors();
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

	private boolean telephoneInUse() {
		try {
			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			int callState = tm.getCallState();
			if (callState == TelephonyManager.CALL_STATE_OFFHOOK
					|| callState == TelephonyManager.CALL_STATE_RINGING)
				return true;
		} catch (Exception e) {
		}
		return false;
	}

	Sensor mAccelerometer;
	Sensor mMagneticField;
	boolean mAccelRegistered;
	boolean mMagneticRegistered;
	boolean mAccelEventReceived;
	boolean mMagneticEventReceived;
	float[] mAccelValues = new float[3];
	float[] mMagneticValues = new float[3];

	private void startReadingSensors() {
		Log.v(this.getClass().getSimpleName(), "startReadingSensors()");
		mAccelEventReceived = false;
		mMagneticEventReceived = false;
		mStabilze = false;
		mAccelValues[0] = SensorManager.GRAVITY_EARTH; // just in case accel
														// can't be read
		mMagneticValues[0] = SensorManager.MAGNETIC_FIELD_EARTH_MIN; // just in
																		// case
																		// mag
																		// can't
																		// be
																		// read
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mAccelRegistered = mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
		mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mMagneticRegistered = mSensorManager.registerListener(this, mMagneticField,
				SensorManager.SENSOR_DELAY_NORMAL);
		if (mAccelRegistered || mMagneticRegistered) {
			Message msgTimeOut = Message.obtain(mHandler, MSG_TIMEOUT);
			mHandler.sendMessageDelayed(msgTimeOut, TIMEOUT_MS);
			Message msgSensorStabilze = Message.obtain(mHandler, MSG_STABILZE);
			mHandler.sendMessageDelayed(msgSensorStabilze, SENSOR_STABILZE_WAIT_TIME_MS);
		} else { // sensors could not be enabled
			if (wasSnooze) {
				Log.v(this.getClass().getSimpleName(), "unable to read sensors. do alarm");
				doAlarm();
			} else {
				Log.v(this.getClass().getSimpleName(),
						"unable to read sensors. snooze until alarm time");
				doSnooze(R.string.notify_init, mId, mDay);
			}
			return;
		}
	}

	private static final int MSG_TIMEOUT = 0;
	private static final int MSG_STABILZE = 1;

	private boolean mStabilze;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIMEOUT:
				if (wasSnooze) {
					Log.v(this.getClass().getSimpleName(), "unable to read sensors. do alarm");
					doAlarm();
				} else {
					Log.v(this.getClass().getSimpleName(),
							"unable to read sensors. snooze until alarm time");
					doSnooze(R.string.notify_init, mId, mDay);
				}
				break;
			case MSG_STABILZE:
				Log.v(this.getClass().getSimpleName(), "sensors have stabalized");
				mStabilze = true;
				break;
			}
		}
	};

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			mAccelValues = event.values.clone();
			mAccelEventReceived = true;
		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			mMagneticValues = event.values.clone();
			mMagneticEventReceived = true;
		}
		if (mStabilze && (mAccelEventReceived || !mAccelRegistered)
				&& (mMagneticEventReceived || !mMagneticRegistered)) {
			sensorsHaveBeenRead();
			mStabilze = false; // to prevent entering again before sensors can be turned off
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private void doAlarm() {
		Log.v(this.getClass().getSimpleName(), "doAlarm()");

		mHandler.removeMessages(MSG_TIMEOUT);
		stopReadingSensors();

		PowerManager.WakeLock newWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, this
						.getClass().getName());
		newWakeLock.setReferenceCounted(false);
		String stringAlarmDuration = mSharedPreferences.getString(MainActivity.KEY_DURATION, "30");
		long alarmDuration = Long.parseLong(stringAlarmDuration);
		newWakeLock.acquire(alarmDuration + TIMEOUT_MS); // acquire lock for
															// duration of alarm
															// + 10 seconds
		AlarmScheduler.releaseWakeLock(); // remove original wake lock
		mWakeLock = newWakeLock; // save new wake lock

		AlarmScheduler.cancelNotification(this);

		Intent intentActivity = new Intent(this, AlarmActivity.class);
		intentActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intentActivity);

		stopSelf();
	}

	private void doSnooze(int reason_resource_id, int id, int day) {
		Log.v(this.getClass().getSimpleName(), "doSnooze()");
		mHandler.removeMessages(MSG_TIMEOUT);
		stopReadingSensors();

		AlarmScheduler.snoozeAlarm(this, 0, reason_resource_id, id, day);
		stopSelf();
		AlarmScheduler.releaseWakeLock();
	}

	private void sensorsHaveBeenRead() {
		Log.v(this.getClass().getSimpleName(), "sensorsHaveBeenRead()");

		stopReadingSensors();

		float[] orientation = new float[3];
		boolean hasMatrix = getCurrentOrientation(orientation);
		if (!hasMatrix) {
			Log.v(this.getClass().getSimpleName(), "hasMatrix = false");
			// Log.d("dexnamic", "getCurrentOrientation failed");
			if (wasSnooze)
				doAlarm();
			else
				doSnooze(R.string.notify_init, mId, mDay);
		}
		if (wasSnooze) {
			checkDeviceOrientation(orientation);
		} else {
			saveDeviceOrientation(orientation);
			Log.v(this.getClass().getSimpleName(), "saved device orientaiton.  snoozing...");
			doSnooze(R.string.notify_init, mId, mDay);
		}
	}

	private void checkDeviceOrientation(float[] newOrientation) {
		Log.v(this.getClass().getSimpleName(), "checkDeviceOrientation()");
		float[] previousOrientation = new float[3];
		readDeviceOrientation(previousOrientation);
		saveDeviceOrientation(newOrientation);

		boolean deviceMoved = false;
		for (int i = 0; i < 3; i++) {
			float diff_deg = (float) (Math.abs(previousOrientation[i] - newOrientation[i]) * 180.0 / Math.PI);
			float wrap = 180;
			if (i == 1) // pitch range from -90 to 90 degrees
				wrap = 360;
			if ((diff_deg > mMotionToleranceDeg) && (diff_deg < (wrap - mMotionToleranceDeg)))
				deviceMoved = true;
		}
		if (deviceMoved) {
			Log.v(this.getClass().getSimpleName(), "device has moved. snoozing...");
			doSnooze(R.string.notify_motion, mId, mDay);
		} else {
			Log.v(this.getClass().getSimpleName(), "device did not move. sounding alarm");
			doAlarm();
		}
	}

	private boolean getCurrentOrientation(float[] orientation) {
		float[] rotationMatrix = new float[16];
		float[] inclinationMatrix = new float[16];
		boolean hasMatrix = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix,
				mAccelValues, mMagneticValues);
		if (!hasMatrix)
			return false;
		SensorManager.getOrientation(rotationMatrix, orientation);
		Log.v(this.getClass().getSimpleName(), "orientation = (" + orientation[0] + ","
				+ orientation[1] + "," + orientation[2] + ")");
		return true;
	}

	private final static String KEY_AZIMUTH = "key_azimuth";
	private final static String KEY_PITCH = "key_pitch";
	private final static String KEY_ROLL = "key_roll";

	private void saveDeviceOrientation(float[] orientation) {
		Log.v(this.getClass().getSimpleName(), "saveDeviceOrientation()");
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putFloat(KEY_AZIMUTH, orientation[0]);
		editor.putFloat(KEY_PITCH, orientation[1]);
		editor.putFloat(KEY_ROLL, orientation[2]);
		editor.commit();
	}

	private void readDeviceOrientation(float[] orientation) {
		SharedPreferences settings = mSharedPreferences;
		orientation[0] = settings.getFloat(KEY_AZIMUTH, 0.0f);
		orientation[1] = settings.getFloat(KEY_PITCH, 0.0f);
		orientation[2] = settings.getFloat(KEY_ROLL, 0.0f);
	}

	private void stopReadingSensors() {
		if (mSensorManager != null) {
			mSensorManager.unregisterListener(this);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
