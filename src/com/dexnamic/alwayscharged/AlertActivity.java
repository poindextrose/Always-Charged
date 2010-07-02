package com.dexnamic.alwayscharged;

import java.lang.reflect.Field;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AlertActivity extends Activity {

	public static final long ALARM_TIMEOUT_MS = 30 * 1000; // 30 seconds

	private KeyguardManager.KeyguardLock mKeyguardLock;

	private PowerManager.WakeLock wakeLock;

	private MediaPlayer mMediaPlayer;
	private Vibrator mVibrator;

	private Button mButtonDismiss;
	private Button mButtonSnooze;

	// received
	BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				int plugged = intent.getIntExtra("plugged", 0);
				if (plugged > 0) { // skip alarm since device plugged in
					AlarmScheduler.cancelAlarm(context,
							AlarmScheduler.TYPE_SNOOZE);
					finish();
				}
			} catch (Exception e) {
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		IntentFilter intentFilter = new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED);
		Intent intentBattery = registerReceiver(null, intentFilter);
		int plugged = intentBattery.getIntExtra("plugged", 0);
		if (plugged > 0) { // skip alarm since device plugged in
			AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_SNOOZE);
			finish();
			return;
		}

		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

		wakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK
						| PowerManager.ACQUIRE_CAUSES_WAKEUP, "My Tag");
		wakeLock.acquire();
		mPowerManager.userActivity(SystemClock.uptimeMillis(), true);

		setContentView(R.layout.alert);
		// FLAG_SHOW_WHEN_LOCKED keeps window above lock screen but only for
		// Android 2.0 and newer
		// reflection used for backward compatibility
		try {
			Field f = WindowManager.LayoutParams.class
					.getField("FLAG_SHOW_WHEN_LOCKED");
			// does not work if window is translucent
			getWindow().addFlags(f.getInt(null));
			mKeyguardLock = null;
		} catch (Exception e) {
			KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
			mKeyguardLock = km
					.newKeyguardLock("com.dexnamic.nighttimechargecheck");
		}

		mButtonDismiss = (Button) findViewById(R.id.ButtonDismiss);
		mButtonDismiss.setOnClickListener(mOnClickListener);
		mButtonSnooze = (Button) findViewById(R.id.ButtonSnooze);
		mButtonSnooze.setOnClickListener(mOnClickListener);
		// fix below for other languages
		mButtonSnooze.setText("Snooze " + AlarmScheduler.SNOOZE_TIME_MIN
				+ " min");

		SharedPreferences settings = getSharedPreferences(
				MainActivity.PREFS_NAME, 0);
		String chosenRingtone = settings.getString(MainActivity.PREF_RINGTONE,
				null);

		mMediaPlayer = new MediaPlayer();
		try {
			Uri uri = Uri.parse(chosenRingtone);
			mMediaPlayer.setDataSource(this, uri);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
			final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
				mMediaPlayer.setLooping(true);
				mMediaPlayer.prepare();
			}
			mMediaPlayer.start();
			if (audioManager
					.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) == AudioManager.VIBRATE_SETTING_ON)
				mVibrator.vibrate(3000);
		} catch (Exception e) {
			Log.e("dexnamic", e.getMessage());
		}

		// stop alarm if user plugs in device
		try {
			String action = (String) Intent.class.getField(
					"ACTION_POWER_CONNECTED").get(null);
			intentFilter = new IntentFilter(action);
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					AlertActivity.this.finish();
				}
			}, new IntentFilter(action));
		} catch (Exception e) {
		}

	}

	private static final int MSG_TIMEOUT = 1;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIMEOUT:
				AlarmScheduler.snoozeAlarm(AlertActivity.this,
						AlarmScheduler.SNOOZE_TIME_MIN);
				AlertActivity.this.finish();
				break;
			}
		}
	};

	private OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v == mButtonDismiss)
				AlertActivity.this.finish();
			else if (v == mButtonSnooze) {
				AlarmScheduler.snoozeAlarm(AlertActivity.this,
						AlarmScheduler.SNOOZE_TIME_MIN);
				AlertActivity.this.finish();
			}
		}
	};

	@Override
	protected void onStart() {
		super.onStart();

		Message msg = Message.obtain(mHandler, MSG_TIMEOUT);
		mHandler.sendMessageDelayed(msg, ALARM_TIMEOUT_MS);
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
			mMediaPlayer.release();
		} catch (Exception e) {
		}
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		try {
			if (mMediaPlayer.isPlaying()) {
				stopRingtone();
			}
		} catch (Exception e) {
		}
		return super.onKeyDown(keyCode, event);
	}

}
