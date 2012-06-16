package com.dexnamic.alwayscharged;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

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
		ListAdapter adapter = new SimpleCursorAdapter(this,  
		                android.R.layout.simple_list_item_1,  // Use a template that displays a text view
		                cursor,                                    // Give the cursor to the list adapter
		                new String[] {DatabaseHelper.KEY_LABEL} ,          // Map the NAME column in the people database to...
		                new int[] { R.id.alarm_item });              // The "text1" view defined in the XML template
		setListAdapter(adapter);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		
	}

}
