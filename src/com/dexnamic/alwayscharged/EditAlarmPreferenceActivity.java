package com.dexnamic.alwayscharged;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class EditAlarmPreferenceActivity extends PreferenceActivity implements
		OnPreferenceClickListener, OnClickListener {

	private int mId;

	Alarm mAlarm;
	DatabaseHelper database;

	CheckBoxPreference checkBox;
	Preference time;
	PreferenceScreen repeatPreference;
	CheckBoxPreference monday, tuesday, wednesday, thursday, friday, saturday, sunday;
	
	Button cancelButton, deleteButton, okButton;

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
		repeatPreference.setOnPreferenceClickListener(this);

		monday = (CheckBoxPreference) ps.findPreference("key_monday");
		monday.setOnPreferenceClickListener(this);
		tuesday = (CheckBoxPreference) ps.findPreference("key_tuesday");
		tuesday.setOnPreferenceClickListener(this);
		wednesday = (CheckBoxPreference) ps.findPreference("key_wednesday");
		wednesday.setOnPreferenceClickListener(this);
		thursday = (CheckBoxPreference) ps.findPreference("key_thursday");
		thursday.setOnPreferenceClickListener(this);
		friday = (CheckBoxPreference) ps.findPreference("key_friday");
		friday.setOnPreferenceClickListener(this);
		saturday = (CheckBoxPreference) ps.findPreference("key_saturday");
		saturday.setOnPreferenceClickListener(this);
		sunday = (CheckBoxPreference) ps.findPreference("key_sunday");
		sunday.setOnPreferenceClickListener(this);

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
		
		if(database != null)
			database.close();
	}

	@Override
	protected void onStart() {
		super.onStart();

		setPreferences();
	}

	private void setPreferences() {

		checkBox.setChecked(mAlarm.getEnabled());
		
		time.setSummary(mAlarm.getTime(this));
		
		repeatPreference.setSummary(Alarm.repeatToString(mAlarm.getRepeats()));
		
		monday.setChecked(mAlarm.getRepeats(0));
		tuesday.setChecked(mAlarm.getRepeats(1));
		wednesday.setChecked(mAlarm.getRepeats(2));
		thursday.setChecked(mAlarm.getRepeats(3));
		friday.setChecked(mAlarm.getRepeats(4));
		saturday.setChecked(mAlarm.getRepeats(5));
		sunday.setChecked(mAlarm.getRepeats(6));

	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		if (preference == checkBox) {
			mAlarm.setEnabled(checkBox.isChecked());
			Log.i("", "checkBox clicked " + mAlarm.getEnabled());
		}
		if (preference == time) {
			// Toast.makeText(this, "time", Toast.LENGTH_SHORT).show();
		}

		return false;
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
		if(view == cancelButton) {
			cancelEdit();
		} else if (view == deleteButton) {
			deleteAlarm();
		} else if (view == okButton) {
			saveAlarm();
		}
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
	            && keyCode == KeyEvent.KEYCODE_BACK
	            && event.getRepeatCount() == 0) {
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
}
