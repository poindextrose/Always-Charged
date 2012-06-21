package com.dexnamic.alwayscharged;

import java.util.ArrayList;
import com.dexnamic.android.preference.ListPreferenceMultiSelect;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
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

	private Alarm mAlarm;
//	private Alarm mAlarmOriginal;
	private DatabaseHelper database;

	private CheckBoxPreference mEnabledCheckBox;
	private Preference mTimePreference;
	private ListPreferenceMultiSelect mRepeatPreference;
	private CheckBoxPreference mVibrateCheckBox;
	private Button cancelButton, deleteButton, okButton;

	private RingtonePreference mRingtonePreference;
	private EditTextPreference mLabelPreference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.alarm_detail);
		addPreferencesFromResource(R.xml.alarm_detail);

		Bundle extras = getIntent().getExtras();
		mId = extras.getInt("id");

		PreferenceScreen ps = getPreferenceScreen();

		mEnabledCheckBox = (CheckBoxPreference) ps.findPreference("key_checkbox");
		mEnabledCheckBox.setOnPreferenceClickListener(this);

		mTimePreference = ps.findPreference("key_time");
		mTimePreference.setOnPreferenceClickListener(this);

		mRepeatPreference = (ListPreferenceMultiSelect) ps.findPreference("key_repeat");
		mRepeatPreference.setOnPreferenceChangeListener(this);

		mRingtonePreference = (RingtonePreference) ps.findPreference("key_ringtone");
		mRingtonePreference.setOnPreferenceChangeListener(this);

		mLabelPreference = (EditTextPreference) ps.findPreference("key_label");
		mLabelPreference.setOnPreferenceChangeListener(this);

		mVibrateCheckBox = (CheckBoxPreference) ps.findPreference("key_vibrate");
		mVibrateCheckBox.setOnPreferenceClickListener(this);
		mVibrateCheckBox.setOnPreferenceChangeListener(this);

		cancelButton = (Button) findViewById(R.id.buttonCancel);
		cancelButton.setOnClickListener(this);
		deleteButton = (Button) findViewById(R.id.buttonDelete);
		deleteButton.setOnClickListener(this);
		okButton = (Button) findViewById(R.id.buttonOK);
		okButton.setOnClickListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();

		database = new DatabaseHelper(this);

		if (mId >= 0)
			mAlarm = database.getAlarm(mId);
		else
			mAlarm = new Alarm();
//		mAlarmOriginal = (Alarm) mAlarm.clone();
	}

	@Override
	protected void onResume() {
		super.onResume();

		setPreferences();
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (database != null)
			database.close();
	}

	private void setPreferences() {

		mEnabledCheckBox.setChecked(mAlarm.getEnabled());

		mTimePreference.setSummary(mAlarm.getTime(this));

		mRepeatPreference.setSummary(Alarm.repeatToString(mAlarm.getRepeats()));
		StringBuffer checked = new StringBuffer();
		// List<Integer> checkedIndicies = new ArrayList<Integer>();
		for (Integer i = 0; i < 7; i++) {
			if (mAlarm.getRepeats(i))
				checked.append(i.toString() + ",");
		}
		mRepeatPreference.setValue(checked.toString());

		mRingtonePreference.setSummary(mAlarm.getRingerName(this));
		getPreferenceManager().getSharedPreferences().edit()
				.putString("key_ringtone", mAlarm.getRingtone()).commit();

		mVibrateCheckBox.setChecked(mAlarm.getVibrate());

		mLabelPreference.setSummary(mAlarm.getLabel());
		mLabelPreference.setText(mAlarm.getLabel());

	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		if (preference == mEnabledCheckBox) {
			mAlarm.setEnabled(mEnabledCheckBox.isChecked());
		} else if (preference == mTimePreference) {
			showDialog(TIME_DIALOG_ID);
		} else if (preference == mVibrateCheckBox) {
			mAlarm.setVibrate(mVibrateCheckBox.isChecked());
		}

		return true;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {

		enableAlarm();

		if (preference == mRepeatPreference) {
			@SuppressWarnings("unchecked")
			ArrayList<String> results = (ArrayList<String>) newValue;
			Integer repeat = 0;
			for (String s : results) {
				repeat = repeat | (1 << Integer.parseInt(s));
			}
			mAlarm.setRepeats(repeat);
			mRepeatPreference.setSummary(Alarm.repeatToString(mAlarm.getRepeats()));
			return true;
		} else if (preference == mRingtonePreference) {
			mAlarm.setRingtone((String) newValue);
			mRingtonePreference.setSummary(mAlarm.getRingerName(this));
			checkVolume();
			return true;
		} else if (preference == mLabelPreference) {
			mAlarm.setLabel((String) newValue);
			mLabelPreference.setSummary((String) newValue);
			return true;
		}
		return true;
	}

	private void checkVolume() {
		String chosenRingtone = mAlarm.getRingtone();
		if (chosenRingtone.length() > 0) {
			AudioManager audioManager;
			audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
				Toast.makeText(this, getString(R.string.checkVolume), Toast.LENGTH_LONG).show();
			}
		}
	}

	private void cancelEdit() {
		finish();
	}

	private void deleteAlarm() {
		if (mId >= 0)
			database.deleteAlarm(mAlarm);
		finish();
	}

	private void saveAlarm() {
		if (mId >= 0) {
			database.updateAlarm(mAlarm);
		} else
			database.addAlarm(mAlarm);
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

		// if alarm is not new and nothing change then no need to save again
//		if(mId >= 0 && mAlarm.equals(mAlarmOriginal))
//			finish();
		saveAlarm();
		return;
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		mAlarm.setHour(hourOfDay);
		mAlarm.setMinute(minute);
		mTimePreference.setSummary(mAlarm.getTime(this));
		enableAlarm();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, this, mAlarm.getHour(), mAlarm.getMinute(), false);
		}
		return null;
	}

	private void enableAlarm() {
		/* if the user changes anything then enable the alarm */
		mAlarm.setEnabled(true);
		mEnabledCheckBox.setChecked(mAlarm.getEnabled());
	}
}
