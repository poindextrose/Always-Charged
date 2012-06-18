package com.dexnamic.alwayscharged;

import java.util.ArrayList;

import com.dexnamic.android.preference.ListPreferenceMultiSelect;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

public class EditAlarmPreferenceActivity extends PreferenceActivity implements
		OnPreferenceClickListener, OnPreferenceChangeListener, OnClickListener,
		TimePickerDialog.OnTimeSetListener {

	static final int TIME_DIALOG_ID = 0;

	private int mId;

	Alarm mAlarm;
	DatabaseHelper database;

	CheckBoxPreference checkBox;
	Preference time;
	ListPreferenceMultiSelect repeatPreference;

	CheckBoxPreference vibrate;

	Button cancelButton, deleteButton, okButton;

	private RingtonePreference mRingtonePreference;

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

		repeatPreference = (ListPreferenceMultiSelect) ps.findPreference("key_repeat");
		repeatPreference.setOnPreferenceChangeListener(this);

		mRingtonePreference = (RingtonePreference) ps.findPreference("key_ringtone");
		mRingtonePreference.setOnPreferenceChangeListener(this);

		vibrate = (CheckBoxPreference) ps.findPreference("key_vibrate");
		vibrate.setOnPreferenceClickListener(this);

		cancelButton = (Button) findViewById(R.id.buttonCancel);
		cancelButton.setOnClickListener(this);
		deleteButton = (Button) findViewById(R.id.buttonDelete);
		deleteButton.setOnClickListener(this);
		okButton = (Button) findViewById(R.id.buttonOK);
		okButton.setOnClickListener(this);

		database = new DatabaseHelper(this);

		mAlarm = database.getAlarm(mId);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (database != null)
			database.close();
	}

	@Override
	protected void onResume() {
		super.onResume();

		setPreferences();
	}

	private void setPreferences() {

		checkBox.setChecked(mAlarm.getEnabled());

		time.setSummary(mAlarm.getTime(this));

		repeatPreference.setSummary(Alarm.repeatToString(mAlarm.getRepeats()));

		mRingtonePreference.setSummary(mAlarm.getRingerName(this));

	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		if (preference == checkBox) {
			mAlarm.setEnabled(checkBox.isChecked());
		} else if (preference == time) {
			showDialog(TIME_DIALOG_ID);
		} else if (preference == vibrate) {
			mAlarm.setVibrate(vibrate.isChecked());
		}

		return true;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference == repeatPreference) {
			@SuppressWarnings("unchecked")
			ArrayList<String> results = (ArrayList<String>) newValue;
			Integer repeat = 0;
			for (String s : results) {
				repeat = repeat | (1 << Integer.parseInt(s));
			}
			mAlarm.setRepeats(repeat);
			repeatPreference.setSummary(Alarm.repeatToString(mAlarm.getRepeats()));
			return true;
		} else if (preference == mRingtonePreference) {
			mAlarm.setRingtone((String)newValue);
			mRingtonePreference.setSummary(mAlarm.getRingerName(this));
			checkVolume();
		}
		return false;
	}
	

	
	private void checkVolume() {
		String chosenRingtone = mAlarm.getRingtone();
		if (chosenRingtone.length() > 0) {
			AudioManager audioManager;
			audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
				Toast.makeText(this, getString(R.string.checkVolume),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private void cancelEdit() {
		finish();
	}

	private void deleteAlarm() {
		database.deleteAlarm(mAlarm);
		finish();
	}

	private void saveAlarm() {
		database.updateAlarm(mAlarm);
		finish();
	}

	@Override
	public void onClick(View view) {
		if (view == cancelButton) {
			cancelEdit();
		} else if (view == deleteButton) {
			deleteAlarm();
		} else if (view == okButton) {
			saveAlarm();
		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
				&& keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			// Take care of calling this method on earlier versions of
			// the platform where it doesn't exist.
			onBackPressed();
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		// This will be called either automatically for you on 2.0
		// or later, or by the code above on earlier versions of the
		// platform.
		saveAlarm();
		return;
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		mAlarm.setHour(hourOfDay);
		mAlarm.setMinute(minute);
		time.setSummary(mAlarm.getTime(this));
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, this, mAlarm.getHour(), mAlarm.getMinute(), false);
		}
		return null;
	}
}
