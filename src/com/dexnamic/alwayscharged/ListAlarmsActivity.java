package com.dexnamic.alwayscharged;

import com.dexnamic.alwayscharged.billing.ResponseHandler;
import com.dexnamic.alwayscharged.billing.UpgradeProActivity;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;

public class ListAlarmsActivity extends ListActivity implements
		ListAlarmsCursorAdaptor.OnListClickListener, OnClickListener {

	private DatabaseHelper dbHelper;

	private Button addButton;

	private Cursor cursor;

	private ListAlarmsCursorAdaptor mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarm_list);

		addButton = (Button) findViewById(R.id.add_alarm);
		addButton.setOnClickListener(this);

		dbHelper = new DatabaseHelper(this);

		fillData();

		registerForContextMenu(getListView());

		setVolumeControlStream(AudioManager.STREAM_ALARM);
	}

	private void fillData() {
		cursor = dbHelper.getAllAlarms();
		startManagingCursor(cursor);

		// Create the ListAdapter. A SimpleCursorAdapter lets you specify two
		// interesting things:
		// an XML template for your list item, and
		// The column to map to a specific item, by ID, in your template.
		mAdapter = new ListAlarmsCursorAdaptor(this, R.layout.alarm_item, // Use
				// a
				// template
				// that
				// displays
				// a
				// text
				// view
				cursor, // Give the cursor to the list adapter
				this);
		setListAdapter(mAdapter);
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
				// intent.setAction("add button");
				startActivity(intent);
			} else {
				this.alarmSelected(-1);
			}
		}

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
		int id = (int) ((AdapterContextMenuInfo) menuInfo).id;
		Alarm alarm = dbHelper.getAlarm(id);
		if (alarm.getEnabled()) {
			menu.getItem(2).setTitle(R.string.disable_alarm);
		} else {
			menu.getItem(2).setTitle(R.string.enable_alarm);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		// Toast.makeText(this,
		// "info.id="+info.id+", info.positino="+info.position,
		// Toast.LENGTH_SHORT).show();
		switch (item.getItemId()) {
		case R.id.edit_alarm:
			alarmSelected((int) info.id);
			return true;
		case R.id.delete_alarm:
			dbHelper.deleteAlarm((int) info.id);
			mAdapter.getCursor().requery();
			mAdapter.notifyDataSetChanged();
			return true;
		case R.id.enable:
			Alarm alarm = dbHelper.getAlarm((int)info.id);
			alarm.setEnabled(!alarm.getEnabled());
			dbHelper.updateAlarm(alarm);
			mAdapter.getCursor().requery();
			mAdapter.notifyDataSetChanged();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
}
