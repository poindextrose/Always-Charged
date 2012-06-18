package com.dexnamic.alwayscharged;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ListAlarmsActivity extends ListActivity
implements ListAlarmsCursorAdaptor.OnListClickListener
{
	
	private DatabaseHelper dbHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarm_list);
		
		dbHelper = new DatabaseHelper(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		Cursor cursor = dbHelper.getAllAlarms();
		startManagingCursor(cursor);

		// Create the ListAdapter. A SimpleCursorAdapter lets you specify two interesting things:
		// an XML template for your list item, and
		// The column to map to a specific item, by ID, in your template.
		ListAdapter adapter = new ListAlarmsCursorAdaptor(this,  
		                R.layout.alarm_item,  // Use a template that displays a text view
		                cursor,                                    // Give the cursor to the list adapter
		                this);
		setListAdapter(adapter);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		
	}

	@Override
	public void alarmChecked(int id, boolean isChecked) {
		Log.i(this.getClass().getSimpleName(), "onClick checkbox, id = " + id);
	}

	@Override
	public void alarmSelected(int id) {
		Log.i(this.getClass().getSimpleName(), "onClick summary, id = " + id);
		Intent intent = new Intent(this, EditAlarmPreferenceActivity.class);
		intent.putExtra("id", id);
		startActivity(intent);
	}

}
