package com.dexnamic.alwayscharged;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Stack;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

public class AlertActivity extends Activity {

	public static final long ALARM_TIMEOUT_MS = 30 * 1000; // 30 seconds

	private KeyguardManager.KeyguardLock mKeyguardLock;

	private PowerManager.WakeLock wakeLock;

	private MediaPlayer mMediaPlayer;
	private Vibrator mVibrator;

	private SharedPreferences mSettings;

	public static final long[] vibratePattern = { 500, 500 };

	// received
	BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action != null) {
				if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
					try {
						int plugged = intent.getIntExtra("plugged", 0);
						if (plugged > 0) { // skip alarm since device plugged in
							AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE);
							finish();
						}
					} catch (Exception e) {
					}
				} else if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
					AlarmScheduler.snoozeAlarm(AlertActivity.this);
					AlertActivity.this.finish();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		IntentFilter intentBatteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent intentBattery = registerReceiver(mBroadcastReceiver, intentBatteryChanged);
		int plugged = intentBattery.getIntExtra("plugged", 0);
		if (plugged > 0) { // skip alarm since device plugged in
			AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_SNOOZE);
			finish();
			return;
		}

		IntentFilter intentPhoneStateChanged = new IntentFilter(
				TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		registerReceiver(mBroadcastReceiver, intentPhoneStateChanged);

		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

		wakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP, "My Tag");
		wakeLock.acquire();
		//mPowerManager.userActivity(SystemClock.uptimeMillis(), true);

		setContentView(R.layout.alert);

		// FLAG_SHOW_WHEN_LOCKED keeps window above lock screen but only for
		// Android 2.0 and newer
		// reflection used for backward compatibility
		try {
			Field f = WindowManager.LayoutParams.class.getField("FLAG_SHOW_WHEN_LOCKED");
			// does not work if window is translucent
			getWindow().addFlags(f.getInt(null));
			mKeyguardLock = null;
		} catch (Exception e) {
			KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
			mKeyguardLock = km.newKeyguardLock(getString(R.string.app_name));
		}

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);

		mMediaPlayer = new MediaPlayer();

		// stop alarm if user plugs in device
		try {
			String action = (String) Intent.class.getField("ACTION_POWER_CONNECTED").get(null);
			intentBatteryChanged = new IntentFilter(action);
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					AlertActivity.this.finish();
				}
			}, new IntentFilter(action));
		} catch (Exception e) {
		}

		// set wallpaper as background
		try {
			Class<?> _WallpaperManager = Class.forName("android.app.WallpaperManager");
			Class<?>[] parameterTypes = { Context.class };
			Method _WM_getinstance = _WallpaperManager.getMethod("getInstance", parameterTypes);
			Object[] args = { this };
			Object wm = _WM_getinstance.invoke(null, args);
			Method _WM_getDrawable = _WallpaperManager.getMethod("getDrawable", (Class[]) null);
			Drawable drawable = (Drawable) _WM_getDrawable.invoke(wm, (Object[]) null);
			getWindow().setBackgroundDrawable(drawable);
		} catch (Exception e) {
			Log.e("dexnamic", e.getMessage());
		}
	}

	private static final int MSG_TIMEOUT = 1;
	private static final int MSG_UP_VOLUME = 2;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIMEOUT:
				if (repeatAlarm()) {
					AlarmScheduler.snoozeAlarm(AlertActivity.this);
				}
				removeMessages(MSG_UP_VOLUME);
				AlertActivity.this.finish();
				break;
			case MSG_UP_VOLUME:
				try {
					Float volume = mVolume.pop();
					if (volume != null) {
						mMediaPlayer.pause();
						mMediaPlayer.setVolume(volume, volume);
						mMediaPlayer.start();
						mHandler.sendMessageDelayed(msg, 1000);
					}
				} catch (Exception e) {
				}
				break;
			}
		}
	};

	static final int DIALOG_ALERT_ID = 0;

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_ALERT_ID:
			String snoozeText = "Snooze " + AlarmScheduler.SNOOZE_TIME_MIN + " "
					+ getString(R.string.minutes);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Don't forget to charge your phone!").setCancelable(false)
					.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							AlertActivity.this.finish();
						}
					}).setNegativeButton(snoozeText, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							AlarmScheduler.snoozeAlarm(AlertActivity.this);
							AlertActivity.this.finish();
						}
					});
			AlertDialog alert = builder.create();
			dialog = alert;
			dialog.setOnKeyListener(mOnKeyListener);
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	private DialogInterface.OnKeyListener mOnKeyListener = new DialogInterface.OnKeyListener() {
	
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {		
			if (mMediaPlayer.isPlaying()) {
				stopRingtone();
			}
			return false;
		}
	};

	private Stack<Float> mVolume = new Stack<Float>();
//	private int mVolumeLevels;

	@Override
	protected void onStart() {
		super.onStart();

		String chosenRingtone = mSettings.getString(MainActivity.KEY_RINGTONE, null);
		int maxVolume = 0;
		try {
			Uri uri = Uri.parse(chosenRingtone);
			mMediaPlayer.setDataSource(this, uri);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
			for (int i = 1; i <= 8; i *= 2) {
				mVolume.push(1 / (float) i);
			}
//			mVolumeLevels = mVolume.size();
//			float volume = mVolume.pop();
//			mMediaPlayer.setVolume(volume, volume);
//			Message msg = Message.obtain(mHandler, MSG_UP_VOLUME);
//			mHandler.sendMessageDelayed(msg, 1000);
			mMediaPlayer.setLooping(true);
			mMediaPlayer.prepare();
			mMediaPlayer.start();
			if (mSettings.getBoolean(MainActivity.KEY_VIBRATE, false))
				mVibrator.vibrate(vibratePattern, 0);
		} catch (Exception e) {
			Log.d("dexnamic", "max Volume = " + maxVolume);
			Log.e("dexnamic", e.getMessage());
		}

		Message msg = Message.obtain(mHandler, MSG_TIMEOUT);
		mHandler.sendMessageDelayed(msg, ALARM_TIMEOUT_MS);

		showDialog(DIALOG_ALERT_ID);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mKeyguardLock != null) {
			mKeyguardLock.disableKeyguard();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mKeyguardLock != null) {
			mKeyguardLock.reenableKeyguard();
		}
		if (wakeLock.isHeld())
			wakeLock.release();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mHandler.removeMessages(MSG_TIMEOUT);
		stopRingtone();
		try {
//			mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mSaveVolume, 0);
			mMediaPlayer.release();
		} catch (Exception e) {
		}
		unregisterReceiver(mBroadcastReceiver);
	}

	void stopRingtone() {
		try {
			mMediaPlayer.stop();
		} catch (Exception e) {
			return;
		}
		try {
			mVibrator.cancel();
		} catch (Exception e) {
		}
	}

	public boolean repeatAlarm() {
		boolean repeat = mSettings.getBoolean(MainActivity.KEY_REPEAT, false);
		if (repeat == false)
			return false;
		String keyCount = MainActivity.KEY_REPEAT_COUNT;
		int count = mSettings.getInt(keyCount, MainActivity.TIMES_TO_REPEAT);
		SharedPreferences.Editor editor = mSettings.edit();
		if (count > 0) {
			editor.putInt(keyCount, count - 1);
			editor.commit();
			return true;
		}
		return false;
	}

}
