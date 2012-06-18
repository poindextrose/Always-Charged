package com.dexnamic.alwayscharged;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

public class EditAlarmPreferenceActivity extends PreferenceActivity implements
		OnPreferenceClickListener, OnPreferenceChangeListener {

	private int mId;

	DatabaseHelper database;

	CheckBoxPreference checkBox;
	Preference time;
	PreferenceScreen repeatPreference;
	CheckBoxPreference monday;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.alarm_detail);
		addPreferencesFromResource(R.xml.alarm_detail);

		Bundle extras = getIntent().getExtras();
		mId = extras.getInt("id");

		PreferenceScreen ps = getPreferenceScreen();

		checkBox = (CheckBoxPreference) ps.findPreference("key_checkbox");
		checkBox.setOnPreferenceClickListener(this);

		time = ps.findPreference("key_time");
		time.setOnPreferenceClickListener(this);

		repeatPreference = (PreferenceScreen) ps.findPreference("key_repeat");
		repeatPreference.setOnPreferenceChangeListener(this);
		
		monday = (CheckBoxPreference) ps.findPreference("key_monday");
		monday.setOnPreferenceChangeListener(this);

		database = new DatabaseHelper(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		setPreferences();
	}

	private void setPreferences() {

		Alarm alarm = database.getAlarm(mId);

		checkBox.setChecked(alarm.getEnabled());
		time.setSummary(alarm.getTime(this));

//		repeatPreference.setSummary(AlarmDetail.repeatToString(alarm.getRepeats()));

	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		if (preference == checkBox) {
			Toast.makeText(this, "checkbox", Toast.LENGTH_SHORT).show();
		} else if (preference == time) {
			Toast.makeText(this, "time", Toast.LENGTH_SHORT).show();
		}

		return false;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {

		if(preference == repeatPreference) {
			Log.i(this.getClass().getSimpleName(), "repeat changed");
		}
		if(preference == monday) {
			Log.i(this.getClass().getSimpleName(), "monday");
		}
		return true;
	}

}
