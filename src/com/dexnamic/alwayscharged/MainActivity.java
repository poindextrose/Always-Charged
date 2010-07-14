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

// manual snooze: set by user on alert dialog
// unanswered snooze time: 5, 10, 20
// user activity silent snooze time: 5 minutes

// skip alarm for the night if battery level over a certain amount?

//add multiple snooze options to alertdialog?

// have snooze time get shorter as it gets later at night
// maybe another strategy if snoozed do to phone movement or telephone usage

// advanced preferences: optionally turn on phone ringer and use max volume

// progressive alarm volume

// alertdialog should have multiple choice on snooze time
// custom ringtone selection dialog that has vibrate option permanently shown at bottom of screen
// combine enable check with time setting preference

// Long Title: "Always Charged Intelligent Alarm"
// "Night(time) Charge Intelligent Reminder"
// "Night(time) Charge Intelligent Alarm"
// "IntelliCharge (Alarm)"
// "Never Dead (Intelligent Reminder)"
// "Fresh Start"
// "Topped Off"
// "Not Annoying Reminder"

// if alarm comes up over main activity, welcome screen is re-shown

// welcome screen doesn't look good on emulator
// so probably need a custom dialog

// if user sets alarm time for some morning time, ask them if they are sure for "am"

// lengthen alarm duration after testing complete

// advanced preference screen:
// alarm duration
// auto snooze time
// movement sensitivity
// always max volume

// use reflection for BatteryManager constants from API 5

// make application icon
// make snooze icon
// setup for other languages
// end-user license agreement
// create logging system for bug reports

// check for task killers and warn user

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
 * reactivates snooze if user unplugs device before battery level gets above 90%
 * can only be deactivated manually with notification button
 * 
 */

public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	static final int TIME_DIALOG_ID = 0;
	static final int FIRST_TIME_DIALOG_ID = 1;
	static final int ABOUT_DIAlOG = 2;

	private CheckBoxPreference mCheckBoxEnable;
	private Preference mPreferenceTime;
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
	public final static String KEY_FIRST_TIME = "key_first_time";
	public final static String KEY_ABOUT = "key_about";

	public final static String KEY_SNOOZE_TIME_MIN = "key_snooze";
	public final static String KEY_DURATION = "key_alarm_duration";
	public final static String KEY_MOTION_TOLERANCE = "key_motion_tolerance";

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
		mPreferenceTime.setOnPreferenceChangeListener(mOnPreferenceChangedListener);
		setTime();

		mRingtonePreference = (RingtonePreference) ps.findPreference(KEY_RINGTONE);
		mRingtonePreference.setOnPreferenceChangeListener(mOnPreferenceChangedListener);
		String uriString = settings.getString(KEY_RINGTONE, null);
		setRingtoneSummary(uriString);

		mPreferenceAbout = (Preference) ps.findPreference(KEY_ABOUT);
		mPreferenceAbout.setOnPreferenceClickListener(mOnPreferenceClickListener);

		Button buttonDone = (Button) findViewById(R.id.ButtonDone);
		buttonDone.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MainActivity.this.finish();
			}
		});

		Button buttonAdvanced = (Button) findViewById(R.id.ButtonAdvanced);
		buttonAdvanced.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(MainActivity.this, AdvancedPreferences.class);
				startActivity(i);
			}
		});

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
				Intent i2 = (Intent) i.clone();
				try {
					ComponentName cn = new ComponentName("com.google.android.gm",
							"com.google.android.gm.ComposeActivityGmail");
					i.setComponent(cn);
					startActivity(i);
				} catch (android.content.ActivityNotFoundException ex1) {
					try {
						startActivity(Intent
								.createChooser(i2, context.getString(R.string.sendmail)));
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
			} else if (preference == mPreferenceTime) {

			}
			return true;
		}
	};

	private void setRingtoneSummary(String uriString) {
		String ringerName = getString(R.string.silent);
		try {
			Uri uri = Uri.parse(uriString);
			if (uriString.length() > 0) {
				ringerName = RingtoneManager.getRingtone(MainActivity.this, uri).getTitle(
						MainActivity.this);
			}
		} catch (Exception e) {
			ringerName = getString(R.string.unknown);
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
				showDialog(ABOUT_DIAlOG);
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
		AlertDialog.Builder builder;
		switch (id) {
		case TIME_DIALOG_ID:
			int hourOfDay = settings.getInt(KEY_HOUR, 22);
			int minute = settings.getInt(KEY_MINUTE, 0);
			return new TimePickerDialog(this, mTimeChangedListener, hourOfDay, minute, false);
		case FIRST_TIME_DIALOG_ID:
			builder = prepareDialogBuilder();
			builder.setNegativeButton(getString(R.string.dontshowagain),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							editor.putBoolean(KEY_FIRST_TIME, false);
							editor.commit();
							dialog.dismiss();
						}
					});
			AlertDialog alertDialog = builder.create();
			return alertDialog;
		case ABOUT_DIAlOG:
			builder = prepareDialogBuilder();
			alertDialog = builder.create();
			return alertDialog;
		}
		return null;
	}

	private AlertDialog.Builder prepareDialogBuilder() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.first_time_dialog,
				(ViewGroup) findViewById(R.id.first_time_layout_root));
//		TextView text = (TextView) layout.findViewById(R.id.text);
//		text.setText("Hello, this is a custom dialog!");
//		ImageView image = (ImageView) layout.findViewById(R.id.image);
//		image.setImageResource(R.drawable.android);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setTitle(getString(R.string.app_name));
//		builder.setMessage(getString(R.string.about));
		builder.setPositiveButton(getString(R.string.close), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		return builder;
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
		editor.commit();
		mCheckBoxEnable.setChecked(true);
		mPreferenceTime.setSummary(formatTime(hourOfDay, minute));
		enableAlaram();
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
		int minutesUntilAlarm = AlarmScheduler.setDailyAlarm(this, hourOfDay, minute);

		String msg = "";
		int hoursUntilAlarm = (int) (minutesUntilAlarm / 60);
		minutesUntilAlarm = minutesUntilAlarm % 60;
		if (hoursUntilAlarm > 0) {
			msg += hoursUntilAlarm + " ";
			if(hoursUntilAlarm == 1)
				msg += getString(R.string.hour);
			else
				msg += getString(R.string.hours);
		}
		if(hoursUntilAlarm > 0 && minutesUntilAlarm > 0)
			msg += ", ";
		if(minutesUntilAlarm > 0) {
			msg += minutesUntilAlarm + " ";
			if(minutesUntilAlarm == 1)
				msg += getString(R.string.minute);
			else
				msg += getString(R.string.minutes);
		}
		msg += " until alarm";
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();		
	}

	private void disableAlarms() {
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_ALARM);
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_SNOOZE);
	}

	private void checkVolume() {
		String chosenRingtone = settings.getString(KEY_RINGTONE, "");
		if (chosenRingtone.length() > 0
				&& mAudioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
			Toast.makeText(MainActivity.this, getString(R.string.checkVolume), Toast.LENGTH_LONG)
					.show();
		}
	}

}
