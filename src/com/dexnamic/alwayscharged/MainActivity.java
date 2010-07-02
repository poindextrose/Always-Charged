package com.dexnamic.alwayscharged;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TimePicker;
import android.widget.Toast;

// vibrate checkbox option
//snooze alarm if phone call received

// from clock alarm:
// on/off vibrate pattern
// toast indicating how long until alarm
// floating window like clock alarm: background image if locked

// menu: settings, feedback

// shake or move to dismiss/snooze alarm
// setup for difference screen orientations
// beautify
// setup for other languages 
// make sure it works as expected if user changes time zones
// do not activate alarm for a few minutes after last use
// optionally raise volume to maximum level
// optionally play any sound file from phone
// android.app.backup
// advanced settings: snooze time, set alarm volume to max
// background of Android robot sleeping on bed with cord plugged into wall
//visually format for large screen tablets

// estimate time to charge

// test: plugged/unplugged, reboot, time zone change, snooze features

/**
 * Features: user notified with alarm if device not plugged it by a certain time
 * at night
 * 
 */

public class MainActivity extends Activity {

	public static final String PREFS_NAME = "MyPrefsFile";
	public static final String PREF_ENABLE = "enable";
	public static final String PREF_HOUR = "hour";
	public static final String PREF_MINUTE = "minute";
	public static final String PREF_RINGTONE = "ringtone";
	public static final String PREF_RINGTONE_TEXT = "ringtone_text";
	public static final String PREF_REPEAT = "repeat";

	private static final int ACTIVITY_RESULT_RINGTONE = 1;

	private int mHour;
	private int mMinute;

	private int mTimeFormat; // 12 or 24

	private String chosenRingtone;

	private Button mButtonTime;
	private Button mButtonRingtone;
	private Button mButtonExit;

	private CheckBox mCheckEnable;
	private CheckBox mCheckRepeat;

	SharedPreferences.Editor editor;
	SharedPreferences settings;

	static final int TIME_DIALOG_ID = 0;

	private AudioManager mAudioManager;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		settings = getSharedPreferences(PREFS_NAME, 0);
		mTimeFormat = Settings.System.getInt(getContentResolver(),
				Settings.System.TIME_12_24, 12);
		editor = settings.edit();

		mButtonTime = (Button) findViewById(R.id.ButtonTime);
		setTime();
		mButtonTime.setOnClickListener(mOnClickListener);

		mCheckEnable = (CheckBox) findViewById(R.id.CheckEnable);
		boolean alarmEnabled = settings.getBoolean(PREF_ENABLE, false);
		mCheckEnable.setChecked(alarmEnabled);
		if (alarmEnabled) {
			enableAlaram();
		}
		mCheckEnable.setOnCheckedChangeListener(mOnCheckChangeListener);

		mCheckRepeat = (CheckBox) findViewById(R.id.CheckRepeat);
		boolean alarmRepeat = settings.getBoolean(PREF_REPEAT, false);
		mCheckRepeat.setChecked(alarmRepeat);
		mCheckRepeat.setOnCheckedChangeListener(mOnCheckChangeListener);

		chosenRingtone = settings.getString(PREF_RINGTONE, null);
		mButtonRingtone = (Button) findViewById(R.id.ButtonRingtone);
		mButtonRingtone.setText(settings
				.getString(PREF_RINGTONE_TEXT, "Choose"));
		mButtonRingtone.setOnClickListener(mOnClickListener);

		mButtonExit = (Button) findViewById(R.id.ButtonExit);
		mButtonExit.setOnClickListener(mOnClickListener);

	}

	CompoundButton.OnCheckedChangeListener mOnCheckChangeListener = new CompoundButton.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (buttonView == mCheckEnable) {
				editor.putBoolean(PREF_ENABLE, isChecked);
				if (isChecked) {
					enableAlaram();
				} else {
					disableAlarm();
				}
			} else if (buttonView == mCheckRepeat) {
				editor.putBoolean(PREF_REPEAT, isChecked);
			}
		}
	};

	private OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v == mButtonTime)
				MainActivity.this.showDialog(TIME_DIALOG_ID);
			else if (v == mButtonRingtone)
				getRingtoneFromUser();
			else if (v == mButtonExit)
				MainActivity.this.finish();
		}
	};

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent intent) {
		Log.d("dexnamic", "MainActivity.onActivityResult, rq = " + requestCode
				+ ", rc = " + resultCode);
		if (resultCode == Activity.RESULT_OK
				&& requestCode == ACTIVITY_RESULT_RINGTONE) {
			Uri uri = intent
					.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			String chosenRingtoneText;
			if (uri != null) {
				chosenRingtone = uri.toString();
				chosenRingtoneText = RingtoneManager.getRingtone(
						MainActivity.this, uri).getTitle(MainActivity.this);
			} else {
				chosenRingtone = null;
				chosenRingtoneText = "Silent";
			}
			editor.putString(PREF_RINGTONE, chosenRingtone);
			mButtonRingtone.setText(chosenRingtoneText);
			editor.putString(PREF_RINGTONE_TEXT, chosenRingtoneText);
			checkVolume();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		editor.commit();
	}

	private TimePickerDialog.OnTimeSetListener mTimeChangedListener = new TimePickerDialog.OnTimeSetListener() {

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			changeTime(hourOfDay, minute);
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, mTimeChangedListener, mHour,
					mMinute, false);
		}
		return null;
	}

	void changeTime(int hourOfDay, int minute) {
		mHour = hourOfDay;
		mMinute = minute;
		editor.putInt(PREF_HOUR, hourOfDay);
		editor.putInt(PREF_MINUTE, minute);
		mCheckEnable.setChecked(true);
		mButtonTime.setText(formatTime(hourOfDay, minute));
		AlarmScheduler.setDailyAlarm(this, hourOfDay, minute);
	}

	void setTime() {
		int hourOfDay = settings.getInt(PREF_HOUR, 22);
		int minute = settings.getInt(PREF_MINUTE, 0);
		mHour = hourOfDay;
		mMinute = minute;
		mButtonTime.setText(formatTime(hourOfDay, minute));
	}

	String formatTime(int hourOfDay, int minute) {
		String suffix = "";
		if (minute == 0) {
			switch (hourOfDay) {
			case 0:
				return "midnight";
			case 12:
				return "noon";
			}
		}
		if (mTimeFormat == 12) {
			if (hourOfDay >= 12) {
				if (hourOfDay > 12)
					hourOfDay -= 12;
				suffix = " pm";
			} else {
				if (hourOfDay == 0)
					hourOfDay = 12;
				suffix = " am";
			}
		}
		return String.format("%d", hourOfDay) + ":"
				+ String.format("%02d", minute) + suffix;
	}

	private void enableAlaram() {
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_SNOOZE);
		checkVolume();

		try {
			Intent.class.getField("ACTION_POWER_DISCONNECTED"); // check for
			// functionality
			// on this API
			IntentFilter intentFilter = new IntentFilter(
					Intent.ACTION_BATTERY_CHANGED);
			Intent intentBattery = registerReceiver(null, intentFilter);
			int plugged = intentBattery.getIntExtra("plugged", 0);
			if (plugged == 0) { // do not set alarm now since device not plugged
				// in
				return;
			}
		} catch (Exception e) {
		}

		int hourOfDay = settings.getInt(PREF_HOUR, 22);
		int minute = settings.getInt(PREF_MINUTE, 0);
		AlarmScheduler.setDailyAlarm(this, hourOfDay, minute);
	}

	private void disableAlarm() {
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_ALARM);
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_SNOOZE);
	}

	private void getRingtoneFromUser() {
		Uri uri = null;
		try {
			if (chosenRingtone != null)
				uri = Uri.parse(chosenRingtone);
		} catch (Exception e) {
		}
		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
				RingtoneManager.TYPE_ALARM); // |
		// RingtoneManager.TYPE_RINGTONE);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm");
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, true);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) uri);
		this.startActivityForResult(intent, ACTIVITY_RESULT_RINGTONE);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			mAudioManager.adjustStreamVolume(AudioManager.STREAM_ALARM,
					AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND
							| AudioManager.FLAG_SHOW_UI);
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			mAudioManager.adjustStreamVolume(AudioManager.STREAM_ALARM,
					AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND
							| AudioManager.FLAG_SHOW_UI);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void checkVolume() {
		if (chosenRingtone != null
				&& mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
			Toast.makeText(MainActivity.this,
					"Alarm volume is set to zero, press volume keys to adjust",
					Toast.LENGTH_LONG).show();
		}
	}

}