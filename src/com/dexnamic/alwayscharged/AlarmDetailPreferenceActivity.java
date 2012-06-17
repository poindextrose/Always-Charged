package com.dexnamic.alwayscharged;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class AlarmDetailPreferenceActivity extends PreferenceActivity {
	
	private int mId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.alarm_detail);
		
		Bundle extras = getIntent().getExtras();
		mId = extras.getInt("id");

//		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onStart() {
		super.onStart();
		
//		Toast.makeText(this, "id = " + mId, Toast.LENGTH_LONG).show();
	}

}
