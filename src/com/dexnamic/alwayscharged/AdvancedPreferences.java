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
		OnSharedPreferenceChangeListener
// implements Preference.OnPreferenceClickListener
{

	private ListPreference mListPreferenceSnooze;
	private ListPreference mListPreferenceDuration;
	private ListPreference mListPreferenceMotion;
	private Boolean mHasPurchased;

	// private Preference mTestPref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.advanced_preferences);

		PreferenceScreen ps = getPreferenceScreen();
		SharedPreferences settings = ps.getSharedPreferences();
		mListPreferenceSnooze = (ListPreference) ps
				.findPreference(MainPreferenceActivity.KEY_SNOOZE_TIME_MIN);
		mListPreferenceSnooze.setSummary(settings.getString(
				MainPreferenceActivity.KEY_SNOOZE_TIME_MIN, "***")
				+ " "
				+ getString(R.string.minutes));
		mListPreferenceSnooze.setOnPreferenceChangeListener(mOnPreferenceChangedListener);

		mListPreferenceDuration = (ListPreference) ps
				.findPreference(MainPreferenceActivity.KEY_DURATION);
		mListPreferenceDuration.setSummary(settings.getString(MainPreferenceActivity.KEY_DURATION,
				"***") + " " + getString(R.string.seconds));
		mListPreferenceDuration.setOnPreferenceChangeListener(mOnPreferenceChangedListener);

		mListPreferenceMotion = (ListPreference) ps
				.findPreference(MainPreferenceActivity.KEY_MOTION_TOLERANCE);
		setMotionToleranceSummary(settings.getString(MainPreferenceActivity.KEY_MOTION_TOLERANCE,
				"***"));
		mListPreferenceMotion.setOnPreferenceChangeListener(mOnPreferenceChangedListener);

		// mTestPref = ps.findPreference("test1");
		// mTestPref.setOnPreferenceClickListener(this);
		
		setVolumeControlStream(AudioManager.STREAM_ALARM);
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
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
				this);
	}

	Preference.OnPreferenceChangeListener mOnPreferenceChangedListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			// Toast.makeText(MainActivity.this, (String) newValue,
			// Toast.LENGTH_LONG).show();
			// Log.d("dexnamic", "newValue=" + (String)newValue);
			if (preference == mListPreferenceSnooze) {
				String minutes = AdvancedPreferences.this.getString(R.string.minutes);
				mListPreferenceSnooze.setSummary((String) newValue + " " + minutes);
			} else if (preference == mListPreferenceDuration) {
				String seconds = AdvancedPreferences.this.getString(R.string.seconds);
				mListPreferenceDuration.setSummary((String) newValue + " " + seconds);
			} else if (preference == mListPreferenceMotion) {
				setMotionToleranceSummary((String) newValue);
			}
			return true;
		}
	};

	private void setMotionToleranceSummary(String newSummary) {
		int motionTolerance = Integer.parseInt(newSummary);
		String summary = getString(R.string.disabled);
		if (motionTolerance > 0) {
			String degrees = AdvancedPreferences.this.getString(R.string.degrees);
			summary = (String) newSummary + " " + degrees;
		}
		mListPreferenceMotion.setSummary(summary);
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
