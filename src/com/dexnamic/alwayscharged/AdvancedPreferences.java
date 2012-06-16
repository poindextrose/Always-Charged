package com.dexnamic.alwayscharged;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class AdvancedPreferences extends PreferenceActivity 
//implements Preference.OnPreferenceClickListener
{

	private ListPreference mListPreferenceSnooze;
	private ListPreference mListPreferenceDuration;
	private ListPreference mListPreferenceMotion;
//	private Preference mTestPref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.advanced_preferences);

		PreferenceScreen ps = getPreferenceScreen();
		SharedPreferences settings = ps.getSharedPreferences();
		mListPreferenceSnooze = (ListPreference) ps.findPreference(MainActivity.KEY_SNOOZE_TIME_MIN);
		mListPreferenceSnooze.setSummary(settings.getString(MainActivity.KEY_SNOOZE_TIME_MIN, "***") + " "
				+ getString(R.string.minutes));
		mListPreferenceSnooze.setOnPreferenceChangeListener(mOnPreferenceChangedListener);

		mListPreferenceDuration = (ListPreference) ps.findPreference(MainActivity.KEY_DURATION);
		mListPreferenceDuration.setSummary(settings.getString(MainActivity.KEY_DURATION, "***")
				+ " " + getString(R.string.seconds));
		mListPreferenceDuration.setOnPreferenceChangeListener(mOnPreferenceChangedListener);

		mListPreferenceMotion = (ListPreference) ps
				.findPreference(MainActivity.KEY_MOTION_TOLERANCE);
		setMotionToleranceSummary(settings.getString(MainActivity.KEY_MOTION_TOLERANCE,
				"***"));
		mListPreferenceMotion.setOnPreferenceChangeListener(mOnPreferenceChangedListener);
		
//		mTestPref = ps.findPreference("test1");
//		mTestPref.setOnPreferenceClickListener(this);
	}

	Preference.OnPreferenceChangeListener mOnPreferenceChangedListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
//			Toast.makeText(MainActivity.this, (String) newValue, Toast.LENGTH_LONG).show();
//			Log.d("dexnamic", "newValue=" + (String)newValue);
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

//	@Override
//	public boolean onPreferenceClick(Preference preference) {
//
//		if(preference == mTestPref) {
//			Intent intent = new Intent(this, AlarmDetailPreferenceActivity.class);
//			startActivity(intent);
//		}
//		return false;
//	}

}
