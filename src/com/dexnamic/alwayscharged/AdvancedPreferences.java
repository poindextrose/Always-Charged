package com.dexnamic.alwayscharged;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.dexnamic.alwayscharged.billing.ResponseHandler;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class AdvancedPreferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener
{
	public final static String KEY_SNOOZE_TIME_MIN = "key_snooze";
	public final static String KEY_DURATION = "key_alarm_duration";
	public final static String KEY_MOTION_TOLERANCE = "key_motion_tolerance";

	private ListPreference mListPreferenceSnooze;
	private ListPreference mListPreferenceDuration;
	private ListPreference mListPreferenceMotion;
	private ListPreference mListPreferenceBattery;
	private Boolean mHasPurchased;
	private Boolean mSnoozeWasChanged;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.advanced_preferences);

		configurePreferences();

		setVolumeControlStream(AudioManager.STREAM_ALARM);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		mSnoozeWasChanged = false;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		mHasPurchased = ResponseHandler.hasPurchased(this);
		if (mHasPurchased) {
			upgradeToPro();
		}
	}

	private void upgradeToPro() {
		mListPreferenceDuration.setEnabled(true);
		mListPreferenceMotion.setEnabled(true);
		mListPreferenceSnooze.setEnabled(true);
		mListPreferenceBattery.setEnabled(true);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
				this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		if(mSnoozeWasChanged)
			Scheduler.resetAllEnabledAlarms(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// Toast.makeText(MainActivity.this, (String) newValue,
		// Toast.LENGTH_LONG).show();
		// Log.d("dexnamic", "newValue=" + (String)newValue);
		if (preference == mListPreferenceSnooze) {
			String minutes = AdvancedPreferences.this.getString(R.string.minutes);
			mListPreferenceSnooze.setSummary((String) newValue + " " + minutes);
			/* we need to update all alarms when snooze time is changed
			 * but the preference hasn't changed until after this method
			 * completes so lets flag it so we reset the alarms in onStop()
			 */
			mSnoozeWasChanged = true;
		} else if (preference == mListPreferenceDuration) {
			String seconds = AdvancedPreferences.this.getString(R.string.seconds);
			mListPreferenceDuration.setSummary((String) newValue + " " + seconds);
		} else if (preference == mListPreferenceMotion) {
			setMotionToleranceSummary((String) newValue);
		} else if (preference == mListPreferenceBattery) {
			setSkipBatterySummary((String) newValue);
		}
		return true;
	}

	private void setMotionToleranceSummary(String newSummary) {
		int motionTolerance = Integer.parseInt(newSummary);
		String summary = getString(R.string.disabled);
		if (motionTolerance > 0) {
			String degrees = AdvancedPreferences.this.getString(R.string.degrees);
			summary = (String) newSummary + " " + degrees;
		}
		mListPreferenceMotion.setSummary(summary);
	}

	private void setSkipBatterySummary(String value) {
		String[] entries = getResources().getStringArray(R.array.cancel_battery_entries);
		String[] entryValues = getResources().getStringArray(R.array.cancel_battery_entryValues);
		for (int i = 0; i < entries.length; i++) {
			if (entryValues[i].contentEquals(value)) {
				mListPreferenceBattery.setSummary(entries[i]);
				return;
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		try {
			// try to backup preferences to Google if supported on this device
			Class<?> _BackupManager = Class.forName("android.app.backup.BackupManager");
			Constructor<?> constructor = _BackupManager
					.getConstructor(new Class[] { Context.class });
			// reflection: BackupManager bm = new BackupManager(this);
			Object backupManager = constructor.newInstance(this);
			Method dataChanged = _BackupManager.getMethod("dataChanged", (Class[]) null);
			// reflection: backupManager.dataChanged();
			dataChanged.invoke(backupManager, (Object[]) null);
		} catch (Exception e) {
		}
	}

	private void configurePreferences() {
	
		PreferenceScreen ps = getPreferenceScreen();
		SharedPreferences settings = ps.getSharedPreferences();
		mListPreferenceSnooze = (ListPreference) ps
				.findPreference(KEY_SNOOZE_TIME_MIN);
		mListPreferenceSnooze.setSummary(settings.getString(
				KEY_SNOOZE_TIME_MIN, "***")
				+ " "
				+ getString(R.string.minutes));
		mListPreferenceSnooze.setOnPreferenceChangeListener(this);
	
		mListPreferenceDuration = (ListPreference) ps
				.findPreference(KEY_DURATION);
		mListPreferenceDuration.setSummary(settings.getString(KEY_DURATION,
				"***") + " " + getString(R.string.seconds));
		mListPreferenceDuration.setOnPreferenceChangeListener(this);
	
		mListPreferenceMotion = (ListPreference) ps
				.findPreference(KEY_MOTION_TOLERANCE);
		setMotionToleranceSummary(settings.getString(KEY_MOTION_TOLERANCE,
				"***"));
		mListPreferenceMotion.setOnPreferenceChangeListener(this);
	
		String key = getString(R.string.key_skip_battery);
		mListPreferenceBattery = (ListPreference) ps.findPreference(key);
		String defaultBattery = getResources().getStringArray(
				R.array.cancel_battery_entries)[0];
		setSkipBatterySummary(settings.getString(key, defaultBattery));
		mListPreferenceBattery.setOnPreferenceChangeListener(this);
	}

	// @Override
	// public boolean onPreferenceClick(Preference preference) {
	//
	// if(preference == mTestPref) {
	// Intent intent = new Intent(this, AlarmDetailPreferenceActivity.class);
	// startActivity(intent);
	// }
	// return false;
	// }

}
