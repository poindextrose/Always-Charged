package com.dexnamic.alwayscharged;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

// progressive alarm volume

// make application icon
// make snooze icon
// bring up explanation on first use with prompts for each feature
// more short explanations on main screen

// test: prevent alarm from going off during phone call
// test: snooze alarm if phone call received

//bring up explanation on first use with prompts for each feature

// feedback button

//from clock alarm:
//floating window like clock alarm: background image if locked

//menu: settings, feedback

//shake or move to dismiss/snooze alarm
//setup for difference screen orientations
//beautify
//setup for other languages 
//make sure it works as expected if user changes time zones
//do not activate alarm for a few minutes after last use
//optionally raise volume to maximum level
//optionally play any sound file from phone
//android.app.backup
//advanced settings: snooze time, set alarm volume to max
//background of Android robot sleeping on bed with cord plugged into wall
//visually format for large screen tablets

//estimate time to charge

//test: plugged/unplugged, reboot, time zone change, snooze features

/**
* Features: user notified with alarm if device not plugged it by a certain time
* at night
* 
*/


public class MainActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	static final int TIME_DIALOG_ID = 0;

	private CheckBoxPreference mCheckBoxEnable;
	private Preference mPreferenceTime;
	// private CheckBoxPreference mPreferenceRepeat;
	private ListPreference mListPreferenceSnooze;

	SharedPreferences settings;
	SharedPreferences.Editor editor;

	public final static String KEY_ALARM_ENABLED = "key_alarm_enabled";
	public final static String KEY_TIME = "key_time";
	public final static String KEY_HOUR = "key_hour";
	public final static String KEY_MINUTE = "key_minute";
	public final static String KEY_RINGTONE = "key_ringtone";
	public final static String KEY_REPEAT = "key_repeat";
	public final static String KEY_VIBRATE = "key_vibrate";
	public final static String KEY_SNOOZE = "key_snooze";
	
	public final static int TIMES_TO_REPEAT = 2;
	public final static String KEY_REPEAT_COUNT = "key_repeat_count";

	private int mTimeFormat; // 12 or 24

	private Button mButtonDone;

	private RingtonePreference mPreferenceRingtone;
	
	private AudioManager mAudioManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		mTimeFormat = Settings.System.getInt(getContentResolver(),
				Settings.System.TIME_12_24, 12);

		// Load the XML preferences file
		addPreferencesFromResource(R.xml.preferences);
		setContentView(R.layout.main);
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		PreferenceScreen ps = getPreferenceScreen();
		settings = ps.getSharedPreferences();
		editor = settings.edit();

		mCheckBoxEnable = (CheckBoxPreference) ps
				.findPreference(KEY_ALARM_ENABLED);

		mPreferenceTime = (Preference) ps.findPreference(KEY_TIME);
		mPreferenceTime
				.setOnPreferenceClickListener(mOnPreferenceClickListener);
		setTime();

		mPreferenceRingtone = (RingtonePreference) ps
				.findPreference(KEY_RINGTONE);
		mPreferenceRingtone
				.setOnPreferenceChangeListener(mOnPreferenceChangedListener);
		String uriString = settings.getString(KEY_RINGTONE, null);
		setRingtoneSummary(uriString);

		//CheckBoxPreference repeat = (CheckBoxPreference) ps.findPreference(KEY_REPEAT);
		// repeat.setSum
		
		mListPreferenceSnooze = (ListPreference) ps.findPreference(KEY_SNOOZE);
		mListPreferenceSnooze.setSummary(settings.getString(KEY_SNOOZE, "***") + " " + getString(R.string.minutes));
		mListPreferenceSnooze.setOnPreferenceChangeListener(mOnPreferenceChangedListener);

		mButtonDone = (Button) findViewById(R.id.ButtonDone);
		mButtonDone.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MainActivity.this.finish();
			}
		});
	}
	
	private void setRingtoneSummary(String uriString) {
		String ringerName = "Silent";
		try {
			Uri uri = Uri.parse(uriString);
			if (uriString.length() > 0) {
				ringerName = RingtoneManager.getRingtone(
						MainActivity.this, uri).getTitle(
						MainActivity.this);
			}
		} catch (Exception e) {
			ringerName = "unknown";
		} finally {
			mPreferenceRingtone.setSummary(ringerName);
			checkVolume();
		}
	}
	
	OnPreferenceChangeListener mOnPreferenceChangedListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			//Toast.makeText(MainPreferenceActivity.this, (String)newValue, Toast.LENGTH_LONG).show();
			if (preference == mPreferenceRingtone) {
				setRingtoneSummary((String)newValue);
			} else if (preference == mListPreferenceSnooze) {
				String minutes = MainActivity.this.getString(R.string.minutes);
				mListPreferenceSnooze.setSummary((String)newValue + " " + minutes);
			}
			return true;
		}
	};

	Preference.OnPreferenceClickListener mOnPreferenceClickListener = new Preference.OnPreferenceClickListener() {

		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (preference == mPreferenceTime) {
				MainActivity.this.showDialog(TIME_DIALOG_ID);
			}
			return false;
		}
	};

	@Override
	protected void onResume() {
		super.onResume();

		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);

		editor.commit();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(KEY_ALARM_ENABLED)) {
			if (sharedPreferences.getBoolean(KEY_ALARM_ENABLED, false)) {
				enableAlaram();
			} else {
				disableAlarm();
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			int hourOfDay = settings.getInt(KEY_HOUR, 22);
			int minute = settings.getInt(KEY_MINUTE, 0);
			return new TimePickerDialog(this, mTimeChangedListener, hourOfDay,
					minute, false);
		}
		return null;
	}

	private TimePickerDialog.OnTimeSetListener mTimeChangedListener = new TimePickerDialog.OnTimeSetListener() {

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			changeTime(hourOfDay, minute);
		}
	};

	void changeTime(int hourOfDay, int minute) {
		editor.putInt(KEY_HOUR, hourOfDay);
		editor.putInt(KEY_MINUTE, minute);
		mCheckBoxEnable.setChecked(true);
		mPreferenceTime.setSummary(formatTime(hourOfDay, minute));
		AlarmScheduler.setDailyAlarm(this, hourOfDay, minute);
	}

	void setTime() {
		int hourOfDay = settings.getInt(KEY_HOUR, 22);
		int minute = settings.getInt(KEY_MINUTE, 0);
		mPreferenceTime.setSummary(formatTime(hourOfDay, minute));
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

		int hourOfDay = settings.getInt(KEY_HOUR, 22);
		int minute = settings.getInt(KEY_MINUTE, 0);
		AlarmScheduler.setDailyAlarm(this, hourOfDay, minute);
	}

	private void disableAlarm() {
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_ALARM);
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_SNOOZE);
	}

	private void checkVolume() {
		String chosenRingtone = settings.getString(KEY_RINGTONE, "");
		if (chosenRingtone.length() > 0
				&& mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
			Toast.makeText(MainActivity.this,
					"Alarm volume is set to zero, press volume keys to adjust",
					Toast.LENGTH_LONG).show();
		}
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

}


