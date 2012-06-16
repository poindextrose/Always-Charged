package com.dexnamic.alwayscharged;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class AlarmListActivity extends ListActivity {
	
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
		ListAdapter adapter = new AlarmListCursorAdaptor(this,  
		                R.layout.alarm_item,  // Use a template that displays a text view
		                cursor,                                    // Give the cursor to the list adapter
		                new String[] {},// not needed since bindView() is overridden
		                new int[] {});// not needed since bindView() is overridden
		setListAdapter(adapter);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		
	}

}
