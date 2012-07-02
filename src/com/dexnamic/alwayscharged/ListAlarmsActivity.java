package com.dexnamic.alwayscharged;

import com.dexnamic.alwayscharged.billing.ResponseHandler;
import com.dexnamic.alwayscharged.billing.UpgradeProActivity;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListAdapter;

public class ListAlarmsActivity extends ListActivity implements
		ListAlarmsCursorAdaptor.OnListClickListener, OnClickListener {

	private DatabaseHelper dbHelper;

	private Button addButton;

	private Cursor cursor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarm_list);

		addButton = (Button) findViewById(R.id.add_alarm);
		addButton.setOnClickListener(this);

		dbHelper = new DatabaseHelper(this);

		cursor = dbHelper.getAllAlarms();
		startManagingCursor(cursor);

		// Create the ListAdapter. A SimpleCursorAdapter lets you specify two
		// interesting things:
		// an XML template for your list item, and
		// The column to map to a specific item, by ID, in your template.
		ListAdapter adapter = new ListAlarmsCursorAdaptor(this, R.layout.alarm_item, // Use
																						// a
																						// template
																						// that
																						// displays
																						// a
																						// text
																						// view
				cursor, // Give the cursor to the list adapter
				this);
		setListAdapter(adapter);
		
		setVolumeControlStream(AudioManager.STREAM_ALARM);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (dbHelper != null)
			dbHelper.close();
	}

	@Override
	public void alarmChecked(int id, boolean isChecked) {
		Alarm alarm = dbHelper.getAlarm(id);
		alarm.setEnabled(isChecked);
		dbHelper.updateAlarm(alarm);
	}

	@Override
	public void alarmSelected(int id) {
		Intent intent = new Intent(this, EditAlarmPreferenceActivity.class);
		intent.putExtra("id", id);
		startActivity(intent);
	}

	@Override
	public void onClick(View view) {
		if (view == addButton) {
			if (ResponseHandler.hasPurchased(this) == false && cursor.getCount() > 0) {
				Intent intent = new Intent(this, UpgradeProActivity.class);
//				intent.setAction("add button");
				startActivity(intent);
			} else {
				this.alarmSelected(-1);
			}
		}

	}

}
