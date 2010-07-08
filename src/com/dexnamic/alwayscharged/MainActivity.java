package com.dexnamic.alwayscharged;

import java.lang.reflect.Method;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
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
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

// Long Title: "Always Charged Intelligent Alarm"
// or "Night(time) Charge Intelligent Reminder"
// or "Night(time) Charge Intelligent Alarm"
// or "IntelliCharge (Alarm)"

// test "power snooze"

// don't go off if phone is moving (accelerometers)
// replace screenOn() function with movement (if accelerometers are on device)
// do this buy silently checking movement in AlertReceiver before doing anything
// this will prevent "power snooze" to alarm if house loses power

// make alarm duration shorter, something around 10 seconds

// aggressive alarm:
// replace "repeat 2x" option with "snooze until fully charged"

// advanced preference screen:
// alarm duration

// use reflection for BatteryManager constants from API 5

// make application icon
// make snooze icon
// setup for other languages
// end-user license agreement
// create logging system for bug reports

// check for task killers and warn user

// motion detection, just need to hold off on vibrate for a few seconds

// android.app.backup

// annotate code

// menu: reset to defaults

// progressive alarm volume
// test: prevent alarm from going off during phone call
// test: snooze alarm if phone call received

// shake or move to dismiss/snooze alarm
// setup for difference screen orientations
// make sure it works as expected if user changes time zones
// do not activate alarm for a few minutes after last use
// optionally raise volume to maximum level
// optionally play any sound file from phone
// visually format for large screen tablets

// estimate time to charge

// test: plugged/unplugged, reboot, time zone change, snooze features

/**
 * Features: user notified with alarm if device not plugged it by a certain time
 * at night
 * 
 * PowerSnooze (Android 1.6+)
 * 
 */

public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	static final int TIME_DIALOG_ID = 0;
	static final int FIRST_TIME_DIALOG_ID = 1;

	private CheckBoxPreference mCheckBoxEnable;
	private Preference mPreferenceTime;
	// private CheckBoxPreference mPreferenceRepeat;
	private ListPreference mListPreferenceSnooze;
	private Preference mPreferenceAbout;

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
	public final static String KEY_FIRST_TIME = "key_first_time";
	public final static String KEY_ABOUT = "key_about";

	public final static int TIMES_TO_REPEAT = 2;
	public final static String KEY_REPEAT_COUNT = "key_repeat_count";

	private int mTimeFormat; // 12 or 24

	private RingtonePreference mRingtonePreference;

	private AudioManager mAudioManager;

	private boolean mFirstInstance = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		mTimeFormat = Settings.System.getInt(getContentResolver(), Settings.System.TIME_12_24, 12);

		// Load the XML preferences file
		setContentView(R.layout.main);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		PreferenceScreen ps = getPreferenceScreen();
		settings = ps.getSharedPreferences();
		editor = settings.edit();

		mCheckBoxEnable = (CheckBoxPreference) ps.findPreference(KEY_ALARM_ENABLED);

		mPreferenceTime = (Preference) ps.findPreference(KEY_TIME);
		mPreferenceTime.setOnPreferenceClickListener(mOnPreferenceClickListener);
		setTime();

		mRingtonePreference = (RingtonePreference) ps.findPreference(KEY_RINGTONE);
		mRingtonePreference.setOnPreferenceChangeListener(mOnPreferenceChangedListener);
		String uriString = settings.getString(KEY_RINGTONE, null);
		setRingtoneSummary(uriString);

		mListPreferenceSnooze = (ListPreference) ps.findPreference(KEY_SNOOZE);
		mListPreferenceSnooze.setSummary(settings.getString(KEY_SNOOZE, "***") + " "
				+ getString(R.string.minutes));
		mListPreferenceSnooze.setOnPreferenceChangeListener(mOnPreferenceChangedListener);

		Button buttonDone = (Button) findViewById(R.id.ButtonDone);
		buttonDone.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MainActivity.this.finish();
			}
		});

		mPreferenceAbout = (Preference) ps.findPreference(KEY_ABOUT);
		mPreferenceAbout.setOnPreferenceClickListener(mOnPreferenceClickListener);

		Button buttonFeedback = (Button) findViewById(R.id.ButtonFeedback);
		buttonFeedback.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Context context = MainActivity.this;
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				String appName2 = context.getString(R.string.app_name_no_spaces);
				i.putExtra(Intent.EXTRA_EMAIL,
						new String[] { "dexnamic+" + appName2 + "@gmail.com" });
				String feedback = context.getString(R.string.feedback);
				String appName = context.getString(R.string.app_name);
				i.putExtra(Intent.EXTRA_SUBJECT, feedback + " " + "(" + appName + ")");
//				i.putExtra(Intent.EXTRA_TEXT   , "body of email");
				ComponentName cn = new ComponentName("com.google.android.gm",
						"com.google.android.gm.ComposeActivityGmail");
				i.setComponent(cn);
				try {
					startActivity(i);
				} catch (android.content.ActivityNotFoundException ex1) {
					try {
						startActivity(Intent.createChooser(i, context.getString(R.string.sendmail)));
					} catch (android.content.ActivityNotFoundException ex2) {
						Toast.makeText(context, context.getString(R.string.noemail),
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		});

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

		setVolumeControlStream(AudioManager.STREAM_RING);

		if (savedInstanceState == null) // savedInstanceState is null during first instantiation of class
			mFirstInstance = true;
		else
			mFirstInstance = false;
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (mFirstInstance && settings.getBoolean(KEY_FIRST_TIME, true))
			showDialog(FIRST_TIME_DIALOG_ID);
	}

	Preference.OnPreferenceChangeListener mOnPreferenceChangedListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
//			Toast.makeText(MainActivity.this, (String) newValue, Toast.LENGTH_LONG).show();
//			Log.d("dexnamic", "newValue=" + (String)newValue);
			if (preference == mRingtonePreference) {
				setRingtoneSummary((String) newValue);
			} else if (preference == mListPreferenceSnooze) {
				String minutes = MainActivity.this.getString(R.string.minutes);
				mListPreferenceSnooze.setSummary((String) newValue + " " + minutes);
			}
			return true;
		}
	};

	private void setRingtoneSummary(String uriString) {
		String ringerName = "Silent";
		try {
			Uri uri = Uri.parse(uriString);
			if (uriString.length() > 0) {
				ringerName = RingtoneManager.getRingtone(MainActivity.this, uri).getTitle(
						MainActivity.this);
			}
		} catch (Exception e) {
			ringerName = "unknown";
		} finally {
			mRingtonePreference.setSummary(ringerName);
			checkVolume();
		}
	}

	Preference.OnPreferenceClickListener mOnPreferenceClickListener = new Preference.OnPreferenceClickListener() {

		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (preference == mPreferenceTime) {
				MainActivity.this.showDialog(TIME_DIALOG_ID);
			} else if (preference == mPreferenceAbout) {
				showDialog(FIRST_TIME_DIALOG_ID);
			}
			return false;
		}
	};

	@Override
	protected void onResume() {
		super.onResume();

		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

//		Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
				this);

		editor.commit();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//		Toast.makeText(MainActivity.this, key, Toast.LENGTH_SHORT).show();
		if (key.equals(KEY_ALARM_ENABLED)) {
			if (sharedPreferences.getBoolean(KEY_ALARM_ENABLED, false)) {
				enableAlaram();
			} else {
				disableAlarms();
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			int hourOfDay = settings.getInt(KEY_HOUR, 22);
			int minute = settings.getInt(KEY_MINUTE, 0);
			return new TimePickerDialog(this, mTimeChangedListener, hourOfDay, minute, false);
		case FIRST_TIME_DIALOG_ID:
			AlertDialog.Builder builder;
			AlertDialog alertDialog;

			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.first_time_dialog,
					(ViewGroup) findViewById(R.id.first_time_layout_root));
//			TextView text = (TextView) layout.findViewById(R.id.text);
//			text.setText("Hello, this is a custom dialog!");
//			ImageView image = (ImageView) layout.findViewById(R.id.image);
//			image.setImageResource(R.drawable.android);
			builder = new AlertDialog.Builder(this);
			builder.setView(layout);
			builder.setMessage(getString(R.string.welcome));
			builder.setPositiveButton(getString(R.string.close),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			builder.setNegativeButton(getString(R.string.dontshowagain),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							editor.putBoolean(KEY_FIRST_TIME, false);
							editor.commit();
							dialog.dismiss();
						}
					});
			alertDialog = builder.create();
			return alertDialog;
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
		return String.format("%d", hourOfDay) + ":" + String.format("%02d", minute) + suffix;
	}

	private void enableAlaram() {
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_SNOOZE);
		checkVolume();

		try {
			Intent.class.getField("ACTION_POWER_DISCONNECTED"); // check for functionality on this API
			IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent intentBattery = registerReceiver(null, intentFilter);
			int plugged = intentBattery.getIntExtra("plugged", 0);
			if (plugged == 0) { // do not set alarm now since device not plugged in
				return;
			}
		} catch (Exception e) {
		}

		int hourOfDay = settings.getInt(KEY_HOUR, 22);
		int minute = settings.getInt(KEY_MINUTE, 0);
		AlarmScheduler.setDailyAlarm(this, hourOfDay, minute);
	}

	private void disableAlarms() {
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_ALARM);
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_SNOOZE);
	}

	private void checkVolume() {
		String chosenRingtone = settings.getString(KEY_RINGTONE, "");
		if (chosenRingtone.length() > 0
				&& mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
			Toast.makeText(MainActivity.this,
					"Alarm volume is set to zero, press volume keys to adjust", Toast.LENGTH_LONG)
					.show();
		}
	}

}
