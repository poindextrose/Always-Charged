package com.dexnamic.alwayscharged;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TimePicker;

// setup for other languages 
// make sure it works as expected if user changes timezones
// pressing volume keys should silence alarm
// do not activate alarm for a few minutes after last use
// optionally raise volume to maximum level
// optionally play any mp3 from phone
// shake or move to dismiss/snooze alarm
// android.app.backup

public class MainActivity extends Activity {

	public static final String PREFS_NAME = "MyPrefsFile";
	public static final String PREF_ENABLE = "enable";
	public static final String PREF_HOUR = "hour";
	public static final String PREF_MINUTE = "minute";
	public static final String PREF_RINGTONE = "ringtone";

	private static final int ACTIVITY_RESULT_RINGTONE = 1;

	private int mHour;
	private int mMinute;

	private int mTimeFormat; // 12 or 24

	private String chosenRingtone;

	private Button mButtonTime;
	private Button mButtonRingtone;
	private Button mButtonExit;

	private CheckBox mCheckEnable;

	SharedPreferences.Editor editor;
	SharedPreferences settings;

	static final int TIME_DIALOG_ID = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		settings = getSharedPreferences(PREFS_NAME, 0);
		mTimeFormat = Settings.System.getInt(getContentResolver(),
				Settings.System.TIME_12_24, 12);
		editor = settings.edit();

		mButtonTime = (Button) findViewById(R.id.ButtonTime);
		setTime();
		mButtonTime.setOnClickListener(mOnClickListener);

		mCheckEnable = (CheckBox) findViewById(R.id.CheckEnable);
		boolean alarmEnabled = settings.getBoolean(PREF_ENABLE, false);
		mCheckEnable.setChecked(alarmEnabled);
		if (alarmEnabled)
			enableAlaram();
		mCheckEnable
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						editor.putBoolean(PREF_ENABLE, isChecked);
						editor.commit();
						if (isChecked) {
							enableAlaram();
						} else {
							disableAlarm();
						}
					}
				});

		chosenRingtone = settings.getString(PREF_RINGTONE, null);
		mButtonRingtone = (Button) findViewById(R.id.ButtonRingtone);
		mButtonRingtone.setOnClickListener(mOnClickListener);

		mButtonExit = (Button) findViewById(R.id.ButtonExit);
		mButtonExit.setOnClickListener(mOnClickListener);
	}

	private OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v == mButtonTime)
				MainActivity.this.showDialog(TIME_DIALOG_ID);
			else if (v == mButtonRingtone)
				getRingtoneFromUser();
			else if (v == mButtonExit)
				MainActivity.this.finish();
		}
	};

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent intent) {
		if (resultCode == Activity.RESULT_OK
				&& requestCode == ACTIVITY_RESULT_RINGTONE) {
			Uri uri = intent
					.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			if (uri != null) {
				chosenRingtone = uri.toString();
			} else {
				chosenRingtone = null;
			}
			editor.putString(PREF_RINGTONE, chosenRingtone);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		editor.commit();
	}

	private TimePickerDialog.OnTimeSetListener mTimeChangedListener = new TimePickerDialog.OnTimeSetListener() {

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			changeTime(hourOfDay, minute);
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, mTimeChangedListener, mHour,
					mMinute, false);
		}
		return null;
	}

	void changeTime(int hourOfDay, int minute) {
		mHour = hourOfDay;
		mMinute = minute;
		editor.putInt(PREF_HOUR, hourOfDay);
		editor.putInt(PREF_MINUTE, minute);
		editor.commit();
		mCheckEnable.setChecked(true);
		mButtonTime.setText(formatTime(hourOfDay, minute));
		AlarmScheduler.setDailyAlarm(this, hourOfDay, minute);
	}

	void setTime() {
		int hourOfDay = settings.getInt(PREF_HOUR, 22);
		int minute = settings.getInt(PREF_MINUTE, 0);
		mHour = hourOfDay;
		mMinute = minute;
		mButtonTime.setText(formatTime(hourOfDay, minute));
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
		return String.format("%02d", hourOfDay) + ":"
				+ String.format("%02d", minute) + suffix;
	}

	private void enableAlaram() {
		int hourOfDay = settings.getInt(PREF_HOUR, 22);
		int minute = settings.getInt(PREF_MINUTE, 0);
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_SNOOZE);
		AlarmScheduler.setDailyAlarm(this, hourOfDay, minute);
	}

	private void disableAlarm() {
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_ALARM);
		AlarmScheduler.cancelAlarm(this, AlarmScheduler.TYPE_SNOOZE);
	}

	private void getRingtoneFromUser() {
		Uri uri = null;
		if (chosenRingtone != null)
			uri = Uri.parse(chosenRingtone);
		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
				RingtoneManager.TYPE_ALARM);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm");
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, true);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) uri);
		MainActivity.this.startActivityForResult(intent,
				ACTIVITY_RESULT_RINGTONE);
	}
}