package com.dexnamic.alwayscharged;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;
import android.widget.Toast;

public class MainActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

	static final int TIME_DIALOG_ID = 0;
	static final int FIRST_TIME_DIALOG_ID = 1;
	static final int ABOUT_DIAlOG = 2;
	static final int CHANGELOG_DIALOG_ID = 3;

	private CheckBoxPreference mCheckBoxEnable;
	private Preference mPreferenceTime;
	private Preference mPreferenceAbout;

	SharedPreferences mSettings;
	SharedPreferences.Editor mEditor;

	public final static String KEY_ALARM_ENABLED = "key_alarm_enabled";
	public final static String KEY_TIME = "key_time";
	public final static String KEY_HOUR = "key_hour";
	public final static String KEY_MINUTE = "key_minute";
	public final static String KEY_RINGTONE = "key_ringtone";
	public final static String KEY_REPEAT = "key_repeat";
	public final static String KEY_VIBRATE = "key_vibrate";
	public final static String KEY_FIRST_TIME = "key_first_time";
	public final static String KEY_ABOUT = "key_about";
	public final static String KEY_VERSION_CODE = "key_version_code";

	public final static String KEY_SNOOZE_TIME_MIN = "key_snooze";
	public final static String KEY_DURATION = "key_alarm_duration";
	public final static String KEY_MOTION_TOLERANCE = "key_motion_tolerance";

	public final static String KEY_REPEAT_COUNT = "key_repeat_count";
	public static final int TIMES_TO_REPEAT = 2;

	private RingtonePreference mRingtonePreference;

	private boolean mFirstInstance = true;

	private final String LOG_TAG = this.getClass().getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		setContentView(R.layout.main);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		PreferenceManager.setDefaultValues(this, R.xml.advanced_preferences,
				false);

		PreferenceScreen ps = getPreferenceScreen();
		mSettings = ps.getSharedPreferences();
		mEditor = mSettings.edit();

		mCheckBoxEnable = (CheckBoxPreference) ps
				.findPreference(KEY_ALARM_ENABLED);

		mPreferenceTime = ps.findPreference(KEY_TIME);
		mPreferenceTime.setOnPreferenceClickListener(this);
		setTime();

		mRingtonePreference = (RingtonePreference) ps
				.findPreference(KEY_RINGTONE);
		mRingtonePreference
				.setOnPreferenceChangeListener(this);
		String uriString = mSettings.getString(KEY_RINGTONE, null);
		setRingtoneSummary(uriString);

		mPreferenceAbout = ps.findPreference(KEY_ABOUT);
		mPreferenceAbout.setOnPreferenceClickListener(this);

		// set wallpaper as background
		// TODO: fix stretching issue
		try {
			Class<?> _WallpaperManager = Class
					.forName("android.app.WallpaperManager");
			Class<?>[] parameterTypes = { Context.class };
			Method _WM_getinstance = _WallpaperManager.getMethod("getInstance",
					parameterTypes);
			Object[] args = { this };
			Object wm = _WM_getinstance.invoke(null, args);
			Method _WM_getDrawable = _WallpaperManager.getMethod("getDrawable",
					(Class[]) null);
			Drawable drawable = (Drawable) _WM_getDrawable.invoke(wm,
					(Object[]) null);
			getWindow().setBackgroundDrawable(drawable);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage());
		}

		setVolumeControlStream(AudioManager.STREAM_RING);

		if (savedInstanceState == null) // savedInstanceState is null during
										// first instantiation of class
			mFirstInstance = true;
		else
			mFirstInstance = false;
	}

	@Override
	protected void onStart() {
		super.onStart();

		// display a change log if the user just upgraded this application
		try {
			PackageManager pm = getPackageManager();
			PackageInfo packageInfo = pm.getPackageInfo(this.getPackageName(),
					0);
			if (mFirstInstance
					&& (packageInfo.versionCode > mSettings.getInt(
							KEY_VERSION_CODE, 0))) {
				mEditor.putInt(KEY_VERSION_CODE, packageInfo.versionCode);
				mEditor.commit();
				showDialog(CHANGELOG_DIALOG_ID);
			}
		} catch (NameNotFoundException e) {
		}

		// display a short educational dialog if this is a new user
		if (mFirstInstance && mSettings.getBoolean(KEY_FIRST_TIME, true))
			showDialog(FIRST_TIME_DIALOG_ID);
	}

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

		mEditor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.email_dev:
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("text/plain");
			String contactEmail = getString(R.string.contact_email);
			i.putExtra(Intent.EXTRA_EMAIL, new String[] { contactEmail });
			String feedback = getString(R.string.feedback);
			String appName = getString(R.string.app_name);
			i.putExtra(Intent.EXTRA_SUBJECT, feedback + " " + "(" + appName
					+ ")");
			// i.putExtra(Intent.EXTRA_TEXT , "body of email");
			Intent i2 = (Intent) i.clone();
			try {
				ComponentName cn = new ComponentName("com.google.android.gm",
						"com.google.android.gm.ComposeActivityGmail");
				i.setComponent(cn);
				startActivity(i);
			} catch (android.content.ActivityNotFoundException ex1) {
				try {
					startActivity(Intent.createChooser(i2,
							getString(R.string.sendmail)));
				} catch (android.content.ActivityNotFoundException ex2) {
					Toast.makeText(this, getString(R.string.noemail),
							Toast.LENGTH_SHORT).show();
				}
			}
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// Toast.makeText(MainActivity.this, (String) newValue,
		// Toast.LENGTH_LONG).show();
		// Log.d(LOG_TAG, "newValue=" + (String)newValue);
		if (preference == mRingtonePreference) {
			setRingtoneSummary((String) newValue);
		}
		return true;
	}

	private void setRingtoneSummary(String uriString) {
		String ringerName = getString(R.string.silent);
		try {
			Uri uri = Uri.parse(uriString);
			if (uriString.length() > 0) {
				ringerName = RingtoneManager
						.getRingtone(MainActivity.this, uri).getTitle(
								MainActivity.this);
			}
		} catch (Exception e) {
			ringerName = getString(R.string.default_ringtone);
			try {
				Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this,
						RingtoneManager.TYPE_RINGTONE);
				if (uriString.length() > 0) {
					ringerName = RingtoneManager.getRingtone(MainActivity.this,
							uri).getTitle(MainActivity.this);
				}
			} catch (Exception e2) {
				ringerName = getString(R.string.unknown);
			}
		} finally {
			mRingtonePreference.setSummary(ringerName);
			checkVolume();
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == mPreferenceTime) {
			MainActivity.this.showDialog(TIME_DIALOG_ID);
		} else if (preference == mPreferenceAbout) {
			showDialog(ABOUT_DIAlOG);
		}
		return false;
	}

	private void prefsChanged() {
		try {
			// try to backup preferences to Google if supported on this device
			Class<?> _BackupManager = Class
					.forName("android.app.backup.BackupManager");
			Constructor<?> constructor = _BackupManager
					.getConstructor(new Class[] { Context.class });
			// reflection: BackupManager bm = new BackupManager(this);
			Object backupManager = constructor.newInstance(this);
			Method dataChanged = _BackupManager.getMethod("dataChanged",
					(Class[]) null);
			// reflecdtion: backupManager.dataChanged();
			dataChanged.invoke(backupManager, (Object[]) null);
		} catch (Exception e) {
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		prefsChanged();
		if (key.equals(KEY_ALARM_ENABLED)) {
			if (sharedPreferences.getBoolean(KEY_ALARM_ENABLED, false)) {
				enableAlaram();
			} else {
				AlarmScheduler.disableAllAlarms(this);
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder;
		switch (id) {
		case TIME_DIALOG_ID:
			int hourOfDay = mSettings.getInt(KEY_HOUR, 22);
			int minute = mSettings.getInt(KEY_MINUTE, 0);
			return new TimePickerDialog(this, mTimeChangedListener, hourOfDay,
					minute, false);
		case FIRST_TIME_DIALOG_ID:
			builder = aboutDialogBuilder();
			builder.setNegativeButton(getString(R.string.dontshowagain),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							mEditor.putBoolean(KEY_FIRST_TIME, false);
							mEditor.commit();
							dialog.dismiss();
						}
					});
			AlertDialog alertDialog = builder.create();
			return alertDialog;
		case ABOUT_DIAlOG:
			builder = aboutDialogBuilder();
			alertDialog = builder.create();
			return alertDialog;
		case CHANGELOG_DIALOG_ID:
			builder = changelogDialogBuilder();
			alertDialog = builder.create();
			return alertDialog;
		}
		return null;
	}

	private AlertDialog.Builder aboutDialogBuilder() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.first_time_dialog,
				(ViewGroup) findViewById(R.id.first_time_layout_root));
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setTitle(getString(R.string.app_name));
		builder.setPositiveButton(getString(R.string.close),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		return builder;
	}

	private AlertDialog.Builder changelogDialogBuilder() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.changelog_dialog,
				(ViewGroup) findViewById(R.id.changelog_layout_root));
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setTitle(getString(R.string.changelog_title));
		builder.setPositiveButton(getString(R.string.close),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		return builder;
	}

	private TimePickerDialog.OnTimeSetListener mTimeChangedListener = new TimePickerDialog.OnTimeSetListener() {

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mEditor.putInt(KEY_HOUR, hourOfDay);
			mEditor.putInt(KEY_MINUTE, minute);
			mEditor.commit();
			mPreferenceTime.setSummary(formatTime(hourOfDay, minute));
			if(mCheckBoxEnable.isChecked()) {
				enableAlaram();
			} else {
				mCheckBoxEnable.setChecked(true); // this will set alarm
			}
		}
	};

	void setTime() {
		int hourOfDay = mSettings.getInt(KEY_HOUR, 22);
		int minute = mSettings.getInt(KEY_MINUTE, 0);
		mPreferenceTime.setSummary(formatTime(hourOfDay, minute));
	}

	private String formatTime(int hourOfDay, int minute) {
		String suffix = "";
		if (minute == 0) {
			switch (hourOfDay) {
			case 0:
				return "midnight";
			case 12:
				return "noon";
			}
		}
		int timeFormat = Settings.System.getInt(getContentResolver(),
				Settings.System.TIME_12_24, 12);
		if (timeFormat == 12) {
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
		checkVolume();

		int hourOfDay = mSettings.getInt(KEY_HOUR, 22);
		int minute = mSettings.getInt(KEY_MINUTE, 0);
		int minutesUntilAlarm = AlarmScheduler.setDailyAlarm(this, hourOfDay,
				minute);

		notifyUserTimeUntilAlarm(minutesUntilAlarm);
	}
	
	private void notifyUserTimeUntilAlarm(int minutesUntilAlarm) {
		String msg = "";
		int hoursUntilAlarm = (int) (minutesUntilAlarm / 60);
		minutesUntilAlarm = minutesUntilAlarm % 60;
		if (hoursUntilAlarm > 0) {
			msg += hoursUntilAlarm + " ";
			if (hoursUntilAlarm == 1)
				msg += getString(R.string.hour);
			else
				msg += getString(R.string.hours);
		}
		if (hoursUntilAlarm > 0 && minutesUntilAlarm > 0)
			msg += ", ";
		if (minutesUntilAlarm > 0) {
			msg += minutesUntilAlarm + " ";
			if (minutesUntilAlarm == 1)
				msg += getString(R.string.minute);
			else
				msg += getString(R.string.minutes);
		}
		msg += " " + getString(R.string.until_alarm);
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	private void checkVolume() {
		String chosenRingtone = mSettings.getString(KEY_RINGTONE, "");
		if (chosenRingtone.length() > 0) {
			AudioManager audioManager;
			audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
				Toast.makeText(MainActivity.this,
						getString(R.string.checkVolume), Toast.LENGTH_LONG)
						.show();
			}
		}
	}

}
