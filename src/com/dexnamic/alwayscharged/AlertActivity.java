package com.dexnamic.alwayscharged;

import java.lang.reflect.Field;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.WindowManager;

public class AlertActivity extends Activity {

	private boolean useKeyguardManger;
	private KeyguardManager.KeyguardLock mKeyguardLock;

	private AlertDialog mAlert;
	private PowerManager.WakeLock wakeLock;
	private boolean keylockDisabled;

	SharedPreferences settings;
	private String chosenRingtone;
	private MediaPlayer mMediaPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		wakeLock = AlertReceiver.mWakeLock;
		if (wakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
					| PowerManager.ON_AFTER_RELEASE
					| PowerManager.ACQUIRE_CAUSES_WAKEUP, "My Tag");
			wakeLock.acquire();
		}

		// FLAG_SHOW_WHEN_LOCKED keeps window above lock screen but only for
		// Android 2.0 and newer
		// reflection used for backward compatibility
		try {
			Field f = WindowManager.LayoutParams.class
					.getField("FLAG_SHOW_WHEN_LOCKED");
			// does not work if window is translucent
			getWindow().addFlags(f.getInt(null));
			useKeyguardManger = false;
		} catch (Exception e) {
			useKeyguardManger = true;
			KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
			mKeyguardLock = km
					.newKeyguardLock("com.dexnamic.nighttimechargecheck");
		}

		IntentFilter intentFilter = new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED);
		Intent intentBattery = registerReceiver(null, intentFilter);
		int plugged = intentBattery.getIntExtra("plugged", 0);
		if (plugged > 0) { // skip alarm since device plugged in
			wakeLock.release();
			finish();
		}

		setContentView(R.layout.alert);

		keylockDisabled = false;

		settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
		chosenRingtone = settings.getString(MainActivity.PREF_RINGTONE, null);

		mMediaPlayer = new MediaPlayer();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Time to plug in your phone!").setCancelable(false)
				.setPositiveButton("Dismiss",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								AlertActivity.this.finish();
							}
						}).setNeutralButton("Snooze 30 min",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								AlarmScheduler.snoozeAlarm(AlertActivity.this,
										30);
								AlertActivity.this.finish();
							}
						});
		mAlert = builder.create();

	}

	private static final int MSG_TIMEOUT = 1;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIMEOUT:
				AlertActivity.this.finish();
				break;
			}
		}
	};

	@Override
	protected void onStart() {
		super.onStart();

		playRingtone();
		mAlert.show();
		Message msg = Message.obtain(mHandler, MSG_TIMEOUT);
		long delay_ms = 30 * 1000; // 30 seconds in milliseconds
		mHandler.sendMessageDelayed(msg, delay_ms);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (useKeyguardManger && !keylockDisabled) {
			mKeyguardLock.disableKeyguard();
			keylockDisabled = true;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopRingtone();
		if (useKeyguardManger && keylockDisabled) {
			mKeyguardLock.reenableKeyguard();
			keylockDisabled = false;
		}
		if (wakeLock.isHeld())
			wakeLock.release();
	}

	void playRingtone() {
		try {
			Uri uri = Uri.parse(chosenRingtone);
			mMediaPlayer.setDataSource(this, uri);
			final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
				mMediaPlayer.setLooping(true);
				mMediaPlayer.prepare();
				mMediaPlayer.start();
			}
		} catch (Exception e) {
			return;
		}
	}

	void stopRingtone() {
		if (mMediaPlayer != null) {
			try {
				mMediaPlayer.stop();
				mMediaPlayer.release();
			} catch (Exception e) {
				return;
			}
		}
	}
}
